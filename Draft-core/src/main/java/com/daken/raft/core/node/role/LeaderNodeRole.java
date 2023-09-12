package com.daken.raft.core.node.role;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.schedule.LogReplicationTask;
import lombok.Getter;
import lombok.ToString;

/**
 * Leader Role
 */
@Getter
@ToString
public class LeaderNodeRole extends AbstractNodeRole {

    /**
     * 日志复制和心跳定时器（在 raft 中心跳和日志复制用的是同一中请求 AppendEntries RPC）
     */
    private final LogReplicationTask logReplicationTask;

    public LeaderNodeRole(int term, LogReplicationTask logReplicationTask) {
        super(RoleName.LEADER, term);
        this.logReplicationTask = logReplicationTask;
    }

    @Override
    public void cancelTimeOutOrTask() {
        this.logReplicationTask.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return selfId;
    }
}
