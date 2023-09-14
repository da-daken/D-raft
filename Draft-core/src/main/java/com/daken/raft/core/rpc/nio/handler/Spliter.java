package com.daken.raft.core.rpc.nio.handler;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Spliter 提前预读8个字节（代表一个 type int， len int），后面才是要反序列化的东西
 */
public class Spliter extends LengthFieldBasedFrameDecoder {

    private static final int LENGTH_FIELD_OFFSET = 4;
    private static final int LENGTH_FIELD_LENGTH = 4;

    public Spliter() {
        super(Integer.MAX_VALUE, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH);
    }
}
