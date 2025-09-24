package org.teletronics.vsyrov.filestorage.api.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.service.BasicFileService;

/**
 * @author vsyrov
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileStorageController {
    private final BasicFileService fileStorageService;

    @PostMapping
    public ResponseEntity<String> uploadFile(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("visibility") VisibilityType visibility,
            @RequestParam(value = "filename", required = false) @Nullable String filename,
            @RequestParam(value = "tags", required = false) @Nullable List<String> tags
    ) {
        String fileId = fileStorageService.uploadFile(userId, file, visibility, tags, filename);
        return ResponseEntity.ok(fileId);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadata>> listFiles(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "public", required = false) Boolean listPublic,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "fileName,asc") String sort
    ) {
        boolean getPublic = (listPublic != null && listPublic) || (userId == null);
        String[] sortParts = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        var files = fileStorageService.listFiles(userId, getPublic, tag, pageable);
        return ResponseEntity.ok(files);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> renameFile(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id,
            @RequestParam("filename") String newFilename
    ) {
        fileStorageService.renameFile(userId, id, newFilename);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id
    ) {
        fileStorageService.deleteFile(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable String id) {
        GridFsResource resource = fileStorageService.getFileResource(id);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        String fileName = id;
        try {
            fileName = resource.getFilename();
            String ct = resource.getContentType();
            if (ct != null && !ct.isEmpty()) {
                contentType = MediaType.parseMediaType(ct);
            }
        } catch (Exception e) {
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(contentType)
                .body(resource);
    }
}
