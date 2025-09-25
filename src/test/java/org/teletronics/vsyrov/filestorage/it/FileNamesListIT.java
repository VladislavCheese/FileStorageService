package org.teletronics.vsyrov.filestorage.it;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.teletronics.vsyrov.filestorage.api.mapper.FileMapper;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vsyrov
 */
public class FileNamesListIT extends MongoTestBase {

    @Autowired
    FileService files;
    @Autowired
    FileMapper mapper;

    @Test
    void twoUsersHavePublic_listPublic_withTagAndSort() {
        files.upload("userE", new MockMultipartFile("f", "p1.txt", "text/plain", "1".getBytes()),
                VisibilityType.PUBLIC, "p1.txt", List.of("Alpha"));
        files.upload("userE", new MockMultipartFile("f", "s1.txt", "text/plain", "2".getBytes()),
                VisibilityType.USER_PRIVATE, "s1.txt", List.of("Beta"));

        // userF: public tag=alpha
        files.upload("userF", new MockMultipartFile("f", "p2.txt", "text/plain", "3".getBytes()),
                VisibilityType.PUBLIC, "p2.txt", List.of("alpha"));

        var page = files.listPublic("ALPHA", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fileName")));

        var list = page.map(mapper::toDto).getContent();
        assertEquals(2, list.size());
        assertEquals("p1.txt", list.get(0).getFileName());
        assertEquals("p2.txt", list.get(1).getFileName());
    }

    @Test
    void oneUserHavePublic_listPublic_withTagAndSort() {
        // userE: private tag=alpha / private tag=beta
        files.upload("userE", new MockMultipartFile("f","p1.txt","text/plain","1".getBytes()),
                VisibilityType.USER_PRIVATE, "p1.txt", List.of("Alpha"));
        files.upload("userE", new MockMultipartFile("f","s1.txt","text/plain","2".getBytes()),
                VisibilityType.USER_PRIVATE, "s1.txt", List.of("Beta"));

        // userF: public tag=alpha  (только этот попадёт в публичный список)
        files.upload("userF", new MockMultipartFile("f","p2.txt","text/plain","3".getBytes()),
                VisibilityType.PUBLIC, "p2.txt", List.of("alpha"));

        var page = files.listPublic("ALPHA",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fileName")));

        var list = page.map(mapper::toDto).getContent();
        assertEquals(1, list.size());
        assertEquals("p2.txt", list.getFirst().getFileName());
    }
}
