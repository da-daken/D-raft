package com.daken.raft.core.log.entry;

import lombok.AllArgsConstructor;

/**
 * AbstractEntry
 */
@AllArgsConstructor
public abstract class AbstractEntry implements Entry {

    private final int kind;
    protected final int index;
    protected final int term;

    @Override
    public int getKind() {
        return kind;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public EntryMeta getMeta() {
        return new EntryMeta(kind, index, term);
    }
}
