package org.teletronics.vsyrov.filestorage.common.model;

/**
 * @author vsyrov
 */

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "metadata")
@CompoundIndex(name = "uniq_filename_per_owner", def = "{'ownerId': 1, 'fileName': 1}", unique = true)
@CompoundIndex(name = "uniq_hash_per_owner", def = "{'ownerId': 1, 'hash': 1}", unique = true)
public class FileMetadata {
    @Id
    String id;
    String ownerId;
    String fileName;
    String contentType;
    VisibilityType visibility;
    List<String> tags;
    String hash;
    long size;
    Instant createdTs;
}