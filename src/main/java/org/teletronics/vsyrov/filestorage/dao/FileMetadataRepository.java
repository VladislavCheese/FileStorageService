package org.teletronics.vsyrov.filestorage.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;

/**
 * @author vsyrov
 */
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
    boolean existsByUserIdAndFileName(String userId, String fileName);

    boolean existsByUserIdAndContentHash(String userId, String contentHash);

    Page<FileMetadata> findByUserId(String userId, Pageable pageable);

    Page<FileMetadata> findByVisibility(VisibilityType visibility, Pageable pageable);

    @Query("{ 'userId': ?0, 'tags': ?1 }")
    Page<FileMetadata> findByUserIdAndTag(String userId, String tag, Pageable pageable);

    @Query("{ 'visibility': ?0, 'tags': ?1 }")
    Page<FileMetadata> findByVisibilityAndTag(VisibilityType visibility, String tag, Pageable pageable);
}
