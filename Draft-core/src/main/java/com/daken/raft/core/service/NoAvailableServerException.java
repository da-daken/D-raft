package com.daken.raft.core.service;

public class NoAvailableServerException extends RuntimeException {

    public NoAvailableServerException(String message) {
        super(message);
    }

}
