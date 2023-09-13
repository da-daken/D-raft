package com.daken.raft.core.node;

import com.daken.raft.core.log.entry.EntryMeta;
import com.daken.raft.core.rpc.message.req.AppendEntriesRpc;
import com.daken.raft.core.rpc.message.req.RequestVoteRpc;
import com.daken.raft.core.rpc.message.resp.AppendEntriesResult;
import com.daken.raft.core.rpc.message.resp.RequestVoteResult;
import com.google.common.eventbus.Subscribe;
import com.daken.raft.core.node.role.*;
import com.daken.raft.core.node.store.NodeStore;
import com.daken.raft.core.rpc.message.*;
import com.daken.raft.core.schedule.ElectionTimeout;
import com.daken.raft.core.schedule.LogReplicationTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * NodeImpl
 */
@Slf4j
@Getter
public class NodeImpl implements Node {

    private final NodeContext context;
    private volatile boolean started;
    private AbstractNodeRole role;

    public NodeImpl(NodeContext context) {
        this.context = context;
    }

    @Override
    public synchronized void start() {
        if (started) {
            log.warn("node has been started");
            return;
        }

        // 注册事件处理
        context.getEventBus().register(this);

        // 初始化 rpc 连接
        context.getConnector().initialize();

        // 启动的时候节点是 Follower 角色
        // 1、将持久化的任期和投票信息重新赋值 2、启动选举超时定时器
        NodeStore store = context.getStore();
        FollowerNodeRole nodeRole = new FollowerNodeRole(store.getTerm(), store.getVotedFor(),
                null, scheduleElectionTimeout());
        changeToRole(nodeRole);

        started = true;
    }

    private ElectionTimeout scheduleElectionTimeout() {
        return context.getScheduler().scheduleElectionTimeout(this::electionTimeout);
    }


    /**
     * 选举超时,执行超时逻辑
     * 需要从定时线程切换到主处理线程执行具体逻辑
     */
    public void electionTimeout() {
        context.getTaskExecutor().submit(this::doProcessElectionTimeout);
    }

    /**
     * 选举超时
     * 如果是 Follower ：变为 Candidate 发起投票
     * 如果是 Candidate ：重新发起投票
     */
    private void doProcessElectionTimeout() {
        // leader 节点不会有选举超时
        if (RoleName.LEADER.equals(role.getRoleName())) {
            log.warn("node {}, current role is leader, ignore election timeout", context.getSelfId());
            return;
        }

        // 任期加1
        int newTerm = role.getTerm() + 1;
        // 取消原定时任务
        role.cancelTimeOutOrTask();

        // 转变角色为 Candidate
        changeToRole(new CandidateNodeRole(newTerm, scheduleElectionTimeout()));

        // 向所有节点发送选举投票
        RequestVoteRpc requestVoteRpc = new RequestVoteRpc();
        requestVoteRpc.setTerm(newTerm);
        requestVoteRpc.setCandidateId(context.getSelfId());
        // 日志相关处理
        EntryMeta lastEntryMeta = context.getLog().getLastEntryMeta();
        requestVoteRpc.setLastLogIndex(lastEntryMeta.getIndex());
        requestVoteRpc.setLastLogTerm(lastEntryMeta.getTerm());
        // 发起投票
        context.getConnector().sendRequestVote(requestVoteRpc, context.getGroup().listEndpointOfMajorExceptSelf());
    }


    /**
     * 收到投票请求
     *
     * @param rpcMessage rpcMessage
     */
    @Subscribe
    public void onReceiveRequestVoteRpc(RequestVoteRpcMessage rpcMessage) {
        // 切换线程到主处理线程
        context.getTaskExecutor().submit(() -> context.getConnector().replyRequestVote(doProcessRequestVoteRpc(rpcMessage),
                context.getGroup().findMember(rpcMessage.getSourceNodeId()).getEndpoint()));
    }

