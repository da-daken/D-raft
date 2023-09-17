package com.daken.raft.kvstore.message.resp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * GetCommandResponse
 */
@Getter
@Setter
@ToString
public class GetCommandResponse {

    /**
     * 是否找到数，区分 null 和 没有找到数据的情况
     */
    private final boolean found;
    /**
     * 设计为二进制数组，因为store里面的数据可能多样，用通用的二进制表示
     */
    private final byte[] value;

    public GetCommandResponse(byte[] value) {
        this(true, value);
    }

    public GetCommandResponse(boolean found, byte[] value) {
        this.found = found;
        this.value = value;
    }
}
