package com.daken.raft.core.log.sequence;

import com.daken.raft.core.support.RandomAccessFileAdapter;
import com.daken.raft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * EntryIndexFile
 */
public class EntryIndexFile implements Iterable<EntryIndexItem> {

    /**
     * 最大条目索引偏移量
     */
    private static final long OFFSET_MAX_ENTRY_INDEX = Integer.BYTES;

    /**
     * 单条日志长度
     */
    private static final int LENGTH_ENTRY_INDEX_ITEM = 16;

    /**
     * 文件
     */
    private final SeekableFile seekableFile;

    /**
     * 日志数
     */
    private int entryIndexCount;

    /**
     * 最小日志索引
     */
    private int minEntryIndex;

    /**
     * 最大日志索引
     */
    private int maxEntryIndex;

    /**
     * 索引缓存
     */
    private final Map<Integer, EntryIndexItem> entryIndexMap = new HashMap<>();

    public EntryIndexFile(File file) throws IOException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntryIndexFile(SeekableFile seekableFile) throws IOException {
        this.seekableFile = seekableFile;
        load();
    }

    private void load() throws IOException {
        if (seekableFile.size() == 0) {
            entryIndexCount = 0;
            return;
        }
        minEntryIndex = seekableFile.readInt();
        maxEntryIndex = seekableFile.readInt();
        updateEntryIndexCount();
        for (int i = minEntryIndex; i <= maxEntryIndex; i++) {
            long offset = seekableFile.readLong();
            int kind = seekableFile.readInt();
            int term = seekableFile.readInt();
            entryIndexMap.put(i, new EntryIndexItem(i, offset, kind, term));
        }
    }

    public void appendEntryIndex(int index, long offset, int kind, int term) throws IOException {
        if (seekableFile.size() == 0) {
            seekableFile.writeInt(index);
            minEntryIndex = index;
        } else {
            // index 必须是 maxEntryIndex 的下一个，这里不需要传进来，应该由 EntryIndexFile 自己判断
            if (index != maxEntryIndex + 1) {
                throw new IllegalArgumentException("index must be " + (maxEntryIndex + 1) + ", but was " + index);
            }
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        }

        // 写入最大索引
        seekableFile.writeInt(index);
        maxEntryIndex = index;
        updateEntryIndexCount();

        // 在指定 index 写入索引日志
        seekableFile.seek(getOffsetOfEntryIndexItem(index));
        seekableFile.writeLong(offset);
        seekableFile.writeInt(kind);
        seekableFile.writeInt(term);
        entryIndexMap.put(index, new EntryIndexItem(index, offset, kind, term));
    }

    public void clear() throws IOException {
        seekableFile.truncate(0L);
        entryIndexCount = 0;
        entryIndexMap.clear();
    }

    public void removeAfter(int newMaxEntryIndex) throws IOException {
        if (isEmpty() || newMaxEntryIndex >= maxEntryIndex) {
            return;
        }
        if (newMaxEntryIndex < minEntryIndex) {
            clear();
            return;
        }
        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        seekableFile.writeInt(newMaxEntryIndex);
        seekableFile.truncate(getOffsetOfEntryIndexItem(newMaxEntryIndex + 1));
        for (int i = newMaxEntryIndex + 1; i <= maxEntryIndex; i++) {
            entryIndexMap.remove(i);
        }
        maxEntryIndex = newMaxEntryIndex;
        entryIndexCount = newMaxEntryIndex - minEntryIndex + 1;
    }

    public long getOffset(int entryIndex) {
        return get(entryIndex).getOffset();
    }

    @Nonnull
    public EntryIndexItem get(int entryIndex) {
        checkEmpty();
        if (entryIndex < minEntryIndex || entryIndex > maxEntryIndex) {
            throw new IllegalArgumentException("index < min or index > max");
        }
        return entryIndexMap.get(entryIndex);
    }

    public boolean isEmpty() {
        return entryIndexCount == 0;
    }

    public int getMinEntryIndex() {
        checkEmpty();
        return minEntryIndex;
    }


    public int getMaxEntryIndex() {
        checkEmpty();
        return maxEntryIndex;
    }

    public int getEntryIndexCount() {
        return entryIndexCount;
    }

    private void updateEntryIndexCount() {
        entryIndexCount = maxEntryIndex - minEntryIndex + 1;
    }

    private long getOffsetOfEntryIndexItem(int index) {
        return (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer.BYTES * 2;
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("no entry index");
        }
    }

    @Override
    @Nonnull
    public Iterator<EntryIndexItem> iterator() {
        if (isEmpty()) {
            return Collections.emptyIterator();
        }
        return new EntryIndexIterator(entryIndexCount, minEntryIndex);
    }

    public void close() throws IOException {
        seekableFile.close();
    }

    private class EntryIndexIterator implements Iterator<EntryIndexItem> {

        private final int entryIndexCount;
        private int currentEntryIndex;

        EntryIndexIterator(int entryIndexCount, int minEntryIndex) {
            this.entryIndexCount = entryIndexCount;
            this.currentEntryIndex = minEntryIndex;
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return currentEntryIndex <= maxEntryIndex;
        }

        private void checkModification() {
            if (this.entryIndexCount != EntryIndexFile.this.entryIndexCount) {
                throw new IllegalStateException("entry index count changed");
            }
        }

        @Override
        public EntryIndexItem next() {
            checkModification();
            return entryIndexMap.get(currentEntryIndex++);
        }
    }
}
