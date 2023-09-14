package com.daken.raft.core.rpc.nio.handler;

import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.nio.NioChannel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于连接远程节点的处理器
 * 除了基本的转发远程节点回复的消息之外
 * 还需要在连接之后马上发送字节的NodeId（服务端的handler要确认是哪个node）
 */
@Slf4j
public class ToRemoteHandler extends AbstractHandler {

    private final NodeId selfNodeId;

    public ToRemoteHandler(EventBus eventBus, NodeId remoteId, NodeId selfNodeId) {
        super(eventBus);
        this.remoteId = remoteId;
        this.selfNodeId = selfNodeId;
    }

    /**
     * 连接成功后发送自己的节点ID
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.write(selfNodeId);
        channel = new NioChannel(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("receive {} from {}", msg, remoteId);
        super.channelRead(ctx, msg);
    }
}
