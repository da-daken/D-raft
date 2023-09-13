package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.LogDir;
import com.daken.raft.core.log.LogException;
import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryFactory;
import com.daken.raft.core.log.entry.EntryMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * FileEntrySequence
 */
public class FileEntrySequence extends AbstractEntrySequence {

    private final EntryFactory entryFactory = new EntryFactory();

    /**
     * 日志文件（包含全部日志条目内容的二进制文件，按照记录行的方式组织文件，具体见书中）
     * 没有文件头，快速访问需要访问索引文件
     */
    private final EntriesFile entriesFile;

    /**
     * 日志索引文件（只包含日志条目的元信息，即index， term， kind 和 在日志文件中的数据偏移，具体见书中）
     */
    private final EntryIndexFile entryIndexFile;

    /**
     * 未 commit 的日志，还在内存中
     */
    private final LinkedList<Entry> pendingEntries = new LinkedList<>();

    /**
     * 将被提交的日志记录的索引(初值为 0 且单调递增)
     * 不需要持久化
     */
    private int commitIndex;


    public FileEntrySequence(LogDir logDir, int logIndexOffset) {
        super(logIndexOffset);
        try {
            this.entriesFile = new EntriesFile(logDir.getEntriesFile());
            this.entryIndexFile = new EntryIndexFile(logDir.getEntryOffsetIndexFile());
            initialize();
        } catch (IOException e) {
            throw new LogException("failed to open entries file or entry index file", e);
        }
    }

    public FileEntrySequence(EntriesFile entriesFile, EntryIndexFile entryIndexFile, int logIndexOffset) {
        super(logIndexOffset);
        this.entriesFile = entriesFile;
        this.entryIndexFile = entryIndexFile;
        initialize();
    }

    private void initialize() {
        if (entryIndexFile.isEmpty()) {
            commitIndex = logIndexOffset - 1;
            return;
        }
        // 根据索引文件恢复日志的索引信息
        logIndexOffset = entryIndexFile.getMinEntryIndex();
        nextLogIndex = entryIndexFile.getMaxEntryIndex() + 1;
        // 这里也可以不对 commitIndex 赋值，因为 commitIndex 在经过和 Leader 的 rpc 之后会被更新为正确的值
        commitIndex = entryIndexFile.getMaxEntryIndex();
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        List<Entry> result = new ArrayList<>();

        // 先从 committed 日志文件中获取
        if (!entryIndexFile.isEmpty() && fromIndex <= entryIndexFile.getMaxEntryIndex()) {
            int maxIndex = Math.min(entryIndexFile.getMaxEntryIndex() + 1, toIndex);
            for (int i = fromIndex; i < maxIndex; i++) {
                result.add(getEntry(i));
            }
        }

        // 再从未 committed 的日志中获取
        if (!pendingEntries.isEmpty() && toIndex > pendingEntries.getFirst().getIndex()) {
            for (Entry entry : pendingEntries) {
                int index = entry.getIndex();
                if (index > toIndex) {
                    break;
                }
                if (index >= fromIndex) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @Override
    protected Entry doGetEntry(int index) {
        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex) {
                return pendingEntries.get(index - firstPendingEntryIndex);
            }
        }

        assert !entryIndexFile.isEmpty();
        return getEntryInFile(index);
    }


    @Override
    protected void doAppend(Entry entry) {
        pendingEntries.add(entry);
    }

    @Override
    protected void doRemoveAfter(int index) {
        // 只需要删除未提交的日志
        if (!pendingEntries.isEmpty() && index >= pendingEntries.getFirst().getIndex()) {
            for (int i = index + 1; i <= doGetLastLogIndex(); i++) {
                pendingEntries.removeLast();
            }
            nextLogIndex = index + 1;
            return;
        }

        try {
            if (index >= doGetFirstLogIndex()) {
                // 清除所有的未提交日志
                pendingEntries.clear();
                // 清除一部分已提交日志
                entriesFile.truncate(entryIndexFile.get(index + 1).getOffset());
                entryIndexFile.removeAfter(index);
                nextLogIndex = index + 1;
                commitIndex = index;
            } else {
                // 清除所有日志
                pendingEntries.clear();
                entriesFile.clear();
                entryIndexFile.clear();
                nextLogIndex = logIndexOffset;
                commitIndex = logIndexOffset - 1;
            }
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    @Override
    public EntryMeta getEntryMeta(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }

        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex) {
                return pendingEntries.get(index - firstPendingEntryIndex).getMeta();
            }
        }
        return entryIndexFile.get(index).toEntryMeta();
    }

    @Override
    public void commit(int index) {
        if (index < commitIndex) {
            throw new IllegalArgumentException("commit index < " + commitIndex);
        }
        if (index == commitIndex) {
            return;
        }

        // 根据 raft 算法，节点重启的时候 commitIndex 会被重置为 0， 所以可能存在 commitIndex 对应的日志已经被提交的情况
        if (!entryIndexFile.isEmpty() && index <= entryIndexFile.getMaxEntryIndex()) {
            commitIndex = entryIndexFile.getMaxEntryIndex();
            return;
        }

        if (pendingEntries.isEmpty() || pendingEntries.getLast().getIndex() < index) {
            throw new IllegalArgumentException("no entry to commit or commit index exceed");
        }

        // 提交缓存中的日志
        long offset;
        Entry entry = null;
        try {
            for (int i = commitIndex + 1; i <= index; i++) {
                entry = pendingEntries.removeFirst();
                offset = entriesFile.appendEntry(entry);
                entryIndexFile.appendEntryIndex(i, offset, entry.getKind(), entry.getTerm());
                commitIndex = i;
            }
        } catch (IOException e) {
            throw new LogException("failed to commit entry " + entry, e);
        }
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    public void close() {
        try {
            entriesFile.close();
            entryIndexFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close", e);
        }
    }

    private Entry getEntryInFile(int index) {
        long offset = entryIndexFile.getOffset(index);
        try {
            return entriesFile.loadEntry(offset, entryFactory);
        } catch (IOException e) {
            throw new LogException("failed to load entry " + index, e);
        }
    }
}
