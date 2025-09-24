package org.teletronics.vsyrov.filestorage.api.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.api.mapper.FileMapper;
import org.teletronics.vsyrov.filestorage.api.model.FileDto;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.service.FileService;

/**
 * @author vsyrov
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileStorageController {
    private final FileService files;
    private final FileMapper mapper;

    @PostMapping(name = "/v1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> uploadFile(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam("visibility") VisibilityType visibility,
            @RequestParam(value = "tags", required = false) List<String> tags
    ) {
        var meta = files.upload(ownerId, file, visibility, filename, tags);
        return ResponseEntity.status(201).body(mapper.toDto(meta));
    }

    @PatchMapping("/v1/{id}/rename")
    public ResponseEntity<Void> rename(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable String id,
            @RequestBody RenameRequest body
    ) {
        files.rename(ownerId, id, body.filename());
        return ResponseEntity.noContent().build();
    }

    public record RenameRequest(String filename) {
    }

    @DeleteMapping("/v1/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable String id
    ) {
        files.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v1/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        var r = files.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        r.contentType() == null || r.contentType().isBlank()
                                ? "application/octet-stream" : r.contentType()))
                .body(r.body());
    }
}
