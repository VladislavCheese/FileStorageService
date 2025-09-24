package org.teletronics.vsyrov.filestorage.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author vsyrov
 */
@Component
public class FileSystemCasStorage implements ContentStorageService {
    private final Path base;
    private final Path tmp;
    private final Path cas;

    public FileSystemCasStorage() throws IOException {
        this.base = Path.of(System.getProperty("filestorage.path", "/data"));
        this.tmp = base.resolve("tmp");
        this.cas = base.resolve("cas/sha256");
        Files.createDirectories(tmp);
        Files.createDirectories(cas);
    }

    @Override
    public Path writeTemp(InputStream in) throws IOException {
        Path t = Files.createTempFile(tmp, "up-", ".part");
        try (OutputStream out = Files.newOutputStream(t, WRITE)) {
            in.transferTo(out);
        }
        return t;
    }

    @Override
    public Path moveToCas(Path temp, String sha256) throws IOException {
        String a = sha256.substring(0, 2);
        String b = sha256.substring(2, 4);
        Path dir = cas.resolve(a).resolve(b);
        Files.createDirectories(dir);
        Path target = dir.resolve(sha256);
        if (Files.exists(target)) {
            Files.deleteIfExists(temp); // контент уже есть — temp не нужен
            return target;
        }
        return Files.move(temp, target, ATOMIC_MOVE);
    }

    @Override
    public InputStream open(String sha256) throws IOException {
        Path p = pathOf(sha256);
        return Files.newInputStream(p, READ);
    }

    @Override
    public boolean deleteIfExists(String sha256) throws IOException {
        Path p = pathOf(sha256);
        return Files.deleteIfExists(p);
    }

    @Override
    public boolean exists(String sha256) throws IOException {
        return Files.exists(pathOf(sha256));
    }

    private Path pathOf(String sha256) {
        return cas.resolve(sha256.substring(0, 2))
                .resolve(sha256.substring(2, 4))
                .resolve(sha256);
    }
}
