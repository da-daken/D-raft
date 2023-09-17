package com.daken.raft.kvstore.server;

import com.daken.raft.core.log.statemachine.AbstractSingleThreadStateMachine;
import com.daken.raft.core.node.Node;
import com.daken.raft.core.node.role.RoleName;
import com.daken.raft.core.node.role.RoleNameAndLeaderId;
import com.daken.raft.kvstore.message.*;
import com.daken.raft.kvstore.message.resp.GetCommandResponse;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 持有 node 的实例
 */
@Slf4j
public class Service {

    /**
     * node 实例
     */
    private final Node node;

    /**
     * 存放 请求ID 对应的连接
     * 因为网络IO线程和核心组件回调时的线程会同时当问这个映射，用线程安全的集合
     */
    private final ConcurrentHashMap<String, CommandRequest<?>> pendingCommands = new ConcurrentHashMap<>();

    private final Map<String, byte[]> map = new HashMap<>();

    public Service(Node node) {
        this.node = node;
        this.node.registerStateMachine(new StateMachineImpl());
    }

    public void set(CommandRequest<SetCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            commandRequest.reply(redirect);
            return;
        }
        SetCommand command = commandRequest.getCommand();
        log.debug("set {}", command.getKey());
        pendingCommands.put(command.getRequestId(), commandRequest);
        commandRequest.addCloseListener(() -> pendingCommands.remove(command.getRequestId()));

        // 追加日志
        this.node.appendLog(command.toBytes());
    }

    /**
     * 测试用的 get 读
     * 正确的情况是即使是读请求也需要经过 raft 日志复制，保证一致性（或者使用 ReadIndex 优化）
     */
    public void get(CommandRequest<GetCommand> commandRequest) {
        String key = commandRequest.getCommand().getKey();
        log.debug("get {}", key);
        byte[] bytes = map.get(key);
        commandRequest.reply(new GetCommandResponse(bytes));
    }

    /**
     * 判断是否为 leader
     */
    private Redirect checkLeadership() {
        RoleNameAndLeaderId roleNameAndLeaderId = node.getRoleNameAndLeaderId();
        if (!RoleName.LEADER.equals(roleNameAndLeaderId.getRoleName())) {
            return new Redirect(roleNameAndLeaderId.getLeaderId());
        }
        return null;
    }


    private class StateMachineImpl extends AbstractSingleThreadStateMachine {


        @Override
        protected void applyCommand(@Nonnull byte[] commandBytes) {
            // 1. 反序列化命令
            SetCommand command = SetCommand.fromBytes(commandBytes);
            // 2. 执行修改
            map.put(command.getKey(), command.getValue());
            // 3. 回复客户端
            CommandRequest<?> commandRequest = pendingCommands.remove(command.getRequestId());
            if (commandRequest != null) {
                commandRequest.reply(Success.INSTANCE);
            }
        }

    }
}
