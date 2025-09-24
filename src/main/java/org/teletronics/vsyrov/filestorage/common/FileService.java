package org.teletronics.vsyrov.filestorage.common;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;

/**
 * @author vsyrov
 */
public interface FileService {
    String uploadFile(
            String userId,
            MultipartFile file,
            VisibilityType visibility,
            List<String> tags,
            String filenameOverride
    );

    List<FileMetadata> listFiles(
            String userId,
            boolean listPublic,
            String tag,
            Pageable pageable
    );
}
