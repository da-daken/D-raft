package com.daken.raft.core.node;

import lombok.Getter;

import java.util.Objects;

/**
 * GroupMember 记录 node 地址
 */
@Getter
public class GroupMember {

    private final NodeEndpoint endpoint;

    public GroupMember(NodeEndpoint endpoint) {
        Objects.requireNonNull(endpoint);
        this.endpoint = endpoint;
    }
}