    /**
     * 收到投票请求结果
     *
     * @param voteResult voteResult
     */
    @Subscribe
    public void onReceiveRequestVoteResult(RequestVoteResult voteResult) {
        // 切换线程到主处理线程
        context.getTaskExecutor().submit(() -> doProcessRequestVoteResult(voteResult));
    }

    /**
     * 收到日志复制请求
     *
     * @param appendEntriesRpcMessage appendEntriesRpcMessage
     */
    @Subscribe
    public void onReceiveAppendEntriesRpc(AppendEntriesRpcMessage appendEntriesRpcMessage) {
        context.getTaskExecutor().submit(() -> context.getConnector().replyAppendEntries(
                doProcessAppendEntriesRpc(appendEntriesRpcMessage),
                context.getGroup().findMember(appendEntriesRpcMessage.getSourceNodeId()).getEndpoint()));
    }

    /**
     * 收到日志复制结果请求 leader 收到 follower 回复
     *
     * @param message AppendEntriesResultMessage
     */
    @Subscribe
    public void onReceiveAppendEntriesResult(AppendEntriesResultMessage message) {
        context.getTaskExecutor().submit(() -> doProcessAppendEntriesResult(message));
    }

    /**
     * 具体处理日志追加结果响应 leader 收到 follower 回复
     *
     * @param message message
     */
    private void doProcessAppendEntriesResult(AppendEntriesResultMessage message) {
        AppendEntriesResult rpc = message.getRpc();

        // 变成 Follower 节点
        if (role.getTerm() < rpc.getTerm()) {
            becomeFollower(rpc.getTerm(), null, null, true);
            return;
        }

        // 检查自己的角色
        if (role.getRoleName() != RoleName.LEADER) {
            log.warn("receive append entries result from node {} but current node is not leader, ignore", message.getSourceNodeId());
            return;
        }

        // 日志相关处理
        NodeId sourceNodeId = message.getSourceNodeId();
        GroupMember member = context.getGroup().getMember(sourceNodeId);
        if (member == null) {
            log.info("unexpected append entries result from node {}, node maybe removed", sourceNodeId);
            return;
        }
        if (rpc.isSuccess()) {
            // follower 追加日志成功，推进 leader 保存的 matchIndex 和 nextIndex (这两个index用于确定给follower发送从哪开始的日志)
            // 上述 matchIndex 和 nextIndex 存储在nodeGroup中的每一个nodeMember
            if (member.advanceReplicatingState(message.getAppendEntriesRpc().getLastEntryIndex())) {
                // 推进 leader 的 commitIndex
                // 用matchIndex进行比较是因为，follower的日志进度比leader小很多，有些不能进行提交，要当前日志的index过了半数才提交当前的
                context.getLog().advanceCommitIndex(context.getGroup().getMatchIndex(), role.getTerm());
            }
        } else {
            // follower 追加日志失败，将 nextIndex - 1 然后，重新发送 AppendEntriesRPC
            if (!member.backOfNextIndex()) {
                log.warn("cannot back off next index more, node {}", sourceNodeId);
            }
        }

    }


