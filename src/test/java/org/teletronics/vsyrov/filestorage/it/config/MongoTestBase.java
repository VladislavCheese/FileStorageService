package org.teletronics.vsyrov.filestorage.it.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author vsyrov
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "filestorage.base-path=${java.io.tmpdir}/filestorage-test"
        }
)
public abstract class MongoTestBase {

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        if (!MONGO.isRunning()) {
            MONGO.start();
        }
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @BeforeEach
    void before() throws IOException {
        cleanupBase();
        Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir"), "filestorage-test"));
    }

    @AfterEach
    void after() throws IOException {
        cleanupBase();
    }

    private void cleanupBase() throws IOException {
        Path base = Paths.get(System.getProperty("java.io.tmpdir"), "filestorage-test");
        if (Files.exists(base)) {
            try (var s = Files.walk(base)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }
}
