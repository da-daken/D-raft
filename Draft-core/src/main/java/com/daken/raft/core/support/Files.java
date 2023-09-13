package com.daken.raft.core.support;

import java.io.File;
import java.io.IOException;

/**
 * Files
 */
public class Files {

    public static void touch(File file) throws IOException {
        if (!file.createNewFile() && !file.setLastModified(System.currentTimeMillis())) {
            throw new IOException("failed to touch file " + file);
        }
    }

}
