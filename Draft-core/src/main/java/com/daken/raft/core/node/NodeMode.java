package com.daken.raft.core.node;

/**
 * NodeMode 集群状态
 */
public enum NodeMode {

    STANDALONE,
    STANDBY,
    GROUP_MEMBER;
}
