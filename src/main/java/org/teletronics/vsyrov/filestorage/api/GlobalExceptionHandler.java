package org.teletronics.vsyrov.filestorage.api;

/**
 * @author vsyrov
 */

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.teletronics.vsyrov.filestorage.common.exception.BadRequestException;
import org.teletronics.vsyrov.filestorage.common.exception.DuplicateFileException;
import org.teletronics.vsyrov.filestorage.common.exception.ForbiddenException;
import org.teletronics.vsyrov.filestorage.common.exception.NotFoundException;
import org.teletronics.vsyrov.filestorage.common.exception.UnexpectedStorageException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateFileException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<String> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<String> handleBadRequest(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler({UnexpectedStorageException.class, Exception.class})
    public ResponseEntity<String> handleServerErrors(Exception e) {
        // Catch-all for other exceptions
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error: " + e.getMessage());
    }
}