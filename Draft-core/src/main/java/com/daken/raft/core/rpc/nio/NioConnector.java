package com.daken.raft.core.rpc.nio;

import com.daken.raft.core.rpc.message.InstallSnapshotRpcMessage;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.InstallSnapshotResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import com.daken.raft.core.rpc.Connector;
import com.daken.raft.core.rpc.nio.handler.FromRemoteHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NioConnector
 */
@Slf4j
public class NioConnector implements Connector {


    /**
     *  Selector 的线程池，此处为单线程
     */
    private final NioEventLoopGroup bossNioEventLoopGroup = new NioEventLoopGroup();

    /**
     * IO线程池，此处为固定数量
     */
    private final NioEventLoopGroup workerNioEventLoopGroup;

    /**
     * 是否和上层服务共享IO线程池
     */
    private final boolean workerGroupShared;


    private final EventBus eventBus;

    /**
     * 节点间通信端口
     */
    private final int port;

    /**
     * 入口 Channel 组
     */
    private final InboundChannelGroup inboundChannelGroup = new InboundChannelGroup();

    /**
     * 出口 Channel 组
     */
    private final OutboundChannelGroup outboundChannelGroup;

    private final ExecutorService executorService = Executors.newCachedThreadPool((r) -> {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((t, e) -> log.warn("failed to process channel", e));
        return thread;
    });

    public NioConnector(NodeId selfNodeId, EventBus eventBus, int port, int logReplicationInterval) {
        this(new NioEventLoopGroup(), false, selfNodeId, eventBus, port, logReplicationInterval);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, NodeId selfNodeId, EventBus eventBus, int port, int logReplicationInterval) {
        this(workerNioEventLoopGroup, true, selfNodeId, eventBus, port, logReplicationInterval);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, boolean workerGroupShared, NodeId selfId,
                        EventBus eventBus, int port, int logReplicationInterval) {
        this.workerNioEventLoopGroup = workerNioEventLoopGroup;
        this.workerGroupShared = workerGroupShared;
        this.eventBus = eventBus;
        this.port = port;
        outboundChannelGroup = new OutboundChannelGroup(workerNioEventLoopGroup, eventBus, selfId, logReplicationInterval);
    }


    @Override
    public void initialize() {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossNioEventLoopGroup, workerNioEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new FromRemoteHandler(eventBus, inboundChannelGroup));
                    }
                });

        log.debug("node listen on port {}", port);
        try {
            serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new ConnectorException("failed to bind port", e);
        }
    }


    @Override
    public void sendRequestVote(@Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {
        for (NodeEndpoint endpoint : destinationEndpoints) {
            log.debug("send {} to node {}", rpc, endpoint.getId());
            executorService.execute(() -> getChannel(endpoint).writeRequestVoteRpc(rpc));
        }
    }

    @Override
    public void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull NodeEndpoint destinationEndpoint) {
        log.debug("reply {} to node {}", result, destinationEndpoint.getId());
        executorService.execute(() -> getChannel(destinationEndpoint).writeRequestVoteResult(result));
    }

    @Override
    public void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        log.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        executorService.execute(() -> getChannel(destinationEndpoint).writeAppendEntriesRpc(rpc));
    }

    @Override
    public void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull NodeEndpoint destinationEndpoint) {
        log.debug("reply {} to node {}", result, destinationEndpoint.getId());
        executorService.execute(() -> getChannel(destinationEndpoint).writeAppendEntriesResult(result));
    }

    @Override
    public void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoint);
        log.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        getChannel(destinationEndpoint).writeInstallSnapshotRpc(rpc);
    }

    @Override
    public void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        log.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        rpcMessage.getChannel().writeInstallSnapshotResult(result);
    }

    @Override
    public void close() {
        log.debug("close connector");
        inboundChannelGroup.closeAll();
        outboundChannelGroup.closeAll();
        bossNioEventLoopGroup.shutdownGracefully();
        if (!workerGroupShared) {
            workerNioEventLoopGroup.shutdownGracefully();
        }
    }

    @Override
    public void resetChannels() {
        inboundChannelGroup.closeAll();
    }

    private Channel getChannel(NodeEndpoint endpoint) {
        return outboundChannelGroup.getOrConnect(endpoint.getId(), endpoint.getAddress());
    }

}
