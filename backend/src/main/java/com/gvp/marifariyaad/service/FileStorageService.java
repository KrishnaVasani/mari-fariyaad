package com.gvp.marifariyaad.service;

import com.gvp.marifariyaad.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_PHOTO_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024; // 50 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/3gpp", "video/x-matroska"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Validates and stores an uploaded complaint photo. Returns the generated
     * (UUID-based) stored file name, or null if no file was supplied.
     */
    public String storePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid media type: only JPG, PNG and other image formats are allowed for the photo attachment.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new BadRequestException("File too large: complaint photo must not exceed 10 MB.");
        }
        return storeFile(file, "complaints/photos");
    }

    /**
     * Validates and stores an uploaded complaint video. Returns the generated
     * (UUID-based) stored file name, or null if no file was supplied.
     */
    public String storeVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid media type: only MP4, WebM, MOV and other video formats are allowed for the video attachment.");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("File too large: complaint video must not exceed 50 MB.");
        }
        return storeFile(file, "complaints/videos");
    }

    private String storeFile(MultipartFile file, String subDir) {
        try {
            Path targetDirectory = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDirectory);

            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                extension = originalName.substring(dotIndex);
            }
            // Never trust the original file name: generate a fresh UUID-based name,
            // which also prevents path traversal since it contains no user input.
            String generatedName = UUID.randomUUID() + sanitizeExtension(extension);

            Path targetPath = targetDirectory.resolve(generatedName).normalize();
            if (!targetPath.getParent().equals(targetDirectory)) {
                throw new BadRequestException("Invalid file path.");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return generatedName;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store uploaded file: " + e.getMessage());
        }
    }

    private String sanitizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) return "";
        String cleaned = extension.replaceAll("[^a-zA-Z0-9.]", "");
        List<String> allowed = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
                ".mp4", ".webm", ".mov", ".avi", ".3gp", ".mkv");
        return allowed.contains(cleaned.toLowerCase()) ? cleaned.toLowerCase() : "";
    }
}
