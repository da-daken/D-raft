package com.daken.raft.kvstore.message;


import com.daken.raft.core.node.NodeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Redirect
 */
@Getter
@Setter
@ToString
public class Redirect {

    private final String leaderId;

    public Redirect(NodeId leaderId) {
        this.leaderId = leaderId != null ? leaderId.getValue() : null;
    }

    public Redirect(String leaderId) {
        this.leaderId = leaderId;
    }
}
