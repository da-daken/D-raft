package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.entry.Entry;
import com.daken.raft.core.log.entry.EntryMeta;

import java.util.List;

/**
 * EntrySequence 日志条目序列，日志组件组要的操作对象的接口
 */
public interface EntrySequence {

    /**
     * 判断是否为空
     * @return
     */
    boolean isEmpty();

    /**
     * 获取第一条日志的索引
     * @return
     */
    int getFirstLogIndex();

    /**
     * 获取最后一条日志的索引
     * @return
     */
    int getLastLogIndex();

    /**
     * 获取下一条日志的索引
     * @return
     */
    int getNextLogIndex();

    /**
     * 获取从当前索引开始之后的索引
     * [fromIndex, endIndex)
     * @param fromIndex
     * @return
     */
    List<Entry> subList(int fromIndex);

    // [fromIndex, toIndex)
    List<Entry> subList(int fromIndex, int toIndex);

    /**
     * 检查某个条目是否存在
     * @param index
     * @return
     */
    boolean isEntryPresent(int index);

    /**
     * 获取当前日志条目的元信息
     * @param index
     * @return
     */
    EntryMeta getEntryMeta(int index);

    /**
     * 获取当前日志条目（普通信息会比元信息多了负载）
     * @param index
     * @return
     */
    Entry getEntry(int index);

    Entry getLastEntry();

    void append(Entry entry);

    void append(List<Entry> entries);

    /**
     * 推进 commitIndex 提交日志
     * @param index
     */
    void commit(int index);

    /**
     * 获取当前 commitIndex
     * @return
     */
    int getCommitIndex();

    /**
     * 移除当前索引之后的日志条目(用于与leader发生冲突后移除自身的冲突日志)
     * @param index
     */
    void removeAfter(int index);

    void close();

}
