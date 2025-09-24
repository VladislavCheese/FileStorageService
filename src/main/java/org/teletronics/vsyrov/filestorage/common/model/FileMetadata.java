package org.teletronics.vsyrov.filestorage.common.model;

/**
 * @author vsyrov
 */

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder(toBuilder = true)
@Document(collection = "files_metadata")
@CompoundIndex(name = "uniq_name_per_user", def = "{'userId': 1, 'fileName': 1}", unique = true)
@CompoundIndex(name = "uniq_content_per_user", def = "{'userId': 1, 'contentHash': 1}", unique = true)
public class FileMetadata {
    @Id
    String id;
    String userId;
    String fileName;
    String contentType;
    VisibilityType visibility;
    List<String> tags;
    String contentHash;
    long size;
    Instant createdTs;
    Instant modifiedTs;
    int version;
}