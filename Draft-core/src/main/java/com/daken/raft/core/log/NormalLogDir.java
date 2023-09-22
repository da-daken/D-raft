package com.daken.raft.core.log;

import java.io.File;

/**
 * NormalLogDir
 */
public class NormalLogDir extends AbstractLogDir {

    NormalLogDir(File dir) {
        super(dir);
    }

    @Override
    public String toString() {
        return "NormalLogDir{" +
                "dir=" + dir +
                '}';
    }

}

