package com.daken.raft.core.log.statemachine;

/**
 * StateMachineException
 */
public class StateMachineException extends RuntimeException {

    public StateMachineException(Throwable cause) {
        super(cause);
    }

    public StateMachineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
