package com.daken.raft.kvstore.message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 包装 命令 和 客户端连接
 */
@AllArgsConstructor
@Getter
public class CommandRequest<T> {

    private T command;
    private Channel channel;

    /**
     * 响应结果
     * @param response
     */
    public void reply(Object response) {
        this.channel.writeAndFlush(response);
    }

    /**
     * 用于添加连接关闭时的监听器，可以在客户端自身连接关闭的时候，将该连接移出 pendingMap
     * @param runnable
     */
    public void addCloseListener(Runnable runnable) {
        this.channel.closeFuture().addListener((ChannelFutureListener) future -> runnable.run());
    }

}
