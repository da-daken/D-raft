package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.sequence.excpetion.EmptySequenceException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * AbstractEntrySequence
 */
@Slf4j
public abstract class AbstractEntrySequence implements EntrySequence {

    /**
     * 日志索引偏移(标识为写入快照的开始部分)
     */
    int logIndexOffset;

    /**
     * 下一条日志索引
     */
    int nextLogIndex;

    public AbstractEntrySequence(int logIndexOffset) {
        this.logIndexOffset = logIndexOffset;
        this.nextLogIndex = logIndexOffset;
    }

    @Override
    public boolean isEmpty() {
        return logIndexOffset == nextLogIndex;
    }

    @Override
    public int getFirstLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetFirstLogIndex();
    }


    @Override
    public int getLastLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetLastLogIndex();
    }

    @Override
    public int getNextLogIndex() {
        return nextLogIndex;
    }

    @Override
    public boolean isEntryPresent(int index) {
        return !isEmpty() && index >= doGetFirstLogIndex() && index <= doGetLastLogIndex();
    }

    @Override
    public Entry getEntry(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }
        return doGetEntry(index);
    }

    @Override
    public Entry getLastEntry() {
        return isEmpty() ? null : doGetEntry(doGetLastLogIndex());
    }

    @Override
    public List<Entry> subList(int fromIndex) {
        if (isEmpty() || fromIndex > doGetLastLogIndex()) {
            return Collections.emptyList();
        }
        return subList(Math.max(fromIndex, doGetFirstLogIndex()), doGetLastLogIndex());
    }

    @Override
    public List<Entry> subList(int fromIndex, int toIndex) {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        if (fromIndex < doGetFirstLogIndex()
                || toIndex > doGetLastLogIndex() + 1
                || fromIndex > toIndex) {
            throw new IllegalArgumentException("illegal from index " + fromIndex + " or to index " + toIndex);
        }
        return doSubList(fromIndex, toIndex);
    }

    @Override
    public void append(List<Entry> entries) {
        for (Entry entry : entries) {
            append(entry);
        }
    }

    @Override
    public void append(Entry entry) {
        if (entry.getIndex() != nextLogIndex) {
            throw new IllegalArgumentException("entry index must be " + nextLogIndex);
        }
        doAppend(entry);
        nextLogIndex++;
    }

    @Override
    public void removeAfter(int index) {
        if (isEmpty() || index >= doGetLastLogIndex()) {
            return;
        }
        doRemoveAfter(index);
    }

    @Override
    public EntryMeta getEntryMeta(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }
        return entry.getMeta();
    }

    /**
     * 获取一个子试图；前开后闭区间
     *
     * @param fromIndex 开始索引
     * @param toIndex   结束索引
     * @return List<Entry>
     */
    protected abstract List<Entry> doSubList(int fromIndex, int toIndex);

    /**
     * 获取指定索引的日志条目
     *
     * @param index 日志索引
     * @return Entry
     */
    protected abstract Entry doGetEntry(int index);

    /**
     * 追加日志
     *
     * @param entry 日志条目
     */
    protected abstract void doAppend(Entry entry);

    /**
     * 移除指定索引后的日志条目
     *
     * @param index 日志索引
     */
    protected abstract void doRemoveAfter(int index);

    int doGetFirstLogIndex() {
        return logIndexOffset;
    }

    int doGetLastLogIndex() {
        return nextLogIndex - 1;
    }
}
