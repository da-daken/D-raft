package com.daken.raft.core.node.store;

import com.daken.raft.core.node.NodeId;

import javax.annotation.Nullable;

/**
 * FileNodeStore TODO
 */
public class FileNodeStore implements NodeStore {


    @Override
    public int getTerm() {
        return 0;
    }

    @Override
    public void setTerm(int term) {

    }

    @Nullable
    @Override
    public NodeId getVotedFor() {
        return null;
    }

    @Override
    public void setVotedFor(@Nullable NodeId votedFor) {

    }

    @Override
    public void close() {

    }
}
