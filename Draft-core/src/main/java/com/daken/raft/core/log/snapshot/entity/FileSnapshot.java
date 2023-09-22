package com.daken.raft.core.log.snapshot.entity;

import com.daken.raft.core.Protos;
import com.daken.raft.core.log.LogDir;
import com.daken.raft.core.log.LogException;
import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.log.snapshot.SnapshotChunk;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.support.RandomAccessFileAdapter;
import com.daken.raft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于文件的日志快照
 */
public class FileSnapshot implements Snapshot {

    private LogDir logDir;
    private SeekableFile seekableFile;
    private int lastIncludeIndex;
    private int lastIncludeTerm;
    private Set<NodeEndpoint> lastConfig;
    private long dataStart;
    private long dataLength;

    public FileSnapshot(LogDir logDir) {
        this.logDir = logDir;
        readHeader(logDir.getSnapshotFile());
    }

    public FileSnapshot(File file) {
        readHeader(file);
    }

    public FileSnapshot(SeekableFile file) {
        readHeader(file);
    }

    /**
     * r 代表文件只读
     */
    private void readHeader(File snapshotFile) {
        try {
            readHeader(new RandomAccessFileAdapter(snapshotFile, "r"));
        } catch (FileNotFoundException e) {
            throw new LogException(e);
        }
    }

    private void readHeader(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
        try {
            // 文件头大小(int) 4字节
            int headerLength = seekableFile.readInt();
            byte[] headerBytes = new byte[headerLength];
            seekableFile.read(headerBytes);
            // 文件头包括（last_included_index, last+included_term, lastConfig）lastConfig是集群成员列表涉及的内容
            Protos.SnapshotHeader header = Protos.SnapshotHeader.parseFrom(headerBytes);
            this.lastIncludeIndex = header.getLastIndex();
            this.lastIncludeTerm = header.getLastTerm();
            lastConfig = header.getLastConfigList().stream()
                    .map(e -> new NodeEndpoint(e.getId(), e.getHost(), e.getPort()))
                    .collect(Collectors.toSet());
            dataStart = seekableFile.position();
            dataLength = seekableFile.size() - dataStart;

        } catch (IOException e) {
            throw new LogException("failed to read snapshot", e);
        }
    }


    @Override
    public int getLastIncludedIndex() {
        return lastIncludeIndex;
    }

    @Override
    public int getLastIncludedTerm() {
        return lastIncludeTerm;
    }

    @Nonnull
    @Override
    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }

    @Override
    public long getDataSize() {
        return dataLength;
    }

    @Nonnull
    @Override
    public SnapshotChunk readData(int offset, int length) {
        if (offset > dataLength) {
            throw new IllegalArgumentException("offset > data length, illegal param");
        }
        try {
            seekableFile.seek(dataStart + offset);
            // 若剩余内容比要取得的长度小，取剩余内容即可
            // 若剩余内容比要取得的长度大，取要取得的长度
            byte[] bytes = new byte[Math.min(length, (int) dataLength - offset)];
            int read = seekableFile.read(bytes);
            return new SnapshotChunk(bytes, offset + read >= dataLength);
        } catch (IOException e) {
            throw new LogException("failed to seek or read snapshot content", e);
        }
    }

    @Nonnull
    @Override
    public InputStream getDataStream() {
        try {
            return seekableFile.inputStream(dataStart);
        } catch (IOException e) {
            throw new LogException("failed to get input stream of snapshot data", e);
        }
    }

    @Override
    public void close() {
        try {
            seekableFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close file", e);
        }
    }

    public LogDir getLogDir() {
        return logDir;
    }
}
