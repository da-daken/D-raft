package com.daken.raft.core.node;

import com.daken.raft.core.log.Log;
import com.daken.raft.core.log.MemoryLog;
import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.config.NodeConfig;
import com.daken.raft.core.node.store.MemoryNodeStore;
import com.daken.raft.core.rpc.Connector;
import com.daken.raft.core.schedule.DefaultScheduler;
import com.daken.raft.core.schedule.Scheduler;
import com.daken.raft.core.support.SingleThreadTaskExecutor;
import com.daken.raft.core.support.TaskExecutor;

import java.util.Collection;
import java.util.Collections;

/**
 * NodeBuilder
 */
public class NodeBuilder {

    private NodeConfig config = new NodeConfig();
    private final NodeGroup group;
    private final NodeId selfId;
    private final EventBus eventBus;
    private Scheduler scheduler;
    private Connector connector;
    private TaskExecutor taskExecutor;
    private Log log;

    public NodeBuilder(NodeEndpoint endpoint) {
        this(Collections.singletonList(endpoint), endpoint.getId());
    }

    public NodeBuilder(Collection<NodeEndpoint> endpoint, NodeId selfId) {
        this.group = new NodeGroup(endpoint, selfId);
        this.selfId = selfId;
        this.eventBus = new EventBus(selfId.getValue());
    }

    public NodeBuilder setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public NodeBuilder setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }

    public NodeBuilder setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public NodeBuilder setLog(Log log) {
        this.log = log;
        return this;
    }

    public Node build() {
        return new NodeImpl(buildContext());
    }

    private NodeContext buildContext() {
        NodeContext nodeContext = new NodeContext();
        nodeContext.setSelfId(selfId);
        nodeContext.setGroup(group);
        nodeContext.setConnector(connector);
        nodeContext.setScheduler(scheduler == null ? new DefaultScheduler(config) : scheduler);
        nodeContext.setTaskExecutor(taskExecutor == null ? new SingleThreadTaskExecutor("node") : taskExecutor);
        nodeContext.setEventBus(eventBus);
        nodeContext.setLog(log == null ? new MemoryLog() : log);
        nodeContext.setStore(new MemoryNodeStore());
        return nodeContext;
    }
}
