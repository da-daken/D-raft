package com.daken.raft.core.service;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.nio.ChannelException;

/**
 * RedirectException
 */
public class RedirectException extends ChannelException {

    private final NodeId leaderId;

    public RedirectException(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

}
