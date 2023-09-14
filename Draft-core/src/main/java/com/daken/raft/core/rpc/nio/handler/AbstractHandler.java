package com.daken.raft.core.rpc.nio.handler;

import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;
import com.google.common.eventbus.EventBus;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.rpc.Channel;
import com.daken.raft.core.rpc.message.*;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * AbstractHandler
 */
@Slf4j
public class AbstractHandler extends ChannelDuplexHandler {

    /**
     * 使用 EventBus 解决核心组件和 RPC 组件之间的双向依赖问题，把收到的消息转发给订阅者
     */
    protected final EventBus eventBus;
    protected NodeId remoteId;
    // RPC 组件的 Channel
    protected Channel channel;
    private AppendEntriesRpc lastAppendEntriesRpc;

    public AbstractHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 通过 EventBus 转发消息
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert remoteId != null;
        assert channel != null;

        if (msg instanceof RequestVoteRpc) {
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            eventBus.post(new RequestVoteRpcMessage(rpc, remoteId, channel));
        } else if (msg instanceof RequestVoteResult) {
            eventBus.post(msg);
        } else if (msg instanceof AppendEntriesRpc) {
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            eventBus.post(new AppendEntriesRpcMessage(rpc, remoteId, channel));
        } else if (msg instanceof AppendEntriesResult) {
            AppendEntriesResult result = (AppendEntriesResult) msg;
            if (lastAppendEntriesRpc == null) {
                log.warn("no last append entries rpc");
            } else {
                if (!Objects.equals(result.getMessageId(), lastAppendEntriesRpc.getMessageId())) {
                    log.warn("incorrect append entries rpc message id {}, expected {}", result.getMessageId(), lastAppendEntriesRpc.getMessageId());
                } else {
                    eventBus.post(new AppendEntriesResultMessage(result, remoteId, channel, lastAppendEntriesRpc));
                    lastAppendEntriesRpc = null;
                }
            }
        }
    }

    /**
     * 在发送日志复制的RPC之前记录最后发送出去的RPC，通过 follower 返回的数RPC,可以推进 nextIndex 和 matchIndex
     * @param ctx
     * @param msg
     * @param promise
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof AppendEntriesRpc) {
            lastAppendEntriesRpc = (AppendEntriesRpc) msg;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn(cause.getMessage(), cause);
        ctx.close();
    }
}
