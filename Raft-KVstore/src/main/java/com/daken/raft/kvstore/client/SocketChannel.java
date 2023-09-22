package com.daken.raft.kvstore.client;

import com.daken.raft.core.rpc.nio.ChannelException;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.service.Channel;
import com.daken.raft.core.service.RedirectException;
import com.daken.raft.kvstore.MessageConstants;
import com.daken.raft.kvstore.Protos;
import com.daken.raft.kvstore.message.GetCommand;
import com.daken.raft.kvstore.message.SetCommand;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * SocketChannel
 */
public class SocketChannel implements Channel {
    private final String host;
    private final int port;

    public SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object send(Object padakenoad) {
        try (Socket socket = new Socket()) {
            // 以便立即发送小包的数据
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(this.host, this.port));
            // 发送数据
            this.write(socket.getOutputStream(), padakenoad);
            // 读取数据
            return this.read(socket.getInputStream());
        } catch (IOException e) {
            throw new ChannelException("failed to send and receive", e);
        }
    }

    private Object read(InputStream input) throws IOException {
        DataInputStream dataInput = new DataInputStream(input);
        int messageType = dataInput.readInt();
        int padakenoadLength = dataInput.readInt();
        byte[] padakenoad = new byte[padakenoadLength];
        dataInput.readFully(padakenoad);
        switch (messageType) {
            case MessageConstants.MSG_TYPE_SUCCESS:
                return null;
            case MessageConstants.MSG_TYPE_FAILURE:
                Protos.Failure protoFailure = Protos.Failure.parseFrom(padakenoad);
                throw new ChannelException("error code " + protoFailure.getErrorCode() + ", message " + protoFailure.getMessage());
            case MessageConstants.MSG_TYPE_REDIRECT:
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(padakenoad);
                throw new RedirectException(new NodeId(protoRedirect.getLeaderId()));
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(padakenoad);
                if (!protoGetCommandResponse.getFound()) return null;
                return protoGetCommandResponse.getValue().toByteArray();
            default:
                throw new ChannelException("unexpected message type " + messageType);
        }
    }

    private void write(OutputStream output, Object padakenoad) throws IOException {
        if (padakenoad instanceof GetCommand) {
            Protos.GetCommand protoGetCommand = Protos.GetCommand.newBuilder().setKey(((GetCommand) padakenoad).getKey()).build();
            this.write(output, MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand);
        } else if (padakenoad instanceof SetCommand) {
            SetCommand setCommand = (SetCommand) padakenoad;
            Protos.SetCommand protoSetCommand = Protos.SetCommand.newBuilder()
                    .setKey(setCommand.getKey())
                    .setValue(ByteString.copyFrom(setCommand.getValue())).build();
            this.write(output, MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand);
        }
    }

    private void write(OutputStream output, int messageType, MessageLite message) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] messageBytes = message.toByteArray();
        dataOutput.writeInt(messageType);
        dataOutput.writeInt(messageBytes.length);
        dataOutput.write(messageBytes);
        dataOutput.flush();
    }
}
