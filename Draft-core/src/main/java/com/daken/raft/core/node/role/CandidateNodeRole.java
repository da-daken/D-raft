package com.daken.raft.core.node.role;


import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.schedule.ElectionTimeout;
import lombok.Getter;
import lombok.ToString;

/**
 * Candidate Role
 */
@Getter
@ToString
public class CandidateNodeRole extends AbstractNodeRole {

    /**
     * 获得的票数
     */
    private final int votesCount;

    /**
     * 选举超时定时器
     */
    private final ElectionTimeout electionTimeout;

    public CandidateNodeRole(int term, ElectionTimeout electionTimeout) {
        this(term, 1, electionTimeout);
    }

    public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) {
        super(RoleName.CANDIDATE, term);
        this.votesCount = votesCount;
        this.electionTimeout = electionTimeout;
    }

    @Override
    public void cancelTimeOutOrTask() {
        this.electionTimeout.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return null;
    }
}
