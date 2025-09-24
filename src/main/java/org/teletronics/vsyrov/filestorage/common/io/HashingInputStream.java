package org.teletronics.vsyrov.filestorage.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.lang.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * @author vsyrov
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HashingInputStream extends InputStream {
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private final InputStream sourceStream;
    private final MessageDigest messageDigest;
    @Getter
    private long bytesRead;

    public static HashingInputStream of(InputStream sourceStream) {
        return of(sourceStream, DEFAULT_HASH_ALGORITHM);
    }

    public static HashingInputStream of(InputStream sourceStream, @Nullable String hashAlgorithm) {
        requireNonNull(sourceStream, "The source InputStream cannot be null");
        var resolvedHashAlgorithm = hashAlgorithm == null ? DEFAULT_HASH_ALGORITHM : hashAlgorithm;
        return new HashingInputStream(sourceStream, resolvedHashAlgorithm);
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private HashingInputStream(InputStream sourceStream, String hashAlgorithm) {
        this.sourceStream = sourceStream;
        this.messageDigest = MessageDigest.getInstance(hashAlgorithm);
    }

    @Override
    public int read() throws IOException {
        int b = sourceStream.read();
        if (b != -1) {
            messageDigest.update((byte) b);
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int bytes = sourceStream.read(b, off, len);
        if (bytes != -1) {
            messageDigest.update(b, off, bytes);
            bytesRead += bytes;
        }
        return bytes;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        return sourceStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return sourceStream.available();
    }

    @Override
    public void close() throws IOException {
        sourceStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        sourceStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        sourceStream.reset();
        messageDigest.reset();
        bytesRead = 0;
    }


    @Override
    public boolean markSupported() {
        return sourceStream.markSupported();
    }

    public byte[] getHash() {
        return messageDigest.digest();
    }
}
