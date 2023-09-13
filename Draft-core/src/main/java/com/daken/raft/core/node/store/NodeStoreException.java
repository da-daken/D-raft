package com.daken.raft.core.node.store;

/**
 * NodeStoreException
 */
public class NodeStoreException extends RuntimeException {

    public NodeStoreException(Throwable cause) {
        super(cause);
    }

    public NodeStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
