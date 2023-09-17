package com.daken.raft.kvstore.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.daken.raft.kvstore.Protos;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * SetCommand
 */
@Getter
@Setter
@ToString
public class SetCommand {

    /**
     * 自动分配随机的请求ID，UUID生成
     */
    private final String requestId;
    private final String key;
    private final byte[] value;

    public SetCommand(String key, byte[] value) {
        this(UUID.randomUUID().toString(), key, value);
    }

    public SetCommand(String requestId, String key, byte[] value) {
        this.requestId = requestId;
        this.key = key;
        this.value = value;
    }

    public static SetCommand fromBytes(byte[] bytes) {
        try {
            Protos.SetCommand protoSetCommand = Protos.SetCommand.parseFrom(bytes);
            return new SetCommand(protoSetCommand.getRequestId(), protoSetCommand.getKey(), protoSetCommand.getValue().toByteArray());
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("failed to deserialize setCommand", e);
        }
    }

    public byte[] toBytes() {
        Protos.SetCommand command = Protos.SetCommand.newBuilder()
                .setRequestId(this.getRequestId())
                .setKey(this.getKey())
                .setValue(ByteString.copyFrom(this.getValue()))
                .build();
        return command.toByteArray();
    }
}
