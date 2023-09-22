package com.daken.raft.core.log.sequence;

import com.daken.raft.core.log.entry.GroupConfigEntry;
import com.daken.raft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GroupConfigEntryList
 */
public class GroupConfigEntryList implements Iterable<GroupConfigEntry> {

    /**
     * 初始集群配置
     */
    private final Set<NodeEndpoint> initialGroup;

    /**
     * 集群配置日志条目的链表
     */
    private final LinkedList<GroupConfigEntry> entries = new LinkedList<>();

    public GroupConfigEntryList(Set<NodeEndpoint> initialGroup) {
        this.initialGroup = initialGroup;
    }

    public GroupConfigEntry getLast() {
        return entries.isEmpty() ? null : entries.getLast();
    }

    public Set<NodeEndpoint> getLastGroup() {
        return entries.isEmpty() ? initialGroup : entries.getLast().getResultNodeEndpoints();
    }

    /**
     * 获取指定索引前的集群配置日志
     */
    public Set<NodeEndpoint> getLastGroupBeforeOrDefault(int index) {
        Iterator<GroupConfigEntry> iterator = entries.descendingIterator();
        while (iterator.hasNext()) {
            GroupConfigEntry entry = iterator.next();
            if (entry.getIndex() <= index) {
                return entry.getResultNodeEndpoints();
            }
        }
        return initialGroup;
    }

    public void add(GroupConfigEntry entry) {
        entries.add(entry);
    }

    /**
     * 移除指定索引后的集群配置日志
     */
    public GroupConfigEntry removeAfter(int entryIndex) {
        Iterator<GroupConfigEntry> iterator = entries.iterator();
        GroupConfigEntry firstRemovedEntry = null;
        while (iterator.hasNext()) {
            GroupConfigEntry entry = iterator.next();
            if (entry.getIndex() > entryIndex) {
                if (firstRemovedEntry == null) {
                    firstRemovedEntry = entry;
                }
                iterator.remove();
            }
        }
        return firstRemovedEntry;
    }

    public List<GroupConfigEntry> subList(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("from index > to index");
        }
        return entries.stream()
                .filter(e -> e.getIndex() >= fromIndex && e.getIndex() < toIndex)
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Iterator<GroupConfigEntry> iterator() {
        return entries.iterator();
    }

    @Override
    public String toString() {
        return "GroupConfigEntryList{" + entries + '}';
    }

}
