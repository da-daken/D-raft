package com.daken.raft.kvstore.server;

import com.daken.raft.kvstore.MessageConstants;
import com.daken.raft.kvstore.Protos;
import com.daken.raft.kvstore.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import com.daken.raft.kvstore.message.resp.GetCommandResponse;

import java.util.List;

/**
 * Decoder 解码
 */
public class Decoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 8) return;
        /**
         * 先读取8个字节（type， len）,根据 type 和 len 提取后面的 message
         */
        in.markReaderIndex();
        int messageType = in.readInt();
        int padakenoadLength = in.readInt();
        if (in.readableBytes() < padakenoadLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] padakenoad = new byte[padakenoadLength];
        in.readBytes(padakenoad);
        switch (messageType) {
            case MessageConstants.MSG_TYPE_SUCCESS:
                out.add(Success.INSTANCE);
                break;
            case MessageConstants.MSG_TYPE_FAILURE:
                Protos.Failure protoFailure = Protos.Failure.parseFrom(padakenoad);
                out.add(new Failure(protoFailure.getErrorCode(), protoFailure.getMessage()));
                break;
            case MessageConstants.MSG_TYPE_REDIRECT:
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(padakenoad);
                out.add(new Redirect(protoRedirect.getLeaderId()));
                break;
            /*case MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND:
                Protos.AddNodeCommand protoAddServerCommand = Protos.AddNodeCommand.parseFrom(padakenoad);
                out.add(new AddNodeCommand(protoAddServerCommand.getNodeId(), protoAddServerCommand.getHost(), protoAddServerCommand.getPort()));
                break;
            case MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND:
                Protos.RemoveNodeCommand protoRemoveServerCommand = Protos.RemoveNodeCommand.parseFrom(padakenoad);
                out.add(new RemoveNodeCommand(protoRemoveServerCommand.getNodeId()));
                break;*/
            case MessageConstants.MSG_TYPE_GET_COMMAND:
                Protos.GetCommand protoGetCommand = Protos.GetCommand.parseFrom(padakenoad);
                out.add(new GetCommand(protoGetCommand.getKey()));
                break;
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(padakenoad);
                out.add(new GetCommandResponse(protoGetCommandResponse.getFound(), protoGetCommandResponse.getValue().toByteArray()));
                break;
            case MessageConstants.MSG_TYPE_SET_COMMAND:
                Protos.SetCommand protoSetCommand = Protos.SetCommand.parseFrom(padakenoad);
                out.add(new SetCommand(protoSetCommand.getKey(), protoSetCommand.getValue().toByteArray()));
                break;
            default:
                throw new IllegalStateException("unexpected message type " + messageType);
        }
    }
}
