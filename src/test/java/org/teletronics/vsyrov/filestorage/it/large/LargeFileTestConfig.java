package org.teletronics.vsyrov.filestorage.it.large;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.teletronics.vsyrov.filestorage.service.utility.FileProcessingUtility;
import org.teletronics.vsyrov.filestorage.storage.ContentStorageService;

/**
 * @author vsyrov
 */
@TestConfiguration
public class LargeFileTestConfig {

    @Bean
    public ContentStorageService noOpStorage() {
        return new ContentStorageService() {
            @Override
            public Path writeTemp(InputStream in) throws IOException {
                in.transferTo(OutputStream.nullOutputStream()); // сливаем, но быстро
                return Path.of("/dev/null"); // фиктивный путь
            }

            @Override
            public Path moveToCas(Path temp, String sha256) {
                return temp;
            }

            @Override
            public InputStream open(String sha256) {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public boolean deleteIfExists(String sha256) {
                return true;
            }

            @Override
            public boolean exists(String sha256) {
                return true;
            }
        };
    }

    @Bean
    public FileProcessingUtility fakeProcessing() {
        return new FileProcessingService() {
            @Override
            public TestHashingInputStream hashingStream(InputStream in) {
                return new TestHashingInputStream(in) {
                    private long bytes = 2L * 1024 * 1024 * 1024; // 2 GiB
                    @Override public int read() { return -1; } // не читаем реально
                    @Override public int read(byte[] b, int off, int len) { return -1; }
                    @Override public long getBytesRead() { return bytes; }
                    @Override public String digestHex() { return "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"; }
                };
            }
        };
    }

    /** Вспомогательный анонимный HashingInputStream с переопределённым поведением */
    static abstract class TestHashingInputStream extends HashingInputStream {
        protected TestHashingInputStream(InputStream in) { HashingInputStream(in, "SHA-256"); }
        @Override public abstract int read() throws IOException;
        @Override public abstract int read(byte[] b, int off, int len) throws IOException;
        @Override public abstract long getBytesRead();
        @Override public abstract String digestHex();
    }
}