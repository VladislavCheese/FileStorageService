package org.teletronics.vsyrov.filestorage.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author vsyrov
 */
@Slf4j
@Component
public class FileSystemCasStorage implements ContentStorageService {
    private final Path tmp;
    private final Path cas;

    @Autowired
    public FileSystemCasStorage(
            @Value("${filestorage.base-path:${java.io.tmpdir}/filestorage}") String baseDir
    ) throws IOException {
        Path base = Paths.get(baseDir).toAbsolutePath().normalize();
        this.tmp = base.resolve("tmp");
        this.cas = base.resolve("cas/sha256");
        Files.createDirectories(tmp);
        Files.createDirectories(cas);
    }

    @Override
    public Path writeTemp(InputStream in) throws IOException {
        Files.createDirectories(tmp);
        Path tmpFile = Files.createTempFile(tmp, "up-", ".part");
        try (OutputStream out = Files.newOutputStream(tmpFile, WRITE, TRUNCATE_EXISTING)) {
            in.transferTo(out);
            out.flush();
            return tmpFile;
        }
    }

    @Override
    public Path moveToCas(Path temp, String contentHash) throws IOException {
        Path dir = resolvePath(contentHash);
        Files.createDirectories(dir.getParent());
        Path target = dir.resolve(contentHash);
        if (Files.exists(target)) {
            log.info("File already exists at {}", target);
            Files.deleteIfExists(temp);
            return target;
        }
        try {
            return Files.move(temp, target, ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            log.debug("Atomic move operation not supported", e);
            return Files.move(temp, target, REPLACE_EXISTING);
        }
    }

    @Override
    public InputStream open(String contentHash) throws IOException {
        Path p = resolvePath(contentHash);
        return Files.newInputStream(p, READ);
    }

    @Override
    public boolean deleteIfExists(String contentHash) throws IOException {
        Path p = resolvePath(contentHash);
        return Files.deleteIfExists(p);
    }

    @Override
    public boolean exists(String contentHash) throws IOException {
        return Files.exists(resolvePath(contentHash));
    }

    @Override
    public Path resolvePath(String contentHash) {
        return cas.resolve(contentHash.substring(0, 2))
                .resolve(contentHash.substring(2, 4))
                .resolve(contentHash);
    }
}
