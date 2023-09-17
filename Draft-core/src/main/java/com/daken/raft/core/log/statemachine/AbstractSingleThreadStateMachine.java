package com.daken.raft.core.log.statemachine;

import com.daken.raft.core.support.SingleThreadTaskExecutor;
import com.daken.raft.core.support.TaskExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

/**
 * AbstractSingleThreadStateMachine
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
    public void applyLog(StateMachineContext context, int index, byte[] commandBytes, int firstLogIndex) {
        this.taskExecutor.submit(() -> doApplyLog(context, index, commandBytes, firstLogIndex));
    }

    private void doApplyLog(StateMachineContext context, int index, byte[] commandBytes, int firstLogIndex) {
        if (index <= lastApplied) {
            return;
        }
        applyCommand(commandBytes);
        this.lastApplied = index;

        // TODO 快照
    }

    protected abstract void applyCommand(@Nonnull byte[] commandBytes);

    @Override
    public void shutdown() {
        try {
            taskExecutor.shutdown();
        } catch (InterruptedException e) {
            throw new StateMachineException(e);
        }
    }
}
