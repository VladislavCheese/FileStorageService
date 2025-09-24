package org.teletronics.vsyrov.filestorage.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.teletronics.vsyrov.filestorage.common.exception.BadRequestException;
import org.teletronics.vsyrov.filestorage.common.exception.DuplicateFileException;
import org.teletronics.vsyrov.filestorage.common.exception.ForbiddenException;
import org.teletronics.vsyrov.filestorage.common.exception.NotFoundException;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.dao.FileMetadataRepository;

/**
 * @author vsyrov
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final FileMetadataRepository repo;

    public boolean existsNameForUser(String ownerId, String filename) {
        return repo.existsByOwnerIdAndFileName(ownerId, filename);
    }

    public boolean existsContentForUser(String ownerId, String hash) {
        return repo.existsByOwnerIdAndHash(ownerId, hash);
    }

    public FileMetadata saveNew(
            String ownerId,
            String fileName,
            String contentType,
            long size,
            String hash,
            List<String> tags,
            VisibilityType visibility
    ) {
        var meta = FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .ownerId(ownerId)
                .fileName(fileName)
                .contentType(contentType)
                .visibility(visibility)
                .tags(tags)
                .hash(hash)
                .size(size)
                .createdTs(Instant.now())
                .build();
        try {
            return repo.save(meta);
        } catch (DuplicateKeyException e) {
            throw new DuplicateFileException("Duplicate by name or content", e);
        }
    }

    public void rename(String userId, String fileId, String newName) {
        if (StringUtils.isEmpty(newName)) {
            throw new BadRequestException("New filename cannot be empty");
        }
        var trimmedNewName = newName.trim();

        FileMetadata meta = getOwned(userId, fileId);
        log.info("Renaming filename from {} to {}", meta.getFileName(), trimmedNewName);
        if (trimmedNewName.equals(meta.getFileName())) {
            return;
        }
        if (repo.existsByOwnerIdAndFileName(userId, trimmedNewName)) {
            throw new DuplicateFileException("Filename already exists for this user");
        }
        meta.toBuilder().fileName(trimmedNewName).build();
        repo.save(meta);
    }

    public void deleteOwned(String userId, String fileId) {
        FileMetadata meta = getOwned(userId, fileId);
        repo.delete(meta);
    }

    public FileMetadata getById(String fileId) {
        return repo.findById(fileId).orElseThrow(() -> new NotFoundException("File not found"));
    }

    public FileMetadata getOwned(String userId, String fileId) {
        FileMetadata meta = getById(fileId);
        if (!meta.getOwnerId().equals(userId)) {
            log.info("Forbidden access on file {} for user {}", fileId, userId);
            throw new ForbiddenException("You do not have permission to this file");
        }
        return meta;
    }

    public Page<FileMetadata> listPublic(@Nullable String tag, Pageable pageable) {
        return (tag == null || tag.isBlank())
                ? repo.findByVisibility(VisibilityType.PUBLIC, pageable)
                : repo.findByVisibilityAndTagsContains(VisibilityType.PUBLIC, tag.toLowerCase(), pageable);
    }

    public Page<FileMetadata> listOwned(String ownerId, @Nullable String tag, Pageable pageable) {
        return (tag == null || tag.isBlank())
                ? repo.findByUserId(ownerId, pageable)
                : repo.findByOwnerIdAndTagsContains(ownerId, tag.toLowerCase(), pageable);
    }

    public long countByHash(String hash) {
        return repo.countByHash(hash);
    }
}