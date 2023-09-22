package com.daken.raft.core.log.statemachine;

import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.support.SingleThreadTaskExecutor;
import com.daken.raft.core.support.TaskExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * 单线程异步抽象类
 */
@Slf4j
public abstract class AbstractSingleThreadStateMachine implements StateMachine {

    /**
     * 异步单线程中，处理线程和核心组件的线程都会访问该属性，但是只有处理线程会修改，这种一写多读的场景用 volatile 修饰
     */
    private volatile int lastApplied;
    private final TaskExecutor taskExecutor;

    public AbstractSingleThreadStateMachine() {
        this.taskExecutor = new SingleThreadTaskExecutor("state-machine");
    }

    @Override
    public int getLastApplied() {
        return lastApplied;
    }

    @Override
    public void applyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex, Set<NodeEndpoint> lastGroupConfig) {
        this.taskExecutor.submit(() -> doApplyLog(context, index, commandBytes, firstLogIndex, lastGroupConfig));
    }

    /**
     * 应用日志和应用快照在一个线程里
     * 避免了多个线程同时操作日志组件中的数据
     */
    private void doApplyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex, Set<NodeEndpoint> lastGroupConfig) {
        if (index <= lastApplied) {
            return;
        }
        log.debug("apply log {}", index);
        // 应用命令
        applyCommand(commandBytes);
        lastApplied = index;
        // 是否生成快照
        if (shouldGenerateSnapshot(firstLogIndex, index)) {
            context.generateSnapshot(index);
        }
    }

    protected abstract void applyCommand(@Nonnull byte[] commandBytes);

    @Override
    public void applySnapshot(@Nonnull Snapshot snapshot) throws IOException {
        log.info("apply snapshot, last included index {}", snapshot.getLastIncludedIndex());
        doApplySnapshot(snapshot.getDataStream());
        lastApplied = snapshot.getLastIncludedIndex();
    }

    protected abstract void doApplySnapshot(@Nonnull InputStream input) throws IOException;


    @Override
    public void shutdown() {
        try {
            taskExecutor.shutdown();
        } catch (InterruptedException e) {
            throw new StateMachineException(e);
        }
    }
}
