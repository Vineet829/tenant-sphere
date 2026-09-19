package com.tenantsphere.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AvatarStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarStorage.class);

    private final ProfileRepository profileRepository;
    private final Path avatarDir;

    public AvatarStorage(
            ProfileRepository profileRepository,
            @Value("${app.media.avatar-dir:media/avatars}") String avatarDir) {
        this.profileRepository = profileRepository;
        this.avatarDir = Path.of(avatarDir);
    }

    @Async
    @Transactional
    public void store(Long profilePkid, String reference, byte[] content) {
        try {
            Files.createDirectories(avatarDir);
            Files.write(avatarDir.resolve(reference), content);
            profileRepository.findById(profilePkid).ifPresent(profile -> {
                profile.setAvatar("/media/avatars/" + reference);
                profileRepository.save(profile);
            });
        } catch (IOException exception) {
            LOGGER.error("Failed to store avatar for profile {}", profilePkid, exception);
        }
    }
}
