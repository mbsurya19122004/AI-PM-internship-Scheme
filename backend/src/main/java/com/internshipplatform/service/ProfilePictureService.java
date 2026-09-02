package com.internshipplatform.service;

import com.internshipplatform.entity.User;
import com.internshipplatform.repository.UserRepository;
import com.internshipplatform.security.SecurityEventLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final SecurityEventLogger securityEventLogger;
    private final HttpServletRequest request;

    @Value("${app.upload.dir:uploads/profile-pictures}")
    private String uploadDir;

    @Value("${app.upload.max-size:5242880}")
    private long maxFileSize;

    @Transactional
    public String uploadProfilePicture(String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Store new file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Delete old picture if exists
        String oldPictureUrl = user.getProfilePictureUrl();
        if (oldPictureUrl != null && !oldPictureUrl.isEmpty()) {
            deleteOldPicture(oldPictureUrl);
        }

        // Update user
        user.setProfilePictureUrl(uniqueFilename);
        userRepository.save(user);

        String ipAddress = getClientIp();
        securityEventLogger.logFileUpload(email, "profile-picture", file.getSize(), ipAddress);

        return uniqueFilename;
    }

    @Transactional(readOnly = true)
    public byte[] getProfilePicture(String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filename = user.getProfilePictureUrl();
        if (filename == null || filename.isEmpty()) {
            throw new RuntimeException("No profile picture found");
        }

        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Profile picture file not found");
        }

        return Files.readAllBytes(filePath);
    }

    @Transactional(readOnly = true)
    public String getProfilePictureContentType(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filename = user.getProfilePictureUrl();
        if (filename == null || filename.isEmpty()) {
            return null;
        }

        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    @Transactional
    public void deleteProfilePicture(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filename = user.getProfilePictureUrl();
        if (filename != null && !filename.isEmpty()) {
            deleteOldPicture(filename);
        }

        user.setProfilePictureUrl(null);
        userRepository.save(user);

        securityEventLogger.logFileUpload(email, "profile-picture-deleted", 0, getClientIp());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }
    }

    private void deleteOldPicture(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete old profile picture: {}", e.getMessage());
        }
    }

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
