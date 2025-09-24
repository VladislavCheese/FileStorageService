package org.teletronics.vsyrov.filestorage.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * @author vsyrov
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HashingInputStream extends InputStream {
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private final DigestInputStream dis;
    private final MessageDigest messageDigest;
    @Getter
    private long bytesRead;

    public static HashingInputStream of(InputStream sourceStream) {
        return of(sourceStream, DEFAULT_HASH_ALGORITHM);
    }

    public static HashingInputStream of(InputStream sourceStream, @Nullable String hashAlgorithm) {
        return new HashingInputStream(sourceStream, hashAlgorithm);
    }

    private HashingInputStream(InputStream sourceStream, @Nullable String hashAlgorithm) {
        requireNonNull(sourceStream, "The source InputStream cannot be null");
        var resolvedHashAlgorithm = hashAlgorithm == null ? DEFAULT_HASH_ALGORITHM : hashAlgorithm;

        try {
            this.messageDigest = MessageDigest.getInstance(resolvedHashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unknown hash algorithm: " + hashAlgorithm, e);
        }
        this.dis = new DigestInputStream(sourceStream, messageDigest);
    }

    @Override
    public int read() throws IOException {
        int b = dis.read();
        if (b != -1) {
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = dis.read(b, off, len);
        if (n > 0) {
            bytesRead += n;
        }
        return n;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        return dis.skip(n);
    }

    @Override
    public int available() throws IOException {
        return dis.available();
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        dis.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        dis.reset();
        messageDigest.reset();
        bytesRead = 0;
    }

    @Override
    public boolean markSupported() {
        return dis.markSupported();
    }

    public byte[] digest() {
        return messageDigest.digest();
    }

    public String digestHex() {
        byte[] d = digest();
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte x : d) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }
}
