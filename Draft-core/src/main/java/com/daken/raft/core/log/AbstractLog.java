package com.daken.raft.core.log;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.entry.GeneralEntry;
import com.daken.raft.core.log.entry.NoOpEntry;
import com.daken.raft.core.log.event.SnapshotGenerateEvent;
import com.daken.raft.core.log.sequence.EntrySequence;
import com.daken.raft.core.log.sequence.GroupConfigEntryList;
import com.daken.raft.core.log.snapshot.EntryInSnapshotException;
import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.log.snapshot.SnapshotChunk;
import com.daken.raft.core.log.snapshot.entity.builder.FileSnapshotWriter;
import com.daken.raft.core.log.snapshot.entity.builder.NullSnapshotBuilder;
import com.daken.raft.core.log.snapshot.entity.builder.SnapshotBuilder;
import com.daken.raft.core.log.statemachine.StateMachine;
import com.daken.raft.core.log.statemachine.StateMachineContext;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * AbstractLog
 */
@Slf4j
public abstract class AbstractLog implements Log {

    protected EntrySequence entrySequence;

    protected int commitIndex;

    protected StateMachine stateMachine;

    private final StateMachineContext stateMachineContext = new StateMachineContextImpl();

    protected Snapshot snapshot;

    protected SnapshotBuilder snapshotBuilder = new NullSnapshotBuilder();

    protected final EventBus eventBus;

    protected GroupConfigEntryList groupConfigEntryList;

    AbstractLog(EventBus eventBus) {
        this.eventBus = eventBus;
    }



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
            return new EntryMeta(Entry.KIND_NO_OP, 0, 0);
        }
        return entrySequence.getLastEntry().getMeta();
    }

    @Override
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) {
        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }

        // 若要传的日志比快照的最后一个日志index小
        // 抛出异常，到外层发送快照rpc
        if (nextIndex <= snapshot.getLastIncludedIndex()) {
            throw new EntryInSnapshotException(nextIndex);
        }

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        // TODO messageId 返回方可能乱序回复，生成UUID让回复方丢弃编号乱序的结果，然后依靠raft的重传保证后续处理的正确性
        rpc.setMessageId(UUID.randomUUID().toString());
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLeaderCommit(commitIndex);

        // 设置前一条日志信息
        if (nextIndex == snapshot.getLastIncludedIndex() + 1) {
            rpc.setPrevLogIndex(snapshot.getLastIncludedIndex());
            rpc.setPrevLogTerm(snapshot.getLastIncludedTerm());
        } else {
            Entry preEntry = entrySequence.getEntry(nextIndex - 1);
            if (preEntry != null) {
                rpc.setPrevLogIndex(preEntry.getIndex());
                rpc.setPrevLogTerm(preEntry.getTerm());
            }
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
        // 推进 applyIndex
        advanceApplyIndex();
    }

    private void advanceApplyIndex() {
        int lastApplied = stateMachine.getLastApplied();
        int lastIncludedIndex = snapshot.getLastIncludedIndex();
        if (lastApplied == 0 && lastIncludedIndex > 0) {
            // 推进 lastApplied 时，有日志快照时必须先应用日志快照
            assert commitIndex >= lastIncludedIndex;
            applySnapshot(snapshot);
            lastApplied = lastIncludedIndex;
        }
        for (Entry entry : entrySequence.subList(lastApplied + 1, commitIndex + 1)) {
            applyEntry(entry);
        }
    }

    @Override
    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public InstallSnapshotState installSnapshot(InstallSnapshotRpc rpc) {
        if (rpc.getLastIndex() <= snapshot.getLastIncludedIndex()) {
            log.debug("snapshot's last included index from rpc <= current one ({} <= {}), ignore",
                    rpc.getLastIndex(), snapshot.getLastIncludedIndex());
            return new InstallSnapshotState(InstallSnapshotState.StateName.ILLEGAL_INSTALL_SNAPSHOT_RPC);
        }

        // 偏移量为 0，重新构建快照
        if (rpc.getOffset() == 0) {
            assert rpc.getLastConfig() != null;
            snapshotBuilder.close();
            snapshotBuilder = newSnapshotBuilder(rpc);
        } else {
            // 追加快照
            snapshotBuilder.append(rpc);
        }

        // 快照数据未完成传输
        if (!rpc.isDone()) {
            return new InstallSnapshotState(InstallSnapshotState.StateName.INSTALLING);
        }

        // 快照全都传输完成
        Snapshot newSnapshot = snapshotBuilder.build();
        // 更新日志
        replaceSnapshot(newSnapshot);
        // 应用日志
        applySnapshot(newSnapshot);
        int lastIncludedIndex = snapshot.getLastIncludedIndex();
        if (commitIndex < lastIncludedIndex) {
            commitIndex = lastIncludedIndex;
        }
        return new InstallSnapshotState(InstallSnapshotState.StateName.INSTALLED, newSnapshot.getLastConfig());
    }

    @Override
    public void generateSnapshot(int lastIncludedIndex, Set<NodeEndpoint> groupConfig) {
        log.info("generate snapshot, last included index {}", lastIncludedIndex);
        EntryMeta lastAppliedEntryMeta = entrySequence.getEntryMeta(lastIncludedIndex);
        replaceSnapshot(generateSnapshot(lastAppliedEntryMeta, groupConfig));
    }

    @Override
    public InstallSnapshotRpc createInstallSnapshotRpc(int term, NodeId selfId, int offset, int length) {
        InstallSnapshotRpc rpc = new InstallSnapshotRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLastIndex(snapshot.getLastIncludedIndex());
        rpc.setLastTerm(snapshot.getLastIncludedTerm());
        if (offset == 0) {
            rpc.setLastConfig(snapshot.getLastConfig());
        }
        rpc.setOffset(offset);

        SnapshotChunk chunk = snapshot.readData(offset, length);
        rpc.setData(chunk.getBytes());
        rpc.setDone(chunk.isLastChunk());

        rpc.setMessageId(UUID.randomUUID().toString());
        return rpc;
    }


    @Override
    public void close() {
        entrySequence.close();
        stateMachine.shutdown();
    }

    private void applyEntry(Entry entry) {
        // skip no-op entry and membership-change entry
        if (isApplicable(entry)) {
            Set<NodeEndpoint> lastGroup = groupConfigEntryList.getLastGroup();
            stateMachine.applyLog(stateMachineContext, entry.getIndex(), entry.getCommandBytes(), entrySequence.getFirstLogIndex(), lastGroup);
        }
    }

    private boolean isApplicable(Entry entry) {
        return entry.getKind() == Entry.KIND_GENERAL;
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

    private void applySnapshot(Snapshot snapshot) {
        log.debug("apply snapshot, last included index {}", snapshot.getLastIncludedIndex());
        try {
            stateMachine.applySnapshot(snapshot);
        } catch (IOException e) {
            throw new LogException("failed to apply snapshot", e);
        }
    }


    protected abstract void replaceSnapshot(Snapshot newSnapshot);

    protected abstract SnapshotBuilder newSnapshotBuilder(InstallSnapshotRpc firstRpc);

    protected abstract Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig);

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

    private class StateMachineContextImpl implements StateMachineContext {

        @Override
        public void generateSnapshot(int lastIncludedIndex) {
            eventBus.post(new SnapshotGenerateEvent(lastIncludedIndex));
        }

    }
}
