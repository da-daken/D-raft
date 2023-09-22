package com.daken.raft.kvstore.server;

import com.daken.raft.core.log.statemachine.AbstractSingleThreadStateMachine;
import com.daken.raft.core.node.Node;
import com.daken.raft.core.node.role.RoleName;
import com.daken.raft.core.node.role.RoleNameAndLeaderId;
import com.daken.raft.kvstore.message.*;
import com.daken.raft.kvstore.message.resp.GetCommandResponse;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import com.daken.raft.kvstore.Protos;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private Map<String, byte[]> map = new HashMap<>();

    public Service(Node node) {
        this.node = node;
        this.node.registerStateMachine(new StateMachineImpl());
    }

    public void set(CommandRequest<SetCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            // 回复客户端需要重定向，并发送leaderId
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

    /**
     * 生成数据快照
     * @param map
     * @param outputStream
     * @throws IOException
     */
    public static void toSnapshot(Map<String, byte[]> map, OutputStream outputStream) throws IOException {
        Protos.EntryList.Builder builder = Protos.EntryList.newBuilder();
        map.forEach((k, v) ->
                builder.addEntries(Protos.EntryList.Entry.newBuilder()
                        .setKey(k)
                        .setValue(ByteString.copyFrom(v)).build()));
        builder.build().writeTo(outputStream);
    }

    /**
     * 从数据快照中恢复
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static Map<String, byte[]> fromSnapshot(InputStream inputStream) throws IOException {
        Map<String, byte[]> map = new HashMap<>();
        Protos.EntryList entryList = Protos.EntryList.parseFrom(inputStream);
        for (Protos.EntryList.Entry entry : entryList.getEntriesList()) {
            map.put(entry.getKey(), entry.getValue().toByteArray());
        }
        return map;
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

        @Override
        protected void doApplySnapshot(@Nonnull InputStream input) throws IOException {
            map = fromSnapshot(input);
        }

        @Override
        public boolean shouldGenerateSnapshot(int firstLogIndex, int lastApplied) {
            return lastApplied - firstLogIndex > 100;
        }

        @Override
        public void generateSnapshot(@Nonnull OutputStream output) throws IOException {
            toSnapshot(map, output);
        }

    }
}
