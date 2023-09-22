package com.daken.raft.core.rpc.message.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * InstallSnapshotResult
 */
@AllArgsConstructor
@Getter
@ToString
public class InstallSnapshotResult {

    private final int term;

    private String messageId;

}
