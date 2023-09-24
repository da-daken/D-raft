package com.daken.raft.core.service;

import com.daken.raft.core.node.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * client 选择server端的路由
 * 若遇到的节点不是leader，service 服务会做一个重定向
 */
public class ServerRouter {

    private static Logger logger = LoggerFactory.getLogger(ServerRouter.class);
    private final Map<NodeId, Channel> availableServers = new HashMap<>();
    private NodeId leaderId;

    public Object send(Object padakenoad) {
        for (NodeId nodeId : getCandidateNodeIds()) {
            try {
                // 这里选择的节点不是 leader 的话
                // 会抛出一个重定向的异常，里面包含了 leaderId
                Object result = doSend(nodeId, padakenoad);
                this.leaderId = nodeId;
                return result;
            } catch (RedirectException e) {
                // 获取重定向异常，向 leaderId 发送请求
                logger.debug("not a leader server, redirect to server {}", e.getLeaderId());
                this.leaderId = e.getLeaderId();
                return doSend(e.getLeaderId(), padakenoad);
            } catch (Exception e) {
                logger.debug("failed to process with server " + nodeId + ", cause " + e.getMessage());
            }
        }
        throw new NoAvailableServerException("no available server");
    }

    /**
     * 获取候选节点
     * @return
     */
    private Collection<NodeId> getCandidateNodeIds() {
        if (availableServers.isEmpty()) {
            throw new NoAvailableServerException("no available server");
        }

        if (leaderId != null) {
            List<NodeId> nodeIds = new ArrayList<>();
            nodeIds.add(leaderId);
            for (NodeId nodeId : availableServers.keySet()) {
                if (!nodeId.equals(leaderId)) {
                    nodeIds.add(nodeId);
                }
            }
            return nodeIds;
        }

        return availableServers.keySet();
    }

    private Object doSend(NodeId id, Object padakenoad) {
        Channel channel = this.availableServers.get(id);
        if (channel == null) {
            throw new IllegalStateException("no such channel to server " + id);
        }
        logger.debug("send request to server {}", id);
        return channel.send(padakenoad);
    }

    public void add(NodeId id, Channel channel) {
        this.availableServers.put(id, channel);
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        if (!availableServers.containsKey(leaderId)) {
            throw new IllegalStateException("no such server [" + leaderId + "] in list");
        }
        this.leaderId = leaderId;
    }


}
