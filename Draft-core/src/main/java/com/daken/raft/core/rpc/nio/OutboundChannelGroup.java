package com.daken.raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Address;
import com.daken.raft.core.rpc.nio.handler.Spliter;
import com.daken.raft.core.rpc.nio.handler.ToRemoteHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;

/**
 * OutboundChannelGroup
 */
@Slf4j
public class OutboundChannelGroup {

    /**
     * 复用服务端的工作线程组
     */
    private final EventLoopGroup workerGroup;
    private final EventBus eventBus;
    private final NodeId selfNodeId;
    private final int connectTimeoutMillis;
    private final ConcurrentMap<NodeId, FutureTask<NioChannel>> channelMap = new ConcurrentHashMap<>();

    OutboundChannelGroup(EventLoopGroup workerGroup, EventBus eventBus, NodeId selfNodeId, int logReplicationInterval) {
        this.workerGroup = workerGroup;
        this.eventBus = eventBus;
        this.selfNodeId = selfNodeId;
        this.connectTimeoutMillis = logReplicationInterval / 2;
    }

    /**
     * 惰性连接方法，若这个节点的连接存在，则用，不存在则创建一个连接
     * @param nodeId
     * @param address
     * @return
     */
    NioChannel getOrConnect(NodeId nodeId, Address address) {
        FutureTask<NioChannel> future = channelMap.get(nodeId);
        if (future == null) {
            future = channelMap.computeIfAbsent(nodeId, k -> new FutureTask<>(() -> connection(nodeId, address)));
        }
        future.run();
        try {
            return future.get();
        } catch (Exception e) {
            channelMap.remove(nodeId);
            throw new ChannelException("failed to get channel to node " + nodeId, e);
        }
    }

    private NioChannel connection(NodeId nodeId, Address address) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Spliter());
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new ToRemoteHandler(eventBus, nodeId, selfNodeId));
                    }
                });
        ChannelFuture future = bootstrap.connect(address.getHost(), address.getPort()).sync();
        if (!future.isSuccess()) {
            throw new ChannelException("failed to connect", future.cause());
        }
        log.debug("channel OUTBOUND-{} connected", nodeId);
        Channel nettyChannel = future.channel();
        nettyChannel.closeFuture().addListener((ChannelFutureListener) cf -> {
            log.debug("channel OUTBOUND-{} disconnected", nodeId);
            channelMap.remove(nodeId);
        });
        return new NioChannel(nettyChannel);
    }

    void closeAll() {
        log.debug("close all outbound channels");
        channelMap.forEach((nodeId, nioChannelFuture) -> {
            try {
                nioChannelFuture.get().close();
            } catch (Exception e) {
                log.warn("failed to close", e);
            }
        });
    }

}