    /**
     * 具体处理日志追加请求
     *
     * @param rpcMessage rpcMessage
     * @return
     */
    private AppendEntriesResult doProcessAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) {
        AppendEntriesRpc rpc = rpcMessage.getRpc();

        // 请求的任期小于自己的,返回自己的任期
        if (role.getTerm() > rpc.getTerm()) {
            return new AppendEntriesResult(role.getTerm(), false);
        }

        // 请求的任期大于自己的，变为 Follower，并追加日志
        if (role.getTerm() < rpc.getTerm()) {
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);
            return new AppendEntriesResult(rpc.getTerm(), appendEntries(rpc));
        }

        switch (role.getRoleName()) {
            case FOLLOWER:
                // 重置保存在本地的状态，重置选举超时计时器，并添加日志
                becomeFollower(role.getTerm(), ((FollowerNodeRole) role).getVotedFor(), rpc.getLeaderId(), true);
                return new AppendEntriesResult(role.getTerm(), appendEntries(rpc));
            case CANDIDATE:
                // 说明已经选出了 leader 了，退化为 Follower，并添加日志
                becomeFollower(role.getTerm(), null, rpc.getLeaderId(), true);
                return new AppendEntriesResult(role.getTerm(), appendEntries(rpc));
            case LEADER:
                log.warn("receive append entries rpc from another leader {}, ignore", rpc.getLeaderId());
                return new AppendEntriesResult(rpc.getTerm(), false);
            default:
                throw new IllegalStateException("unexpected node role [" + role.getRoleName() + "]");
        }

    }

    private boolean appendEntries(AppendEntriesRpc rpc) {
        // 追加 leader 日志
        boolean result = context.getLog().appendEntriesFromLeader(rpc.getPrevLogIndex(), rpc.getPrevLogTerm(), rpc.getEntries());
        // 追加成功，根据leader的commitIndex判断是否推进自己的commitIndex
        if (result) {
            // 如果 leaderCommit > commitIndex， 设置本地 commitIndex 为 leaderCommit 和最新日志索引中 较小的一个
            // todo 没懂
            context.getLog().advanceCommitIndex(Math.min(rpc.getLeaderCommit(), rpc.getLastEntryIndex()), rpc.getTerm());
        }
        return result;
    }

    /**
     * 具体处理投票逻辑
     *
     * @param requestVoteRpcMessage rpcMessage
     * @return RequestVoteResult
     */
    private RequestVoteResult doProcessRequestVoteRpc(RequestVoteRpcMessage requestVoteRpcMessage) {
        RequestVoteRpc requestVoteRpc = requestVoteRpcMessage.getRpc();

        // 候选人任期小于自己的任期返回 false
        // Reply false if term < currentTerm (§5.1)
        if (role.getTerm() > requestVoteRpc.getTerm()) {
            log.debug("term from rpc < current term, don't vote ({} < {})", requestVoteRpc.getTerm(), role.getTerm());
            return new RequestVoteResult(role.getTerm(), false);
        }

        // 比较日志
        boolean voteForCandidate = context.getLog().isNewerThan(requestVoteRpc.getLastLogIndex(), requestVoteRpc.getLastLogTerm());

        // 任期数大于自己的任期，转变为 Follower。
        // 并且需要比较候选人的日志是否比自己的日志新，只有候选人的日志比自己的新的时候才投票
        // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
        if (role.getTerm() < requestVoteRpc.getTerm()) {
            // 变成 Follower 重置自己的任期，投票等信息
            becomeFollower(requestVoteRpc.getTerm(), voteForCandidate ? requestVoteRpc.getCandidateId() : null, null, true);
            return new RequestVoteResult(requestVoteRpc.getTerm(), voteForCandidate);
        }

        // 任期一致
        switch (role.getRoleName()) {
            case LEADER: // 任期相同拒绝投票
            case CANDIDATE: // 已经投票给自己了，拒绝投票 (Candidate 只投票给自己)
                return new RequestVoteResult(role.getTerm(), false);
            case FOLLOWER:
                // 两种情况需要投票
                // 1、对方日志比自己新
                // 2、已经给对方投过票了
                FollowerNodeRole follower = (FollowerNodeRole) this.role;
                NodeId votedFor = follower.getVotedFor();

                if ((votedFor == null && voteForCandidate) || Objects.equals(votedFor, requestVoteRpc.getCandidateId())) {

                    // 变成 Follower 重置自己的任期，投票等信息
                    becomeFollower(role.getTerm(), requestVoteRpc.getCandidateId(), null, true);

                    return new RequestVoteResult(role.getTerm(), true);
                }

                return new RequestVoteResult(role.getTerm(), false);
            default:
                throw new IllegalStateException("unexpected node role [" + this.role.getRoleName() + "]");
        }

    }

    private void becomeFollower(int term, NodeId votedFor, NodeId leaderId, boolean scheduleElectionTimeout) {
        role.cancelTimeOutOrTask();
        if (leaderId != null && !leaderId.equals(role.getLeaderId(context.getSelfId()))) {
            log.info("current leader is {}, term {}", leaderId, term);
        }
        ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;
        FollowerNodeRole followerNodeRole = new FollowerNodeRole(term, votedFor, leaderId, electionTimeout);
        changeToRole(followerNodeRole);
    }


    /**
     * 具体处理投票结果逻辑
     *
     * @param voteResult voteResult
     */
    private void doProcessRequestVoteResult(RequestVoteResult voteResult) {
        // 任期大于自己则转变为 Follower
        if (role.getTerm() > voteResult.getTerm()) {
            becomeFollower(voteResult.getTerm(), null, null, true);
            return;
        }

        // 如果自己不是 Candidate 了，说明改节点要们选举成功了，要们选举失败了，忽略请求
        if (!RoleName.CANDIDATE.equals(role.getRoleName())) {
            log.debug("receive request vote result and current role is not candidate, ignore");
            return;
        }

        // 对方没有给自己投票
        if (!voteResult.isVoteGranted()) {
            return;
        }

        CandidateNodeRole role = (CandidateNodeRole) this.role;
        // 当前获取选票数
        int currentVotesCount = role.getVotesCount() + 1;
        int count = context.getGroup().getCount();
        log.debug("votes count {}, major node count {}", currentVotesCount, count);

        // 收到选举投票，取消选举超时任务
        this.role.cancelTimeOutOrTask();

        // 票数过半,成为 Leader
        if (currentVotesCount > count / 2) {
            log.info("become leader, term {}", role.getTerm());
             resetReplicatingStates();

            // 转变为 leader，启动日志复制定时任务
            changeToRole(new LeaderNodeRole(role.getTerm(), scheduleLogReplicationTask()));
            // no-op log
            context.getLog().appendEntry(role.getTerm());
            return;
        }

        // 票数未过半，记录任期数和新的票数。并重新开启选举超时任务
        changeToRole(new CandidateNodeRole(role.getTerm(), currentVotesCount, scheduleElectionTimeout()));
    }

    /**
     * 初始化 leader 的 nextIndex 和 matchIndex
     * nextIndex 初始值为 leader 最新一条日 志的索引 +1； matchIndex 初始值为 0
     */
    private void resetReplicatingStates() {
        context.getGroup().resetReplicatingStates(context.getLog().getNextIndex());
    }

    /**
     * 创建日志复制定时任务
     *
     * @return LogReplicationTask
     */
    private LogReplicationTask scheduleLogReplicationTask() {
        return context.getScheduler().scheduleLogReplicationTask(this::replicateLog);
    }

    public void replicateLog() {
        context.getTaskExecutor().submit((Runnable) this::doReplicateLog);
    }

    private void doReplicateLog() {
        log.debug("replicate log");

        for (GroupMember member : context.getGroup().listReplicationTarget()) {
            doReplicateLog(member, context.getConfig().getMaxReplicationEntries());
        }
    }


    private void doReplicateLog(GroupMember member, int maxEntries) {
        AppendEntriesRpc appendEntriesRpc = context.getLog()
                .createAppendEntriesRpc(role.getTerm(), context.getSelfId(), member.getNextIndex(), maxEntries);

        context.getConnector().sendAppendEntries(appendEntriesRpc, member.getEndpoint());
    }

    @Override
    public synchronized void stop() throws InterruptedException {
        if (!this.started) {
            log.warn("node has been stopped");
            return;
        }

        context.getScheduler().stop();
        context.getConnector().close();
        context.getTaskExecutor().shutdown();

        this.started = false;
    }

    /**
     * 角色变更方法
     *
     * @param newRole 新角色
     */
    private void changeToRole(AbstractNodeRole newRole) {
        log.debug("node {}, role state changed ->{}", context.getSelfId(), newRole);

        // 同步新角色状态到 NodeStore
        NodeStore store = context.getStore();
        store.setTerm(newRole.getTerm());
        if (RoleName.FOLLOWER.equals(newRole.getRoleName())) {
            store.setVotedFor(((FollowerNodeRole) newRole).getVotedFor());
        }
        this.role = newRole;
    }

    // =================================

}
