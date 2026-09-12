package com.tenantsphere.profile;

import com.tenantsphere.user.User;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarService {

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
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Upload a valid image. The file you uploaded was not an image.");
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

        String reference = UUID.randomUUID() + extensionFor(contentType);
        storage.store(profile.getPkid(), reference, content);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
