package com.daken.raft.core.log.entry;

/**
 * Entry 日志条目
 */
public interface Entry {

    int KIND_NO_OP = 0;
    int KIND_GENERAL = 1;

    /**
     * 获取类型
     */
    int getKind();

    /**
     * 获取索引
     */
    int getIndex();

    /**
     * 获取任期
     */
    int getTerm();

    /**
     * 获取元信息
     */
    EntryMeta getMeta();

    /**
     * 获取日志负载
     */
    byte[] getCommandBytes();

}
