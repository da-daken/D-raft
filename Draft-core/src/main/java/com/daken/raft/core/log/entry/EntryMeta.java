package com.daken.raft.core.log.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * EntryMeta 日志元信息
 */
@Data
@AllArgsConstructor
public class EntryMeta {

    /**
     * 日志类型
     */
    private final int kind;
    /**
     * 日志索引
     */
    private final int index;
    /**
     * 日志的当时任期号
     */
    private final int term;
}
