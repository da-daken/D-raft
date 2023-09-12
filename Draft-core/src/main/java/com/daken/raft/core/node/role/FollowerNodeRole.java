package com.daken.raft.core.node.role;

import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.schedule.ElectionTimeout;
import lombok.Getter;
import lombok.ToString;

/**
 * follower role
 */
@Getter
@ToString
public class FollowerNodeRole extends AbstractNodeRole {

    /**
     * 投票给谁，可能为 null
     */
    private final NodeId votedFor;

    /**
     * 当前的 leader 节点，可能为 null
     */
    private final NodeId leaderId;

    /**
     * 选举超时定时器
     */
    private final ElectionTimeout electionTimeout;

    public FollowerNodeRole(int term, NodeId votedFor, NodeId leaderId, ElectionTimeout electionTimeout) {
        super(RoleName.FOLLOWER, term);
        this.votedFor = votedFor;
        this.leaderId = leaderId;
        this.electionTimeout = electionTimeout;
    }

    @Override
    public void cancelTimeOutOrTask() {
        this.electionTimeout.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return leaderId;
    }
}
