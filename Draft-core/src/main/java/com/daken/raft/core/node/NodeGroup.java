package com.daken.raft.core.node;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * node 集群管理
 */
@Slf4j
public class NodeGroup {

    private final NodeId selfId;
    private Map<NodeId, GroupMember> memberMap;

    /**
     * Create group with single member(standalone).
     *
     * @param endpoint endpoint
     */
    NodeGroup(NodeEndpoint endpoint) {
        this(Collections.singleton(endpoint), endpoint.getId());
    }

    /**
     * Create group.
     *
     * @param endpoints endpoints
     * @param selfId    self id
     */
    NodeGroup(Collection<NodeEndpoint> endpoints, NodeId selfId) {
        this.memberMap = buildMemberMap(endpoints);
        this.selfId = selfId;
    }

    /**
     * Build member map from endpoints.
     *
     * @param endpoints endpoints
     * @return member map
     * @throws IllegalArgumentException if endpoints is empty
     */
    private Map<NodeId, GroupMember> buildMemberMap(Collection<NodeEndpoint> endpoints) {
        Map<NodeId, GroupMember> map = new HashMap<>();
        for (NodeEndpoint endpoint : endpoints) {
            map.put(endpoint.getId(), new GroupMember(endpoint));
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("endpoints is empty");
        }
        return map;
    }

    void resetReplicatingStates(int nextLogIndex) {
        for (GroupMember member : memberMap.values()) {
            if (!member.idEquals(selfId)) {
                member.setReplicatingState(new ReplicatingState(nextLogIndex));
            }
        }
    }

    /**
     * 获取 node 节点对 matchIndex 排序后，取中间的作为参考，判断是否超过了一半
     */
    int getMatchIndex() {
        List<NodeMatchIndex> matchIndices = new ArrayList<>();
        // 把每个member的matchIndex加进来
        for (GroupMember member : memberMap.values()) {
            if (!member.idEquals(selfId)) {
                matchIndices.add(new NodeMatchIndex(member.getId(), member.getMatchIndex()));
            }
        }
        int count = matchIndices.size();
        if (count == 0) {
            throw new IllegalStateException("standalone or no major node");
        }
        Collections.sort(matchIndices);
        log.debug("match indices {}", matchIndices);
        return matchIndices.get(count / 2).getMatchIndex();
    }


    Set<NodeEndpoint> listEndpointOfMajorExceptSelf() {
        Set<NodeEndpoint> endpoints = new HashSet<>();
        for (GroupMember member : memberMap.values()) {
            if (!member.getEndpoint().getId().equals(selfId)) {
                endpoints.add(member.getEndpoint());
            }
        }
        return endpoints;
    }

    Collection<GroupMember> listReplicationTarget() {
        return memberMap.values().stream().filter(m -> !m.getEndpoint().getId().equals(selfId)).collect(Collectors.toList());
    }

    @Nonnull
    GroupMember findMember(NodeId id) {
        GroupMember member = getMember(id);
        if (member == null) {
            throw new IllegalArgumentException("no such node " + id);
        }
        return member;
    }

    boolean isStandalone() {
        return memberMap.size() == 1 && memberMap.containsKey(selfId);
    }

    public GroupMember findSelf() {
        return memberMap.get(selfId);
    }


    @Nullable
    GroupMember getMember(NodeId id) {
        return memberMap.get(id);
    }

    public int getCount() {
        return memberMap.keySet().size();
    }

    /**
     * Node match index.
     *
     * @see NodeGroup#getMatchIndex()
     */
    private static class NodeMatchIndex implements Comparable<NodeMatchIndex> {

        private final NodeId nodeId;
        private final int matchIndex;

        NodeMatchIndex(NodeId nodeId, int matchIndex) {
            this.nodeId = nodeId;
            this.matchIndex = matchIndex;
        }

        int getMatchIndex() {
            return matchIndex;
        }

        @Override
        public int compareTo(@Nonnull NodeMatchIndex o) {
            return -Integer.compare(o.matchIndex, this.matchIndex);
        }

        @Override
        public String toString() {
            return "<" + nodeId + ", " + matchIndex + ">";
        }

    }
}
