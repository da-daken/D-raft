package com.daken.raft.core.log;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.entry.GeneralEntry;
import com.daken.raft.core.log.entry.NoOpEntry;
import com.daken.raft.core.log.statemachine.StateMachine;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;

import java.util.List;
import java.util.Set;

/**
 * 对外的日志接口
 */
public interface Log {

    int ALL_ENTRIES = -1;

    /**
     * 获取最新的日志的元信息(发送消息时，选举开始时)
     */
    EntryMeta getLastEntryMeta();

    /**
     * 创建 AppendEntriesRpc 消息
     */
    AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries);

    /**
     * 获取下一条日志索引
     */
    int getNextIndex();

    /**
     * 获取当前 commitIndex
     */
    int getCommitIndex();

    /**
     * 日志比较
     */
    boolean isNewerThan(int lastLogIndex, int lastLogTerm);

    /**
     * 增加一个空的日志条目
     */
    NoOpEntry appendEntry(int term);

    /**
     * 增加一个正常的日志条目
     */
    GeneralEntry appendEntry(int term, byte[] command);

    /**
     * 追加来自 leader 的日志条目
     */
    boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> entries);

    /**
     * 推进 commitIndex
     */
    void advanceCommitIndex(int newCommitIndex, int currentTerm);

    /**
     * 设置状态机
     */
     void setStateMachine(StateMachine stateMachine);

    /**
     * Install snapshot.
     *
     * @param rpc rpc
     * @return install snapshot state
     */
    InstallSnapshotState installSnapshot(InstallSnapshotRpc rpc);

    /**
     * 更新日志快照
     *
     * @param lastIncludedIndex last included index
     * @param groupConfig       group config
     */
    void generateSnapshot(int lastIncludedIndex, Set<NodeEndpoint> groupConfig);

    /**
     * Create install snapshot rpc from log.
     *
     * @param term   current term
     * @param selfId self node id
     * @param offset data offset
     * @param length data length
     * @return install snapshot rpc
     */
    InstallSnapshotRpc createInstallSnapshotRpc(int term, NodeId selfId, int offset, int length);


    /**
     * 关闭
     */
    void close();

}
