package org.teletronics.vsyrov.filestorage.it;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author vsyrov
 */
@Slf4j
class SmokeIT extends MongoTestBase {
    @Autowired
    MongoTemplate mongo;

    @Test
    void showMongoPing() {
        var dbs = mongo.getDb().runCommand(Document.parse("{ ping: 1 }"));
        log.info("Mongo ping: {}", dbs.toJson());
    }
}