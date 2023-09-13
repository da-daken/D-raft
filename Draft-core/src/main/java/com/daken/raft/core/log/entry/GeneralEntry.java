package com.daken.raft.core.log.entry;

import lombok.ToString;

/**
 * GeneralEntry 普通的日志
 */
@ToString
public class GeneralEntry extends AbstractEntry {

    /**
     * 日志负载
     */
    private final byte[] commandBytes;

    public GeneralEntry(int index, int term, byte[] commandBytes) {
        super(KIND_GENERAL, index, term);
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] getCommandBytes() {
        return this.commandBytes;
    }
}
