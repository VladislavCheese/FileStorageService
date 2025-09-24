package org.teletronics.vsyrov.filestorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author vsyrov
 */
@SpringBootApplication
public class FileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileStorageServiceApplication.class, args);
    }
}