package com.daken.raft.kvstore.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * GetCommand Get请求
 */
@Getter
@Setter
@ToString
public class GetCommand {

    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }
}
