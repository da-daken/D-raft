package com.daken.raft.core.log;

import com.daken.raft.core.log.event.SnapshotGenerateEvent;
import com.daken.raft.core.log.snapshot.entity.EmptySnapshot;
import com.daken.raft.core.log.snapshot.entity.FileSnapshot;
import com.daken.raft.core.log.snapshot.entity.builder.FileSnapshotBuilder;
import com.daken.raft.core.log.snapshot.entity.builder.FileSnapshotWriter;
import com.daken.raft.core.log.snapshot.entity.builder.SnapshotBuilder;
import com.daken.raft.core.log.statemachine.StateMachineContext;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.google.common.eventbus.EventBus;
import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.log.sequence.FileEntrySequence;
import com.daken.raft.core.log.snapshot.*;
import com.daken.raft.core.node.NodeEndpoint;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * FileLog
 */
public class FileLog extends AbstractLog {

    private final RootDir rootDir;

    public FileLog(File baseDir, EventBus eventBus) {
        super(eventBus);
        rootDir = new RootDir(baseDir);

        LogGeneration latestGeneration = rootDir.getLatestGeneration();
        snapshot = new EmptySnapshot();

        if (latestGeneration != null) {
            entrySequence = new FileEntrySequence(latestGeneration, latestGeneration.getLastIncludedIndex());
        } else {
            LogGeneration firstGeneration = rootDir.createFirstGeneration();
            entrySequence = new FileEntrySequence(firstGeneration, 1);
        }
    }

    @Override
    protected void replaceSnapshot(Snapshot newSnapshot) {
        FileSnapshot fileSnapshot = (FileSnapshot) newSnapshot;
        int lastIncludedIndex = fileSnapshot.getLastIncludedIndex();
        int logIndexOffset = lastIncludedIndex + 1;

        // 获取快照之后的日志
        List<Entry> entries = entrySequence.subList(logIndexOffset);
        // 写入日志快照所在目录
        FileEntrySequence fileEntrySequence = new FileEntrySequence(fileSnapshot.getLogDir(), logIndexOffset);
        fileEntrySequence.append(entries);
        fileEntrySequence.commit(Math.max(commitIndex, lastIncludedIndex));
        fileEntrySequence.close();

        snapshot.close();
        entrySequence.close();
        newSnapshot.close();

        LogDir generation = rootDir.rename(fileSnapshot.getLogDir(), lastIncludedIndex);
        snapshot = new FileSnapshot(generation);
        entrySequence = new FileEntrySequence(generation, logIndexOffset);
        //entrySequence.buildGroupConfigEntryList();
        commitIndex = entrySequence.getCommitIndex();
    }

    @Override
    protected SnapshotBuilder<FileSnapshot> newSnapshotBuilder(InstallSnapshotRpc firstRpc) {
        return new FileSnapshotBuilder(firstRpc, rootDir.getLogDirForInstalling());
    }

    @Override
    protected Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig) {
        LogDir logDir = rootDir.getLogDirForGenerating();
        try (FileSnapshotWriter snapshotWriter = new FileSnapshotWriter(
                logDir.getSnapshotFile(), lastAppliedEntryMeta.getIndex(), lastAppliedEntryMeta.getTerm(), groupConfig)) {
            stateMachine.generateSnapshot(snapshotWriter.getOutput());
        } catch (IOException e) {
            throw new LogException("failed to generate snapshot", e);
        }
        return new FileSnapshot(logDir);
    }

}
