package com.daken.raft.core.node;

import com.daken.raft.core.rpc.Address;
import lombok.Data;

/**
 * NodeEndpoint
 */
@Data
public class NodeEndpoint {

    private final NodeId id;
    private final Address address;

    public NodeEndpoint(String id, String host, int port) {
        this.id = NodeId.of(id);
        this.address = new Address(host, port);
    }

    public int getPort() {
        return address.getPort();
    }
}
