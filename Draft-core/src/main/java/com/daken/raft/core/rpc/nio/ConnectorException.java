package com.daken.raft.core.rpc.nio;

/**
 * ConnectorException
 */
public class ConnectorException extends RuntimeException {

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

}
