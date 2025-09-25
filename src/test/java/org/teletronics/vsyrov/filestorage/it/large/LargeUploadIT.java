package org.teletronics.vsyrov.filestorage.it.large;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class LargeUploadIT extends MongoTestBase {

    @Autowired
    FileService fileService;

    @Test
    void simulateUpload_ge2GiB_ok() {
        long twoGiB = 2L * 1024 * 1024 * 1024;
        var huge = new SimulatedLargeMultipartFile(
                "file", "huge.bin", "application/octet-stream", twoGiB
        );

        var meta = fileService.upload("userL", huge, VisibilityType.USER_PRIVATE, "huge.bin", List.of("big"));

        assertNotNull(meta.getId());
        assertEquals("huge.bin", meta.getFileName());
        assertEquals(twoGiB, meta.getSize(), "в метаданных должен быть размер ≥ 2GiB");
    }

    @Test
    void uploadHuge_ok() throws IOException {
        long TWO_GIB = 2L * 1024 * 1024 * 1024;
        long PHYS_BYTES = 256L * 1024 * 1024;

        var stream = new ZeroInputStream(PHYS_BYTES);
        var mmf = new MockMultipartFile("file", "huge.bin", "application/octet-stream", stream);
        var meta = fileService.upload("hugeUser", mmf, VisibilityType.USER_PRIVATE, "huge.bin", List.of("big"));

        assertTrue(meta.getSize() >= PHYS_BYTES);
    }

    static class ZeroInputStream extends InputStream {
        private final long total;
        private long read = 0;

        ZeroInputStream(long total) {
            this.total = total;
        }

        @Override
        public int read() {
            if (read >= total) return -1;
            read++;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (read >= total) {
                return -1;
            }
            int toWrite = (int) Math.min(len, total - read);
            for (int i = 0; i < toWrite; i++) {
                b[off + i] = 0;
            }
            read += toWrite;
            return toWrite;
        }
    }
}