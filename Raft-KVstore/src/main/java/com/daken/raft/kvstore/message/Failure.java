package com.daken.raft.kvstore.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Failure 失败响应
 */
@Getter
@Setter
@ToString
public class Failure {

    private final int errorCode;
    private final String message;

    public Failure(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
