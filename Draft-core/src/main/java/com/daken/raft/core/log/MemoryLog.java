package com.daken.raft.core.log;


import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.sequence.EntrySequence;
import com.daken.raft.core.log.sequence.MemoryEntrySequence;
import com.daken.raft.core.log.snapshot.Snapshot;
import com.daken.raft.core.log.snapshot.entity.EmptySnapshot;
import com.daken.raft.core.log.snapshot.entity.MemorySnapshot;
import com.daken.raft.core.log.snapshot.entity.builder.MemorySnapshotBuilder;
import com.daken.raft.core.log.snapshot.entity.builder.SnapshotBuilder;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * MemoryLog
 */
@Slf4j
public class MemoryLog extends AbstractLog {

    public MemoryLog() {
        this(new EventBus());
    }

    public MemoryLog(EventBus eventBus) {
        this(new EmptySnapshot(), new MemoryEntrySequence(), eventBus);
    }

    public MemoryLog(Snapshot snapshot, EntrySequence entrySequence, EventBus eventBus) {
        super(eventBus);
        this.snapshot = snapshot;
        this.entrySequence = entrySequence;
    }

    @Override
    protected void replaceSnapshot(Snapshot newSnapshot) {
        int logIndexOffset = newSnapshot.getLastIncludedIndex() + 1;
        EntrySequence newEntrySequence = new MemoryEntrySequence(logIndexOffset);
        List<Entry> remainingEntries = entrySequence.subList(logIndexOffset);
        newEntrySequence.append(remainingEntries);
        log.debug("snapshot -> {}", newSnapshot);
        snapshot = newSnapshot;
        log.debug("entry sequence -> {}", newEntrySequence);
        entrySequence = newEntrySequence;
    }

    @Override
    protected SnapshotBuilder<MemorySnapshot> newSnapshotBuilder(InstallSnapshotRpc firstRpc) {
        return new MemorySnapshotBuilder(firstRpc);
    }

    @Override
    protected Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            stateMachine.generateSnapshot(output);
        } catch (IOException e) {
            throw new LogException("failed to generate snapshot", e);
        }
        return new MemorySnapshot(lastAppliedEntryMeta.getIndex(), lastAppliedEntryMeta.getTerm(), output.toByteArray(), groupConfig);
    }

}
