package com.daken.raft.core.node;

/**
 * Node(对外提供的接口)
 */
public interface Node {

    /**
     * 启动
     */
    void start();

    /**
     * 停止
     */
    void stop() throws InterruptedException;

}
