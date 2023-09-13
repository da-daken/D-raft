package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryFactory;
import com.daken.raft.core.support.RandomAccessFileAdapter;
import com.daken.raft.core.support.SeekableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 日志文件 基于 seekableFile 的实现
 * 1. 追加日志条目
 * 2. 加载指定位置便宜的日志条目
 * 3. 获取文件大小
 * 4. 裁剪文件大小，包括清空，用于日志的冲突
 */
public class EntriesFile {

    private final SeekableFile seekableFile;

    public EntriesFile(File file) throws FileNotFoundException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntriesFile(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
    }

    public long appendEntry(Entry entry) throws IOException {
        long offset = seekableFile.size();
        seekableFile.seek(offset);
        seekableFile.writeInt(entry.getKind());
        seekableFile.writeInt(entry.getIndex());
        seekableFile.writeInt(entry.getTerm());
        byte[] commandBytes = entry.getCommandBytes();
        seekableFile.writeInt(commandBytes.length);
        seekableFile.write(commandBytes);
        return offset;
    }

    public Entry loadEntry(long offset, EntryFactory factory) throws IOException {
        if (offset > seekableFile.size()) {
            throw new IllegalArgumentException("offset > size");
        }
        seekableFile.seek(offset);
        int kind = seekableFile.readInt();
        int index = seekableFile.readInt();
        int term = seekableFile.readInt();
        int length = seekableFile.readInt();
        byte[] commandBytes = new byte[length];
        seekableFile.read(commandBytes);
        return factory.create(kind, index, term, commandBytes);
    }

    public long size() throws IOException {
        return seekableFile.size();
    }

    public void clear() throws IOException {
        truncate(0L);
    }

    public void truncate(long offset) throws IOException {
        seekableFile.truncate(offset);
    }

    public void close() throws IOException {
        seekableFile.close();
    }

}
