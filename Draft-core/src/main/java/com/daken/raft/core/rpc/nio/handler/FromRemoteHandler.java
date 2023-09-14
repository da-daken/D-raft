package com.daken.raft.core.rpc.nio.handler;

import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.nio.InboundChannelGroup;
import com.daken.raft.core.rpc.nio.NioChannel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * FromRemoteHandler 确认来连接的是哪个 Node
 */
@Slf4j
public class FromRemoteHandler extends AbstractHandler {

    /**
     * 入口 channel 组
     */
    private final InboundChannelGroup channelGroup;

    public FromRemoteHandler(EventBus eventBus, InboundChannelGroup channelGroup) {
        super(eventBus);
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NodeId) {
            remoteId = (NodeId) msg;
            NioChannel nioChannel = new NioChannel(ctx.channel());
            channel = nioChannel;
            channelGroup.add(remoteId, nioChannel);
            return;
        }

        log.debug("receive {} from {}", msg, remoteId);
        super.channelRead(ctx, msg);
    }
}
