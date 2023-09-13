package com.daken.raft.core.log.entry;

import lombok.ToString;

/**
 * NoOpEntry 空日志
 */
@ToString
public class NoOpEntry extends AbstractEntry {

    public NoOpEntry(int index, int term) {
        super(KIND_NO_OP, index, term);
    }

    @Override
    public byte[] getCommandBytes() {
        return new byte[0];
    }
}
