package com.daken.raft.core.log.entry;

import com.daken.raft.core.node.NodeEndpoint;

import java.util.Set;

/**
 * GroupConfigEntry
 */
public abstract class GroupConfigEntry extends AbstractEntry {

    /**
     * 操作前配置
     */
    private final Set<NodeEndpoint> nodeEndpoints;

    protected GroupConfigEntry(int kind, int index, int term, Set<NodeEndpoint> nodeEndpoints) {
        super(kind, index, term);
        this.nodeEndpoints = nodeEndpoints;
    }

    public Set<NodeEndpoint> getNodeEndpoints() {
        return nodeEndpoints;
    }

    /**
     * 获取结果配置
     */
    public abstract Set<NodeEndpoint> getResultNodeEndpoints();

}
