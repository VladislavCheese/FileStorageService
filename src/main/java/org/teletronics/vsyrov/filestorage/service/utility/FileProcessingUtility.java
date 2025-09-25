package org.teletronics.vsyrov.filestorage.service.utility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.common.io.HashingInputStream;

/**
 * @author vsyrov
 */
@Slf4j
@UtilityClass
public class FileProcessingUtility {
    private static final Tika TIKA = new Tika();

    public static HashingInputStream hashingStream(InputStream in) {
        return HashingInputStream.of(in, "SHA-256");
    }

    public static String detectContentType(Path file, String fallback) {
        try {
            String t = TIKA.detect(file);
            if (t != null && !t.isBlank() && !t.equals("application/octet-stream")) {
                return t;
            }
        } catch (Exception exc) {
            log.debug("Could not determine content type using tika for file {}", file, exc);
        }

        try {
            String t = Files.probeContentType(file);
            if (t != null && !t.isBlank()) {
                return t;
            }
        } catch (IOException exc) {
            log.debug("Could not determine content type using tika for file {}", file, exc);
        }

        return (fallback == null || fallback.isBlank()) ? "application/octet-stream" : fallback;
    }

    public static String detectContentTypeByName(String fileName, String fallback) {
        try {
            String t = TIKA.detect(fileName);
            if (t != null && !t.isBlank() && !t.equals("application/octet-stream")) {
                return t;
            }
        } catch (Exception exc) {
            log.debug("Could not determine content type using tika for fileName {}", fileName, exc);
        }
        return (fallback == null || fallback.isBlank()) ? "application/octet-stream" : fallback;
    }

    /**
     * lowercase, trim, ≤5, deduplicate
     */
    public static List<String> normalizeTags(@Nullable List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(s -> s == null ? "" : s.trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(5)
                .toList();
    }

    public static String defineFileName(MultipartFile multipart, @Nullable String filenameOverride) {
        return (filenameOverride != null && !filenameOverride.isBlank())
                ? filenameOverride.trim()
                : (multipart.getOriginalFilename() != null ? multipart.getOriginalFilename() : "file");
    }
}
