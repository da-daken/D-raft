package com.daken.raft.core.rpc.message.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 投票结果
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class RequestVoteResult implements Serializable {

    /**
     * 当前任期，候选者用来更新自己
     */
    private int term;

    /**
     * 如果候选者当选则为 true
     */
    private boolean voteGranted;

}
