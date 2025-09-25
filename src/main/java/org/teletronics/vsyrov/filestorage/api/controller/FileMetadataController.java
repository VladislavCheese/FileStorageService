package org.teletronics.vsyrov.filestorage.api.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.teletronics.vsyrov.filestorage.api.mapper.FileMapper;
import org.teletronics.vsyrov.filestorage.service.FileService;

/**
 * @author vsyrov
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileMetadataController {

    private final FileService fileService;
    private final FileMapper mapper;

    @GetMapping("/v1/public")
    public ResponseEntity<List<FileDto>> listPublicFiles(
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "filename,asc") String sort
    ) {
        Pageable p = pageable(sort, page, size);
        var pageRes = fileService.listPublic(tag, p);
        return ResponseEntity.ok(pageRes.getContent().stream().map(mapper::toDto).toList());
    }

    @GetMapping(path = "/v1/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<String>> listTags(
            @RequestHeader("X-User-Id") String userId
    ) {
        return Collections.singletonMap("tags", fileService.getAccessibleTags(userId));
    }

    @GetMapping("/v1")
    public ResponseEntity<List<FileDto>> listUserFiles(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "filename,asc") String sort
    ) {
        Pageable p = pageable(sort, page, size);
        var pageRes = fileService.listOwned(ownerId, tag, p);
        return ResponseEntity.ok(pageRes.getContent().stream().map(mapper::toDto).toList());
    }

    private Pageable pageable(String sort, int page, int size) {
        String[] sp = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(sp.length > 1 ? sp[1] : "asc"), sp[0]);
        return PageRequest.of(page, size, s);
    }
}
