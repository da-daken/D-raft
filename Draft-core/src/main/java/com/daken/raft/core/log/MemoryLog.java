package com.daken.raft.core.log;


import com.daken.raft.core.log.sequence.EntrySequence;
import com.daken.raft.core.log.sequence.MemoryEntrySequence;

/**
 * MemoryLog
 */
public class MemoryLog extends AbstractLog {

    public MemoryLog() {
        this(new MemoryEntrySequence());
    }

    MemoryLog(EntrySequence entrySequence) {
        this.entrySequence = entrySequence;
    }

}
