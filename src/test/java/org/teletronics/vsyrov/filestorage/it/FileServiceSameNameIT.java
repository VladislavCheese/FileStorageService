package org.teletronics.vsyrov.filestorage.it;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.it.config.MongoTestBase;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author vsyrov
 */
public class FileServiceSameNameIT extends MongoTestBase {

    @Autowired
    FileService files;

    @Test
    void parallelUpload_sameFilename_conflictOne() throws Exception {
        String owner = "userA";
        String fileName = "same.txt";

        var f1 = new MockMultipartFile("file", fileName, "text/plain", "hello".getBytes());
        var f2 = new MockMultipartFile("file", fileName, "text/plain", "world".getBytes());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> c1 = () -> files.upload(owner, f1, VisibilityType.USER_PRIVATE, fileName, List.of()).getId();
        Callable<String> c2 = () -> files.upload(owner, f2, VisibilityType.USER_PRIVATE, fileName, List.of()).getId();

        Future<String> r1 = pool.submit(c1);
        Future<String> r2 = pool.submit(c2);
        pool.shutdown();

        int conflicts = 0;
        int successes = 0;
        for (Future<String> f : List.of(r1, r2)) {
            try {
                String id = f.get(30, TimeUnit.SECONDS);
                assertNotNull(id);
                successes++;
            } catch (ExecutionException ex) {
                conflicts++;
            }
        }

        assertEquals(1, successes, "one successfully upload status");
        assertEquals(1, conflicts, "one result 409 status");
    }
}