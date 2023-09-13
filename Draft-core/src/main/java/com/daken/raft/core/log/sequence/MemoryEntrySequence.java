package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.entry.Entry;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryEntrySequence (测试，先写基于内存的日志序列)
 */
@Slf4j
@ToString
public class MemoryEntrySequence extends AbstractEntrySequence {

    private final List<Entry> entries = new ArrayList<>();

    private int commitIndex;


    public MemoryEntrySequence() {
        // nextLogIndex 从 1 开始，而不是 0 开始
        this(1);
    }

    public MemoryEntrySequence(int logIndexOffset) {
        super(logIndexOffset);
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        return entries.subList(fromIndex - logIndexOffset, toIndex - logIndexOffset);
    }

    @Override
    protected Entry doGetEntry(int index) {
        return entries.get(index - logIndexOffset);
    }

    @Override
    protected void doAppend(Entry entry) {
        entries.add(entry);
    }

    @Override
    protected void doRemoveAfter(int index) {
        if (index < doGetFirstLogIndex()) {
            entries.clear();
            nextLogIndex = logIndexOffset;
        } else {
            entries.subList(index - logIndexOffset + 1, entries.size()).clear();
            nextLogIndex = index + 1;
        }
    }

    @Override
    public void commit(int index) {
        this.commitIndex = index;
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    /**
     * 由于是基于内存实现，close方法什么都不用做
     */
    @Override
    public void close() {

    }
}
