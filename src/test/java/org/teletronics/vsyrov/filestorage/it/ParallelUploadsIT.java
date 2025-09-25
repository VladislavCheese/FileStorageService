package org.teletronics.vsyrov.filestorage.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;
import org.teletronics.vsyrov.filestorage.common.model.VisibilityType;
import org.teletronics.vsyrov.filestorage.service.FileService;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author vsyrov
 */
public class ParallelUploadsIT extends MongoTestBase {
    @Autowired
    FileService fileService;

    @Test
    void sameFilename_parallel_conflict() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        try {
            Callable<Object> task = () -> fileService.upload(
                    "userA",
                    new MockMultipartFile("f", "same.txt", "text/plain", "DATA".getBytes()),
                    VisibilityType.USER_PRIVATE,
                    "same.txt",
                    List.of("tag1")
            );

            List<Future<Object>> futures = pool.invokeAll(List.of(task, task, task, task));
            int success = 0, dup = 0;
            for (var f : futures) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    success++;
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof DuplicateKeyException) dup++;
                    else throw ex;
                }
            }
            assertTrue(success >= 1);
            assertTrue(dup >= 1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sameContent_parallel_conflict() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        try {
            Callable<Object> t1 = () -> fileService.upload(
                    "userB",
                    new MockMultipartFile("f", "a.txt", "text/plain", "SAME".getBytes()),
                    VisibilityType.USER_PRIVATE,
                    "a.txt",
                    List.of("x")
            );
            Callable<Object> t2 = () -> fileService.upload(
                    "userB",
                    new MockMultipartFile("f", "b.txt", "text/plain", "SAME".getBytes()),
                    VisibilityType.USER_PRIVATE,
                    "b.txt",
                    List.of("y")
            );
            var futures = new ArrayList<>(pool.invokeAll(List.of(t1, t2, t1, t2)));
            int success = 0;
            int dup = 0;
            for (var f : futures) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    success++;
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof DuplicateKeyException) {
                        dup++;
                    } else {
                        throw ex;
                    }
                }
            }
            assertTrue(success >= 1);
            assertTrue(dup >= 1);
        } finally {
            pool.shutdownNow();
        }
    }
}
