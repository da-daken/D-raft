import com.daken.raft.core.node.NodeBuilder;
import com.daken.raft.core.node.NodeEndpoint;
import com.daken.raft.core.node.NodeId;
import com.daken.raft.core.node.NodeImpl;
import com.daken.raft.core.node.role.*;
import com.daken.raft.core.rpc.MockConnector;
import com.daken.raft.core.rpc.message.*;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;
import com.daken.raft.core.schedule.NullScheduler;
import com.daken.raft.core.support.DirectTaskExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NodeImplTest
 */
public class NodeImplTest {

    private NodeBuilder newNodeBuilder(NodeId selfId, NodeEndpoint... endpoints) {
        return new NodeBuilder(Arrays.asList(endpoints), selfId)
                .setScheduler(new NullScheduler())
                .setConnector(new MockConnector())
                .setTaskExecutor(new DirectTaskExecutor());
    }

    /**
     * 测试node启动
     */
    @Test
    public void testStart() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 233)).build();
        node.start();
        AbstractNodeRole role = node.getRole();
        Assert.assertEquals(role.getRoleName(), RoleName.FOLLOWER);
        Assert.assertEquals(role.getTerm(), 0);
        Assert.assertNull(((FollowerNodeRole) role).getVotedFor());
    }

    /**
     * 测试是否会超时选举
     */
    @Test
    public void testElectionTimeoutWhenFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();
        node.electionTimeout();

        CandidateNodeRole role = (CandidateNodeRole) node.getRole();
        Assert.assertEquals(role.getTerm(), 1);
        Assert.assertEquals(role.getVotesCount(), 1);
        MockConnector connector = (MockConnector) node.getContext().getConnector();

        RequestVoteRpc rpc = (RequestVoteRpc) connector.getRpc();
        Assert.assertEquals(rpc.getTerm(), 1);
        Assert.assertEquals(rpc.getCandidateId(), NodeId.of("A"));
        Assert.assertEquals(rpc.getLastLogIndex(), 0);
        Assert.assertEquals(rpc.getLastLogTerm(), 0);
    }

    /**
     * 测试是否收到 RequestVote 消息
     */
    @Test
    public void testOnReceiveRequestVoteRpcFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        RequestVoteRpc requestVoteRpc = new RequestVoteRpc();
        requestVoteRpc.setTerm(1);
        requestVoteRpc.setCandidateId(NodeId.of("C"));
        requestVoteRpc.setLastLogIndex(0);
        requestVoteRpc.setLastLogTerm(0);
        node.onReceiveRequestVoteRpc(new RequestVoteRpcMessage(requestVoteRpc, NodeId.of("C"), null));

        MockConnector connector = (MockConnector) node.getContext().getConnector();
        RequestVoteResult result = (RequestVoteResult) connector.getResult();
        System.out.println(connector.getMessages());
        Assert.assertEquals(result.getTerm(), 1);
        Assert.assertTrue(result.isVoteGranted());
        Assert.assertEquals(NodeId.of("C"), ((FollowerNodeRole) node.getRole()).getVotedFor());
    }

    @Test
    public void testOnReceiveRequestVoteResult() {
        // 开始是 Follower
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        // 选举超时变成 Candidate
        node.electionTimeout();
        // 收到投票变成 Leader
        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));
        LeaderNodeRole role = (LeaderNodeRole) node.getRole();
        Assert.assertEquals(role.getTerm(), 1);
    }

    @Test
    public void testReplicateLog() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        node.electionTimeout();
        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));
        // 发送日志复制请求
        node.replicateLog();

        MockConnector connector = (MockConnector) node.getContext().getConnector();
        Assert.assertEquals(connector.getMessageCount(), 3);

        List<MockConnector.Message> messages = connector.getMessages();
        Set<NodeId> destinationNodeIds = messages.subList(1, 3).stream()
                .map(MockConnector.Message::getDestinationNodeId).collect(Collectors.toSet());
        Assert.assertEquals(destinationNodeIds.size(), 2);
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("B")));
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("C")));

        AppendEntriesRpc rpc = (AppendEntriesRpc) messages.get(2).getRpc();
        Assert.assertEquals(rpc.getTerm(), 1);
    }

    @Test
    public void testOnReceiveAppendEntriesRpcFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(1);
        rpc.setLeaderId(NodeId.of("B"));

        node.onReceiveAppendEntriesRpc(new AppendEntriesRpcMessage(rpc, NodeId.of("B"), null));
        MockConnector connector = (MockConnector) node.getContext().getConnector();
        AppendEntriesResult result = (AppendEntriesResult) connector.getResult();
        Assert.assertEquals(result.getTerm(), 1);
        Assert.assertTrue(result.isSuccess());
        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(role.getTerm(), 1);
        Assert.assertEquals(role.getLeaderId(), NodeId.of("B"));
    }


    @Test
    public void testOnReceiveAppendEntriesNormal() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        node.electionTimeout();
        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));
        node.replicateLog();

        node.onReceiveAppendEntriesResult(new AppendEntriesResultMessage(
                new AppendEntriesResult(1, true),
                NodeId.of("B"),
                null,
                new AppendEntriesRpc()
        ));
    }

}
