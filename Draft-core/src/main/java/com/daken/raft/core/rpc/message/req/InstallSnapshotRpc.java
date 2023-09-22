package com.daken.raft.core.rpc.message.req;

import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.node.NodeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

/**
 * InstallSnapshotRpc
 */
@Getter
@Setter
@ToString
public class InstallSnapshotRpc {

    private int term;
    private NodeId leaderId;
    private int lastIndex;
    private int lastTerm;
    private Set<NodeEndpoint> lastConfig;
    private int offset;
    private byte[] data;
    private boolean done;

    private String messageId;

}
