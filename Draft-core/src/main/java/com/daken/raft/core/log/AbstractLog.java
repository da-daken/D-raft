package com.daken.raft.core.log;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.entry.GeneralEntry;
import com.daken.raft.core.log.entry.NoOpEntry;
import com.daken.raft.core.log.sequence.EntrySequence;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * AbstractLog
 */
@Slf4j
public abstract class AbstractLog implements Log {

    protected EntrySequence entrySequence;

    protected int commitIndex;

    @Override
    public int getNextIndex() {
        return entrySequence.getNextLogIndex();
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    public EntryMeta getLastEntryMeta() {
        if (entrySequence.isEmpty()) {
            new EntryMeta(Entry.KIND_NO_OP, 0, 0);
        }
        return entrySequence.getLastEntry().getMeta();
    }

    @Override
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) {
        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLeaderCommit(commitIndex);

        // 设置前一条日志信息
        Entry preEntry = entrySequence.getEntry(nextIndex - 1);
        if (preEntry != null) {
            rpc.setPrevLogIndex(preEntry.getIndex());
            rpc.setPrevLogTerm(preEntry.getTerm());
        }
        // maxEntries 标识最大读取的日志条数，假如传输全部日志条目数量，可能会网络拥堵，默认-1
        // todo 这里没懂
        if (!entrySequence.isEmpty()) {
            int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex, nextIndex + maxEntries));
            rpc.setEntries(entrySequence.subList(nextIndex, maxIndex));
        }
        return rpc;
    }

    /**
     * 投票检查
     * 投票者比 candidate 的还新，就拒绝该投票
     * @param lastLogIndex
     * @param lastLogTerm
     * @return
     */
    @Override
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {
        EntryMeta lastEntryMeta = getLastEntryMeta();
        log.debug("last entry ({}, {}), candidate ({}, {})",
                lastEntryMeta.getIndex(), lastEntryMeta.getTerm(), lastLogIndex, lastLogTerm);
        // 先比较 term 再判断日志索引
        return lastEntryMeta.getTerm() <= lastLogTerm && lastEntryMeta.getIndex() <= lastLogIndex;
    }

    /**
     * 添加no-op日志
     * @param term
     * @return
     */
    @Override
    public NoOpEntry appendEntry(int term) {
        NoOpEntry noOpEntry = new NoOpEntry(entrySequence.getNextLogIndex(), term);
        entrySequence.append(noOpEntry);
        return noOpEntry;
    }

    /**
     * 添加正常日志
     * @param term
     * @param command
     * @return
     */
    @Override
    public GeneralEntry appendEntry(int term, byte[] command) {
        GeneralEntry generalEntry = new GeneralEntry(entrySequence.getNextLogIndex(), term, command);
        entrySequence.append(generalEntry);
        return generalEntry;
    }

    @Override
    public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) {

        // 检查前一条日志是否匹配
        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) {
            return false;
        }

        // leader 节点传过来的日志条目是否为空
        if (leaderEntries.isEmpty()) {
            return true;
        }

        // 解决冲突的日志，并返回需要追加的日志
        // 因为 prevLogIndex 不一定是最后的一条日志，所以要把 preLogIndex 之后的日志都删除
        EntrySequenceView newEntries = removeUnmatchedLog(new EntrySequenceView(leaderEntries));
        // 添加新的日志
        appendEntriesFromLeader(newEntries);
        return true;
    }

    @Override
    public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
        if (!validateNewCommitIndex(newCommitIndex, currentTerm)) {
            return;
        }
        log.debug("advance commit index from {} to {}", commitIndex, newCommitIndex);
        entrySequence.commit(newCommitIndex);

        // TODO
        //advanceApplyIndex();
    }

    @Override
    public void close() {
        entrySequence.close();
    }

    // 判断传过来的是否是当前任期，是当前任期则提交
    private boolean validateNewCommitIndex(int newCommitIndex, int currentTerm) {
        if (newCommitIndex < entrySequence.getCommitIndex()) {
            return false;
        }
        Entry entry = entrySequence.getEntry(newCommitIndex);
        if (entry == null) {
            log.debug("log of new commit index {} not found", newCommitIndex);
            return false;
        }
        /**
         * 推进 commitIndex 需要当前的term和日志的term一致（安全性机制，防止提交了的日志被冲突覆盖）
         */
        return entry.getTerm() == currentTerm;
    }

    private void appendEntriesFromLeader(EntrySequenceView newEntries) {
        if (newEntries.isEmpty()) {
            return;
        }
        log.debug("append entries from leader from {} to {}", newEntries.getFirstLogIndex(), newEntries.getLastLogIndex());
        for (Entry newEntry : newEntries) {
            entrySequence.append(newEntry);
        }
    }

    private EntrySequenceView removeUnmatchedLog(EntrySequenceView leaderEntries) {
        assert !leaderEntries.isEmpty();
        int firstUnmatched = findFirstUnmatchedLog(leaderEntries);

        if (firstUnmatched < 0) {
            return new EntrySequenceView(Collections.emptyList());
        }
        // 移除不匹配的所有的日志
        removeEntriesAfter(firstUnmatched - 1);
        // 返回需要追加的日志
        return leaderEntries.subView(firstUnmatched);
    }

    private void removeEntriesAfter(int index) {
        if (entrySequence.isEmpty() || index > entrySequence.getLastLogIndex()) {
            return;
        }
        // TODO commit 的日志也有可能被移除，这时候需要重新构建状态机
        log.debug("remove entries after {} ", index);
        entrySequence.removeAfter(index);
    }

    private int findFirstUnmatchedLog(EntrySequenceView leaderEntries) {
        for (Entry leaderEntry : leaderEntries) {
            int logIndex = leaderEntry.getIndex();
            // 根据日志索引获取本地的日志元信息
            EntryMeta followerEntryMeta = entrySequence.getEntryMeta(logIndex);
            // 如果日志不存在，或者任期不匹配，返回当前不匹配的日志索引
            if (followerEntryMeta == null || followerEntryMeta.getTerm() != leaderEntry.getTerm()) {
                return logIndex;
            }
        }
        return -1;
    }

    private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) {
        EntryMeta entryMeta = entrySequence.getEntryMeta(prevLogIndex);
        if (entryMeta == null) {
            return false;
        }
        int term = entryMeta.getTerm();
        return term == prevLogTerm;
    }

    @Getter
    private static class EntrySequenceView implements Iterable<Entry> {

        private final List<Entry> entries;
        private int firstLogIndex = -1;
        private int lastLogIndex = -1;

        EntrySequenceView(List<Entry> entries) {
            this.entries = entries;
            if (!entries.isEmpty()) {
                firstLogIndex = entries.get(0).getIndex();
                lastLogIndex = entries.get(entries.size() - 1).getIndex();
            }
        }

        Entry get(int index) {
            if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                return null;
            }
            return entries.get(index - firstLogIndex);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        EntrySequenceView subView(int fromIndex) {
            if (entries.isEmpty() || fromIndex > lastLogIndex) {
                return new EntrySequenceView(Collections.emptyList());
            }
            return new EntrySequenceView(entries.subList(fromIndex - firstLogIndex, entries.size()));
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }
    }
}
