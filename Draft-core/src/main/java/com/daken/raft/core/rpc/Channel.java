package com.daken.raft.core.rpc;



import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.InstallSnapshotRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.InstallSnapshotResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;

import javax.annotation.Nonnull;

public interface Channel {

    /**
     * Write request vote rpc.
     *
     * @param rpc rpc
     */
    void writeRequestVoteRpc(@Nonnull RequestVoteRpc rpc);

    /**
     * Write request vote result.
     *
     * @param result result
     */
    void writeRequestVoteResult(@Nonnull RequestVoteResult result);

    /**
     * Write append entries rpc.
     *
     * @param rpc rpc
     */
    void writeAppendEntriesRpc(@Nonnull AppendEntriesRpc rpc);

    /**
     * Write append entries result.
     *
     * @param result result
     */
    void writeAppendEntriesResult(@Nonnull AppendEntriesResult result);

    /**
     * Write install snapshot rpc.
     *
     * @param rpc rpc
     */
    void writeInstallSnapshotRpc(@Nonnull InstallSnapshotRpc rpc);

    /**
     * Write install snapshot result.
     *
     * @param result result
     */
    void writeInstallSnapshotResult(@Nonnull InstallSnapshotResult result);


    /**
     * Close channel.
     */
    void close();
}
