package com.tenantsphere.profile;

import com.tenantsphere.user.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarService {

    private static final long MAX_BYTES = 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private static final Map<String, String> IMAGE_FORMATS = Map.of(
            "image/jpeg", "jpeg", "image/png", "png", "image/gif", "gif", "image/webp", "webp");

    private final ProfileRepository profileRepository;
    private final AvatarStorage storage;

    public AvatarService(ProfileRepository profileRepository, AvatarStorage storage) {
        this.profileRepository = profileRepository;
        this.storage = storage;
    }

    public void upload(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was submitted.");
        }
        String contentType = file.getContentType();
        String expectedFormat = contentType == null ? null : IMAGE_FORMATS.get(contentType.toLowerCase(Locale.ROOT));
        if (expectedFormat == null) {
            throw invalidImage();
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Choose an image smaller than 1 MB.");
        }

        Profile profile = profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found"));

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Could not read the uploaded file.");
        }

        validateImage(content, expectedFormat);
        String reference = UUID.randomUUID() + (expectedFormat.equals("jpeg") ? ".jpg" : "." + expectedFormat);
        storage.store(profile.getPkid(), reference, content);
    }

    private void validateImage(byte[] content, String expectedFormat) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!expectedFormat.equals(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw invalidImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Choose an image with no more than 16 million pixels.");
                }
                if (reader.read(0) == null) throw invalidImage();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidImage();
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Upload a valid JPEG, PNG, GIF or WebP image.");
    }
}
