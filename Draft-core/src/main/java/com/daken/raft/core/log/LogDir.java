package com.daken.raft.core.log;

import java.io.File;

/**
 * LogDir 为了避免直接在Sequence中硬编码文件名，设计接口获取
 */
public interface LogDir {

    void initialize();

    boolean exists();

    File getSnapshotFile();

    File getEntriesFile();

    File getEntryOffsetIndexFile();

    File get();

    boolean renameTo(LogDir logDir);
}
