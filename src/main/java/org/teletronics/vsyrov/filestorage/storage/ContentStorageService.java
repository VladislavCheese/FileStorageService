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
     * Переместить temp в CAS по contentHash и вернуть финальный Path (атомарно).
     */
    Path moveToCas(Path temp, String contentHash) throws IOException;

    /**
     * Открыть поток для чтения по contentHash.
     */
    InputStream open(String contentHash) throws IOException;

    /**
     * Удалить файл по contentHash (если он вообще нужен; обычно GC по ссылкам).
     */
    boolean deleteIfExists(String contentHash) throws IOException;

    /**
     * Проверить существование файла по contentHash.
     */
    boolean exists(String contentHash) throws IOException;

    Path resolvePath(String contentHash);
}
