package org.teletronics.vsyrov.filestorage.service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.common.FileService;
import org.teletronics.vsyrov.filestorage.common.exception.BadRequestException;
import org.teletronics.vsyrov.filestorage.common.exception.DuplicateFileException;
import org.teletronics.vsyrov.filestorage.common.exception.ForbiddenException;
import org.teletronics.vsyrov.filestorage.common.exception.NotFoundException;
import org.teletronics.vsyrov.filestorage.common.exception.UnexpectedStorageException;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.dao.FileMetadataRepository;

/**
 * @author vsyrov
 */
@Service
@RequiredArgsConstructor
public class BasicFileService implements FileService {
    private final FileMetadataRepository fileMetadataRepository;
    private final GridFsTemplate gridFsTemplate;

    /**
     * Upload a new file to storage.
     *
     * @return the unique ID (download link) for the stored file.
     */
    @Override
    public String uploadFile(
            String userId,
            MultipartFile file,
            VisibilityType visibility,
            List<String> tags,
            String filenameOverride
    ) {
        String originalName = (filenameOverride != null && !filenameOverride.isBlank())
                ? filenameOverride
                : file.getOriginalFilename();
        if (originalName == null) originalName = "file";

        if (tags != null) {
            tags = tags.stream()
                    .limit(5)
                    .map(String::toLowerCase)
                    .distinct()
                    .toList();
        }

        if (fileMetadataRepository.existsByUserIdAndFileName(userId, originalName)) {
            throw new DuplicateKeyException("File name already exists for user");
        }

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        ObjectId contentId = null;
        try (InputStream inputStream = file.getInputStream()) {
            contentId = gridFsTemplate.store(inputStream, originalName, file.getContentType());
        } catch (Exception e) {
            throw new UnexpectedStorageException("Failed to store file content", e);
        }

        var gridFsFile = gridFsTemplate.findOne(queryById(contentId));
        String contentHash;
        if (gridFsFile.getMD5() != null) {
            contentHash = gridFsFile.getMD5();
        } else {
            GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
            try (InputStream is = resource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            contentHash = bytesToHex(md.digest());
        }

        if (fileMetadataRepository.existsByUserIdAndContentHash(userId, contentHash)) {
            gridFsTemplate.delete(queryById(contentId));
            throw new DuplicateFileException("File content already exists for user");
        }

        FileMetadata fileMeta = FileMetadata.builder()
                .id(contentId.toHexString())
                .userId(userId)
                .fileName(originalName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .visibility(visibility)
                .tags(tags)
                .contentHash(contentHash)
                .createdTs(Instant.now())
                .modifiedTs(Instant.now())
                .version(1)
                .build();

        try {
            fileMetadataRepository.save(fileMeta);
        } catch (DuplicateKeyException dk) {
            gridFsTemplate.delete(queryById(contentId));
            throw new DuplicateFileException("Duplicate file detected", dk);
        }

        return fileMeta.getId(); // return the unique download link (file ID)
    }

    /**
     * List files with optional tag filtering and sorting/pagination.
     */
    @Override
    public List<FileMetadata> listFiles(
            String userId,
            boolean listPublic,
            String tag,
            Pageable pageable
    ) {
        if (tag != null) {
            tag = tag.toLowerCase();
        }
        if (listPublic) {
            if (tag != null && !tag.isEmpty()) {
                return fileMetadataRepository.findByVisibilityAndTag(VisibilityType.PUBLIC, tag, pageable).getContent();
            } else {
                return fileMetadataRepository.findByVisibility(VisibilityType.PUBLIC, pageable).getContent();
            }
        } else {
            if (tag != null && !tag.isEmpty()) {
                return fileMetadataRepository.findByUserIdAndTag(userId, tag, pageable).getContent();
            } else {
                return fileMetadataRepository.findByUserId(userId, pageable).getContent();
            }
        }
    }

    /**
     * Rename an existing file (change its filename).
     */
    @Override
    public void renameFile(
            String userId,
            String fileId,
            String newFilename
    ) {
        var fileMetadata = fileMetadataRepository.findById(fileId).orElseThrow(
                () -> new NotFoundException("File not found with id: " + fileId)
        );
        if (!fileMetadata.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to rename this file");
        }
        newFilename = newFilename.trim();
        if (newFilename.isEmpty()) {
            throw new BadRequestException("New filename cannot be empty");
        }
        if (newFilename.equals(fileMetadata.getFileName())) {
            return;
        }
        if (fileMetadataRepository.existsByUserIdAndFileName(userId, newFilename)) {
            throw new DuplicateFileException("File name already exists for user");
        }
        fileMetadataRepository.save(fileMetadata.toBuilder().fileName(newFilename).modifiedTs(Instant.now()).build());
    }

    /**
     * Delete a file owned by the user.
     */
    @Override
    public void deleteFile(String userId, String fileId) {
        var fileMetadata = fileMetadataRepository.findById(fileId).orElseThrow(
                () -> new NotFoundException("File not found with id: " + fileId)
        );
        if (!fileMetadata.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to delete this file");
        }
        fileMetadataRepository.delete(fileMetadata);
        gridFsTemplate.delete(queryById(new ObjectId(fileId)));
    }

    /**
     * Retrieve file content for downloading.
     */
    @Override
    public GridFsResource getFileResource(String fileId) {
        var fileMetadata = fileMetadataRepository.findById(fileId).orElseThrow(
                () -> new NotFoundException("File not found with id: " + fileId)
        );
        ObjectId objId;
        try {
            objId = new ObjectId(fileId);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Invalid file id");
        }
        var gridFsFile = gridFsTemplate.findOne(queryById(objId));
        if (gridFsFile == null) {
            throw new NotFoundException("File content not found");
        }
        return gridFsTemplate.getResource(gridFsFile);
    }

    private Query queryById(ObjectId id) {
        return new Query(
                Criteria.where("_id").is(id)
        );
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
