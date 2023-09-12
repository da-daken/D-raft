package com.daken.raft.core.node.store;

import com.daken.raft.core.node.NodeId;

import javax.annotation.Nullable;

/**
 * NodeStore 存储当前节点的任期和投票给了谁
 * 根据 raft 论文 currentTerm 和 votedFor 是所有节点需要持久化的状态
 */
public interface NodeStore {

    /**
     * Get term.
     *
     * @return term
     */
    int getTerm();

    /**
     * Set term.
     *
     * @param term term
     */
    void setTerm(int term);

    /**
     * Get voted for.
     *
     * @return voted for
     */
    @Nullable
    NodeId getVotedFor();

    /**
     * Set voted for
     *
     * @param votedFor voted for
     */
    void setVotedFor(@Nullable NodeId votedFor);

    /**
     * Close store.
     */
    void close();

}
