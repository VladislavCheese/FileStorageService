package org.teletronics.vsyrov.filestorage.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author vsyrov
 */
public interface ContentStorageService {
    /**
     * Записать поток во временный файл и вернуть путь.
     */
    Path writeTemp(InputStream in) throws IOException;

    /**
     * Переместить temp в CAS по sha256 и вернуть финальный Path (атомарно).
     */
    Path moveToCas(Path temp, String sha256) throws IOException;

    /**
     * Открыть поток для чтения по sha256.
     */
    InputStream open(String sha256) throws IOException;

    /**
     * Удалить файл по sha256 (если он вообще нужен; обычно GC по ссылкам).
     */
    boolean deleteIfExists(String sha256) throws IOException;

    /**
     * Проверить существование файла по sha256.
     */
    boolean exists(String sha256) throws IOException;
}
