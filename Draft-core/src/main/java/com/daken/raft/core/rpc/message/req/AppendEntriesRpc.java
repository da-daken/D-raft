package com.daken.raft.core.rpc.message.req;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.node.NodeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 日志复制请求；心跳请求
 */
@Getter
@Setter
@ToString
public class AppendEntriesRpc implements Serializable {

    /**
     * leader 任期
     */
    private int term;

    /**
     * 用来 follower 重定向到 leader
     * so follower can redirect clients
     */
    private NodeId leaderId;

    /**
     * 前一条日志的索引
     */
    private int prevLogIndex;

    /**
     * 前一条日志的任期
     */
    private int prevLogTerm;

    /**
     * log entries to store (empty for heartbeat; may send more than one for efficiency)
     */
    private List<Entry> entries = Collections.emptyList();

    /**
     * leader’s commitIndex
     */
    private int leaderCommit;

    private String messageId;

    public int getLastEntryIndex() {
        return this.entries.isEmpty() ? this.prevLogIndex : this.entries.get(this.entries.size() - 1).getIndex();
    }

}
