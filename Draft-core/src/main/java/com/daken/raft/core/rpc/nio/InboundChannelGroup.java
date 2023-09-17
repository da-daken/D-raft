package com.daken.raft.core.rpc.nio;

import com.daken.raft.core.node.NodeId;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * InboundChannelGroup 入口连接，记录其他连接点，在当前 node 变成 leader 后重置所有入口连接（即关闭所有入口连接）
 */
@Slf4j
public class InboundChannelGroup {

    private final List<NioChannel> channels = new CopyOnWriteArrayList<>();

    public void add(NodeId remoteId, NioChannel channel) {
        log.debug("channel INBOUND-{} connected", remoteId);
        channels.add(channel);
        channel.getDelegate().closeFuture().addListener((ChannelFutureListener) future -> {
            log.debug("channel INBOUND-{} disconnected", remoteId);
            remove(channel);
        });
    }

    private void remove(NioChannel channel) {
        channels.remove(channel);
    }

    /**
     * 关闭所有入口连接，变成leader时，即重置所有入口连接
     */
    void closeAll() {
        log.debug("close all inbound channels");
        for (NioChannel channel : channels) {
            channel.close();
        }
    }
}
