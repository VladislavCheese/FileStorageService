package org.teletronics.vsyrov.filestorage.it;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.it.config.MongoTestBase;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vsyrov
 */
@AutoConfigureMockMvc
public class ControllerDownloadIT extends MongoTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    FileService fileService;

    @Test
    void uploadAndDownload_ok() throws Exception {
        var meta = fileService.upload("userZ",
                new MockMultipartFile("file", "d.txt", "text/plain", "data".getBytes()),
                VisibilityType.PUBLIC, "d.txt", List.of());

        mvc.perform(get("/file/v1/{id}", meta.getId()).header("X-User-Id", "userZ"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("filename=\"d.txt\"")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }
}
