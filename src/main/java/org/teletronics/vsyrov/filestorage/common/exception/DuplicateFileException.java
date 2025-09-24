package org.teletronics.vsyrov.filestorage.common.exception;

/**
 * @author vsyrov
 */
public class DuplicateFileException extends RuntimeException {
    public DuplicateFileException(String message) {
        super(message);
    }

    public DuplicateFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
