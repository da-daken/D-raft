package com.daken.raft.core.node.store;

import com.daken.raft.core.node.NodeId;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

/**
 * MemoryNodeStore
 */
@NoArgsConstructor
@AllArgsConstructor
public class MemoryNodeStore implements NodeStore {

    private int term;
    private NodeId votedFor;


    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public void setTerm(int term) {
        this.term = term;
    }

    @Nullable
    @Override
    public NodeId getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(@Nullable NodeId votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public void close() {

    }
}
