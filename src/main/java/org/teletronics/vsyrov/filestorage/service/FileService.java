package org.teletronics.vsyrov.filestorage.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.common.exception.DuplicateFileException;
import org.teletronics.vsyrov.filestorage.common.exception.NotFoundException;
import org.teletronics.vsyrov.filestorage.common.io.HashingInputStream;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.service.utility.FileProcessingUtility;
import org.teletronics.vsyrov.filestorage.storage.ContentStorageService;

/**
 * @author vsyrov
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final ContentStorageService storage;
    private final MetadataService metadata;

    public FileMetadata upload(
            String ownerId,
            MultipartFile multipart,
            VisibilityType visibility,
            @Nullable String filenameOverride,
            @Nullable List<String> tags
    ) {
        String filename = (filenameOverride != null && !filenameOverride.isBlank())
                ? filenameOverride.trim()
                : (multipart.getOriginalFilename() != null ? multipart.getOriginalFilename() : "file");

        List<String> normTags = FileProcessingUtility.normalizeTags(tags);
        if (metadata.existsNameForUser(ownerId, filename)) {
            throw new DuplicateFileException("Filename already exists for this user");
        }

        Path temp;
        String sha256;
        long size;
        try (InputStream raw = multipart.getInputStream();
             HashingInputStream his = FileProcessingUtility.hashingStream(raw)) {

            temp = storage.writeTemp(his);
            sha256 = his.digestHex();
            size = his.getBytesRead();
        } catch (Exception e) {
            throw new RuntimeException("Failed to receive upload", e);
        }

        try {
            if (metadata.existsContentForUser(ownerId, sha256)) {
                Files.deleteIfExists(temp);
                throw new DuplicateFileException("Same content already uploaded by this user");
            }

            storage.moveToCas(temp, sha256);
            temp = null;

            String contentType = FileProcessingUtility.detectContentType(
                    Path.of(System.getProperty("filestorage.base", "/data"))
                            .resolve("cas/sha256")
                            .resolve(sha256.substring(0, 2))
                            .resolve(sha256.substring(2, 4))
                            .resolve(sha256),
                    multipart.getContentType()
            );

            return metadata.saveNew(
                    ownerId,
                    filename,
                    contentType,
                    size,
                    sha256,
                    normTags,
                    visibility
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Upload failed", ex);
        } finally {
            try {
                if (temp != null) {
                    Files.deleteIfExists(temp);
                }
            } catch (Exception ignore) {
            }
        }
    }

    public Page<FileMetadata> listPublic(String tag, Pageable pageable) {
        return metadata.listPublic(tag, pageable);
    }

    public Page<FileMetadata> listOwned(String ownerId, String tag, Pageable pageable) {
        return metadata.listOwned(ownerId, tag, pageable);
    }

    public void rename(String ownerId, String fileId, String newName) {
        metadata.rename(ownerId, fileId, newName);
    }

    public void delete(String ownerId, String fileId) {
        FileMetadata m = metadata.getOwned(ownerId, fileId);
        String hash = m.getHash();
        metadata.deleteOwned(ownerId, fileId);
        if (metadata.existsContentForUser(ownerId, hash)) {
            try {
                storage.deleteIfExists(hash);
            } catch (Exception ignore) {
                log.warn("Failed to remove file {} for owner {}", fileId, ownerId, ignore);
            }
        }
    }

    public DownloadResource download(String fileId) {
        FileMetadata m = metadata.getById(fileId);
        try {
            InputStream is = storage.open(m.getHash());
            return new DownloadResource(
                    new InputStreamResource(is),
                    m.getFileName(),
                    m.getContentType()
            );
        } catch (Exception e) {
            throw new NotFoundException("Binary not found");
        }
    }

    public record DownloadResource(
            InputStreamResource body,
            String filename,
            String contentType
    ) {
    }
}

