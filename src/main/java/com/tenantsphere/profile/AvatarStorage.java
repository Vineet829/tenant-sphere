package com.tenantsphere.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AvatarStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarStorage.class);

    private final ProfileRepository profileRepository;
    private final Path avatarDir;

    public AvatarStorage(
            ProfileRepository profileRepository,
            @Value("${app.media.avatar-dir:media/avatars}") String avatarDir) {
        this.profileRepository = profileRepository;
        this.avatarDir = Path.of(avatarDir).toAbsolutePath().normalize();
    }

    @Transactional
    public void store(Long profilePkid, String reference, byte[] content) {
        Profile profile = profileRepository.findByPkidForUpdate(profilePkid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        String previous = profile.getAvatar();
        String next = "/media/avatars/" + reference;
        Path destination = managedFile(next);
        if (destination == null) throw new IllegalArgumentException("Invalid avatar reference");

        try {
            Files.createDirectories(avatarDir);
            Files.createFile(destination);
            // Register cleanup only after creating a file owned by this upload.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        removeManagedFile(previous);
                    } else if (status == STATUS_ROLLED_BACK) {
                        removeManagedFile(next);
                    }
                }
            });
            Files.write(destination, content, StandardOpenOption.WRITE);
            profile.setAvatar(next);
            profileRepository.save(profile);
        } catch (IOException exception) {
            LOGGER.error("Failed to store avatar for profile {}", profilePkid, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save your photo. Please try again.");
        }
    }

    private Path managedFile(String avatar) {
        if (avatar == null || !avatar.matches(
                "/media/avatars/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|gif|webp)")) {
            return null;
        }
        return avatarDir.resolve(avatar.substring("/media/avatars/".length()));
    }

    public void removeAfterCommit(String avatar) {
        com.tenantsphere.common.AfterCommit.run(() -> removeManagedFile(avatar));
    }

    private void removeManagedFile(String avatar) {
        Path file = managedFile(avatar);
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            LOGGER.warn("Could not remove replaced avatar {}", file.getFileName(), exception);
        }
    }
}
