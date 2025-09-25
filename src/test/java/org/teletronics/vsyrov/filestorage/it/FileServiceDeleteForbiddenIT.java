package org.teletronics.vsyrov.filestorage.it;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.teletronics.vsyrov.filestorage.common.exception.ForbiddenException;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.it.config.MongoTestBase;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.junit.Assert.assertThrows;

/**
 * @author vsyrov
 */
public class FileServiceDeleteForbiddenIT extends MongoTestBase {

    @Autowired
    FileService fileService;

    @Test
    void deleteNotOwned_forbidden() {
        var meta = fileService.upload(
                "userC",
                new MockMultipartFile("file", "x.txt", "text/plain", "abc".getBytes()),
                VisibilityType.USER_PRIVATE,
                "x.txt",
                List.of()
        );

        assertThrows(ForbiddenException.class, () -> fileService.delete("userD", meta.getId()));
    }
}
