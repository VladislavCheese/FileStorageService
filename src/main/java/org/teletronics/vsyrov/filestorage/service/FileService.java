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
import org.teletronics.vsyrov.filestorage.common.exception.ForbiddenException;
import org.teletronics.vsyrov.filestorage.common.exception.UnexpectedStorageException;
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
        String filename = FileProcessingUtility.defineFileName(multipart, filenameOverride);

        List<String> normTags = FileProcessingUtility.normalizeTags(tags);
        if (metadata.existsNameForUser(ownerId, filename)) {
            throw new DuplicateFileException("Filename already exists for this user");
        }

        Path temp;
        String contentHash;
        long size;
        try (InputStream raw = multipart.getInputStream();
             HashingInputStream his = FileProcessingUtility.hashingStream(raw)) {

            temp = storage.writeTemp(his);
            contentHash = his.digestHex();
            size = (multipart.getSize() > 0) ? multipart.getSize() : his.getBytesRead();
        } catch (Exception e) {
            throw new RuntimeException("Failed to receive upload", e);
        }

        try {
            if (metadata.existsContentForUser(ownerId, contentHash)) {
                Files.deleteIfExists(temp);
                throw new DuplicateFileException("Same content already uploaded by this user");
            }

            storage.moveToCas(temp, contentHash);
            temp = null;

            String contentType = FileProcessingUtility.detectContentType(
                    storage.resolvePath(contentHash),
                    multipart.getContentType()
            );

            return metadata.saveNew(
                    ownerId,
                    filename,
                    contentType,
                    size,
                    contentHash,
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
        FileMetadata meta = metadata.getOwned(ownerId, fileId);
        String hash = meta.getHash();
        metadata.deleteOwned(ownerId, fileId);
        if (metadata.existsContentForUser(ownerId, hash)) {
            try {
                storage.deleteIfExists(hash);
            } catch (Exception exc) {
                log.warn("Failed to remove file {} for owner {}", fileId, ownerId, exc);
                //todo throw exc
            }
        }
    }

    public DownloadResource download(String fileId, String userId) {
        FileMetadata meta = metadata.getById(fileId);
        if (!meta.getOwnerId().equals(userId) && meta.getVisibility() != VisibilityType.PUBLIC) {
            throw new ForbiddenException("Download file " + fileId + " unavailable for user " + userId);
        }
        try {
            InputStream is = storage.open(meta.getHash());
            return new DownloadResource(
                    new InputStreamResource(is),
                    meta.getFileName(),
                    meta.getContentType()
            );
        } catch (Exception e) {
            log.warn("Failed to download file: {}", fileId, e);
            throw new UnexpectedStorageException("Download failed for file " + fileId, e);
        }
    }

    public List<String> getAccessibleTags(String userId) {
        //todo use metadata service for tags
        return List.of();
    }

    public record DownloadResource(
            InputStreamResource body,
            String filename,
            String contentType
    ) {
    }
}

