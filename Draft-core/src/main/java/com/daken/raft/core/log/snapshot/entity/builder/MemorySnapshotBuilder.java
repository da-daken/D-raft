package com.daken.raft.core.log.snapshot.entity.builder;

import com.daken.raft.core.log.LogException;
import com.daken.raft.core.log.snapshot.entity.MemorySnapshot;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * MemorySnapshotBuilder
 */
public class MemorySnapshotBuilder extends AbstractSnapshotBuilder<MemorySnapshot> {

    private final ByteArrayOutputStream output;

    public MemorySnapshotBuilder(InstallSnapshotRpc firstRpc) {
        super(firstRpc);
        output = new ByteArrayOutputStream();
        // 写入第一段数据
        try {
            output.write(firstRpc.getData());
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    @Override
    protected void doWrite(byte[] data) throws IOException {
        output.write(data);
    }

    @Override
    public MemorySnapshot build() {
        return new MemorySnapshot(lastIncludedIndex, lastIncludedTerm, output.toByteArray(), lastConfig);
    }

    @Override
    public void close() {
    }
}
