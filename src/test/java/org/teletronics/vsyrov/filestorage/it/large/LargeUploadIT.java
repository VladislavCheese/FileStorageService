package org.teletronics.vsyrov.filestorage.it.large;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.it.MongoTestBase;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author vsyrov
 */
@Import(LargeFileTestConfig.class)
public class LargeUploadIT extends MongoTestBase {

    @Autowired
    FileService files;

    @Test
    void simulateUpload_ge2GiB_ok() {
        var huge = new MockMultipartFile("file", "huge.bin", "application/octet-stream", new byte[0]);
        var meta = files.upload("userL", huge, VisibilityType.USER_PRIVATE, "huge.bin", List.of("big"));
        assertNotNull(meta.getId());
        assertEquals("huge.bin", meta.getFileName());
        assertTrue(meta.getSize() >= 2L * 1024 * 1024 * 1024, "size should be >= 2GiB");
    }
}