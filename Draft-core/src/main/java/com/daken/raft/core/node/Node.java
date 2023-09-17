package com.daken.raft.core.node;

import com.daken.raft.core.log.statemachine.StateMachine;
import com.daken.raft.core.node.role.RoleNameAndLeaderId;

import javax.annotation.Nonnull;

/**
 * 对外提供的接口
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

    /**
     * 注册状态机，上层服务（k/v数据库）将自己注册到核心组件中
     */
    void registerStateMachine(@Nonnull StateMachine stateMachine);

    /**
     * 追加日志,真正参与命令执行的方法
     */
    void appendLog(@Nonnull byte[] commandBytes);

    /**
     * 获取当前节点信息和 leader 信息
     */
    RoleNameAndLeaderId getRoleNameAndLeaderId();


}
