package com.daken.raft.core.rpc.message.req;

import com.daken.raft.core.node.NodeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 投票请求
 */
@Getter
@Setter
@ToString
public class RequestVoteRpc implements Serializable {

    /**
     * 候选者当前任期
     */
    private int term;

    /**
     * 候选人 id（发送者自己）
     */
    private NodeId candidateId;

    /**
     * 候选人最新日志索引
     */
    private int lastLogIndex;

    /**
     * 候选人最新日志的任期
     */
    private int lastLogTerm;

}
