# D-raft implement
## 根据《分布式一致性算法开发实战》实现raft共识算法
书中原来的实现 [github](https://github.com/xnnyygn/xraft)

[raft 论文翻译](https://github.com/maemual/raft-zh_cn)

[raft 集群选举，日志复制 演示](https://raft.github.io/raftscope/index.html)

[raft 笔记](https://zhuanlan.zhihu.com/p/588995169)
 ### 1. node 选举
 ### 2. 日志复制
 ### 3. 日志快照
 ### 4. k / v 数据库 服务端 客户端
 ### 5. 集群变更（单节点变更）(待实现)
#### 该书分为 定时器模块 日志模块 通信模块 上层服务模块 核心组件模块

#### 为了解决核心组件和通信组件之间双向依赖的关系，使用EventBus作为第三方进行发送
![compoments.png](photos%2Fcompoments.png)
## raft（强一致性） 学习笔记
#### · question1：为什么新 leader 选举出来之后需要创建一条空日志（Noop）？

这是因为 leader 只能 commit 当前任期的日志，不能提交之前任期的日志。 如果不提交空日志，就可能产生这样一种情况：客户端一直未发送日志过来， leader 就一直不产生当前任期的日志，导致之前任期产生的日志会一直无法被提交。 所以，当新 leader 选举成功之后，先在提交一个当前任期的空日志，以此来保证之前的日志都可以被 commit
强领导者型（读写都在leader上）
将系统分为leader选举，log复制，集群节点变更三个过程
### 1. raft 算法的核心：选举和日志复制
#### 选举：实现分区容错性，在一台机器宕机后，选举出新的可用机器
#### 日志复制：同步节点之间的日志（里面存的term和命令）
日志状态机模型：每一个日志都按照相同
的顺序包含相同的指令，所以每一个服务器都执行相同的指令序列。因为每个状
态机都是确定的，每一次执行操作都产生相同的状态和同样的序列
## 整篇论文只有三个RPC
### requestVoteRpc(请求投票)
![requestVoteRpc.png](photos%2FrequestVoteRpc.png)
### appendEntryRpc(日志复制,心跳)
![appendEntryRpc.png](photos%2FappendEntryRpc.png)
### installSnapshotRpc(传输日志快照)
![installSnapshotRpc.png](photos%2FinstallSnapshotRpc.png)
### 选举流程
1. 所有节点初始为Follower 节点

2. Leader心跳超时，就是没有心跳，Follower节点自动升级为Candidate 节点(自荐)

3. 然后Candidate发起requestVote，经过第一轮选举，投票选出Leader

注意：这里选举规则是Candidate 和 Leader 在收到term 比 自己本地的term大的消息后，就会自己退化为Follower节点（候选者降级为追随者）。

4. 要是出现选举超时情况（票数对半 "split vote分割选举"），就再次进入下一轮选举，

5. 出现Leader节点后，Leader节点会发送心跳消息给其他节点，代表选举结束

6. 其他节点接收到心跳消息后就会重置tram自己的计时器，并保持Follower角色。

注意：只要Leader持续不断发送有心跳消息，Follower节点就不变成Candidate节点并发起选举，会收到消息后重置选举定时器。为了防止同时出现很多Candidate节点，每个节点的选举超时时间都是不一致的（150ms-300ms）
#### · 一节点一票，无重复投票，投完票会进行持久化，宕机回来也不会重新进行投票
### 选举中的角色变更
1、当Leader宕机后，Candidate节点升级为Leader。

2、当Leader和Candidate发现有比自己节点大的时候，自动降级为Follower

3、当Leader宕机，Follower升级为Candidate

注意：所有的节点初始为Follower节点。 正常情况下，只有Leader和Follower节点，只有宕机了才有Candidate节点
### 日志条目
存的是 term 和 command 和 index
#### 提交条件：leader 只要收到超过半数的follower复制成功的回复，就会提交（进行持久化），推进commitIndex和appliedIndex
#### follower在收到下一个appendEntryRpc根据里面的commitIndex提交自己的日志
### 日志复制流程
1. Leader节点收到客户端数据变更请求，

2. Leader追加日志，然后发送给Follower节点进行日志复制，AppendEntries

3. Follower节点要是完成后，那么就进行回复

4. 回复成功后，Leader执行日志持久化，

5. 然后Leader节点给各Follower节点发送日志持久化AppendEntries

6. Follower节点持久化成功后，那么就进行回复

Leader进行计算结果， 然后Leader回复结果给客户端，并且推进commitIndex，
#### 为了知道复制进度，leader会通过 groupMember 记录每个节点的 nextIndex(下一个需要复制的索引条目) 和 matchIndex(已经匹配的日志索引)

## 单节点变更流程
1. 节点 D 向领导者申请加入集群；
2. 领导者 A 向新节点 D 同步数据；
3. 领导者 A 将新配置 [A、B、C、D] 作为一个日志项，复制到配置中的所有节点，然后应用新的配置项（这里需要注意的是，每个节点接收到新的配置项肯定是有时差的）；
4. 如果新的日志项应用成功（被大多数节点复制成功），那么新节点添加成功。
