package org.teletronics.vsyrov.filestorage.api.controller;

import java.time.Instant;
import java.util.List;

import lombok.Data;

/**
 * @author vsyrov
 */
@Data
public class FileDto {
    private String id;
    private String ownerId;
    private String fileName;
    private String contentType;
    private String visibility;
    private List<String> tags;
    private long size;
    private Instant createdTs;
}
