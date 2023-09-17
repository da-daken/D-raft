package com.daken.raft.kvstore.server.handler;

import com.daken.raft.kvstore.message.CommandRequest;
import com.daken.raft.kvstore.message.GetCommand;
import com.daken.raft.kvstore.message.SetCommand;
import com.daken.raft.kvstore.server.Service;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * ServiceHandler 分发命令给服务引用,是 get 请求还是 set 请求
 */
public class ServiceHandler extends ChannelInboundHandlerAdapter {

    private final Service service;

    public ServiceHandler(Service service) {
        this.service = service;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof GetCommand) {
            service.get(new CommandRequest<>((GetCommand) msg, ctx.channel()));
        } else if (msg instanceof SetCommand) {
            service.set(new CommandRequest<>((SetCommand) msg, ctx.channel()));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
