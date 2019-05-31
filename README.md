Reference：
> [https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/why-mq.md](https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/why-mq.md)
[https://juejin.im/post/5b59c6055188257bcc16738c](https://juejin.im/post/5b59c6055188257bcc16738c)

## 1、消息队列的使用场景（作用、优点）
##### 消峰
对于电商秒杀类的场景，突然的高并发请求可能超过系统的极限导致服务停止响应（比如mysql每秒最多抗住2k请求），通过加入MQ可以控制服务每秒的处理数量.
![image.png](https://upload-images.jianshu.io/upload_images/15620186-2cce205a02575bfa.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/440)

##### 异步
想象一下一个创建订单的请求，需要各个表执行写操作，可能需要调其他服务，可能要发短信，发邮件等等，耗时很久，如果引入MQ，只需要发一条消息到MQ就不用管了，响应很快.

##### 解耦
A系统的某个方法需要通过接口调用BCD系统，如果现在E系统也需要A调用，或者D不需要A调用了，A系统的负责人估计会崩溃；A系统完全与其他服务耦合在一起，并且A还需要考虑BCD调用失败要怎么办，要不要重发；
如果使用MQ，A系统只需要往队列里发送消息即可，谁想消费自己取就行了.

## 2、消息队列会带来哪些缺点

1.  系统可用性降低：
需要保证MQ不能挂掉

2.  系统复杂度提高：
引入MQ，怎么保证消息的重复消费？怎么处理消息丢失的情况？

3.  一致性问题：
A服务发送的消息被BCD消费，BC成功D失败，导致数据不一致。

## 3、kafka目前的使用场景：

应用日志收集分析、消息系统、用户行为分析、运行指标、流式处理（Spark、storm）

![image.png](https://upload-images.jianshu.io/upload_images/15620186-25079c3ef37f0534.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![image.png](https://upload-images.jianshu.io/upload_images/15620186-00d9c1971a698b3d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 4、kafka的高吞吐原因：

##### 1.  顺序读写

kafka的消息是不断追加到文件中的，这个特性使kafka可以充分利用磁盘的顺序读写性能，
顺序读写不需要硬盘磁头的寻道时间，只需很少的扇区旋转时间，所以速度远快于随机读写

##### 2. 零拷贝

##### 3. 批量发送

kafka允许进行批量发送消息，producter发送消息的时候，可以将消息缓存在本地,等到了固定条件发送到kafka

（等消息条数到固定条数，一段时间发送一次）

[https://www.jianshu.com/p/6287868c8462?utm_campaign=maleskine&utm_content=note&utm_medium=seo_notes&utm_source=recommendation](https://www.jianshu.com/p/6287868c8462?utm_campaign=maleskine&utm_content=note&utm_medium=seo_notes&utm_source=recommendation)

## 5、消息发送的三种确认方式（发送端可靠性）

生产者发送消息到broker，有三种确认方式：
- acks = 0：producer不会等待broker（leader）发送ack 。效率高，但可能丢失也可能会重发数据
- acks = 1：当leader收到消息后会发送ack确认收到（follower不管），如果丢失则会重发。
- acks = -1：需要等所有follower都同步消息成功后在返回ack，可靠性最高。

## 6、消息存储方式（存储端可靠性）

每一条消息被发送到broker中，会根据partition规则选择被存储到哪一个partition。如果partition规则设置的合理，所有消息可以均匀分布到不同的partition里，这样就实现了水平扩展。

在创建topic时可以指定这个topic对应的partition的数量。在发送一条消息时，可以指定这条消息的key，producer根据这个key和partition机制来判断这个消息发送到哪个partition。

> 如果Broker有多台负载，那么topic的partition会平均分配到每一台上，所以发送的消息也会平均分配到每一台上。

但是这样带来个问题：如果某个Broker单点故障了，这台broker上的partition就不可用了；

kafka的高可靠性的保障来自于另一个叫副本（replication）策略，通过设置副本的相关参数，可以使kafka在性能和可靠性之间做不同的切换。

## 7、副本机制
```
sh kafka-topics.sh --create --zookeeper 192.168.11.140:2181 --replication-factor 3 --partitions 3 --topic MyTopic

--replication-factor表示的副本数
```
![image.png](https://upload-images.jianshu.io/upload_images/15620186-2dbe067fb173b3f6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##### ISR（副本同步队列）

维护的是有资格的follower节点

副本的所有节点都必须要和zookeeper保持连接状态

副本的最后一条消息的offset和leader副本的最后一条消息的offset之间的差值不能超过指定的阀值，这个阀值是可以设置的（replica.lag.max.messages）

![image.png](https://upload-images.jianshu.io/upload_images/15620186-c325754b533021f7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
## 8. 日志的检索方式
kafka一个topic下面的所有消息都是以partition的方式分布式的存储在多个节点上。同时在kafka的机器上，每个Partition其实都会对应一个日志目录，在目录下面会对应多个日志分段(LogSegment)。LogSegment文件由两部分组成，分别为“.index”文件和“.log”文件，分别表示为segment索引文件和数据文件。这两个文件的命令规则为：partition全局的第一个segment从0开始，后续每个segment文件名为上一个segment文件最后一条消息的offset值，数值大小为64位，20位数字字符长度，没有数字用0填充，如下，假设有1000条消息，每个LogSegment大小为100，下面展现了900-1000的索引和Log：
![image.png](https://upload-images.jianshu.io/upload_images/15620186-ac8a9ee0bdf525ce.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

由于kafka消息数据太大，如果全部建立索引，即占了空间又增加了耗时，所以kafka选择了稀疏索引的方式，这样的话索引可以直接进入内存，加快偏查询速度。

简单介绍一下如何读取数据，如果我们要读取第911条数据首先第一步，找到他是属于哪一段的，根据二分法查找到他属于的文件，找到0000900.index和00000900.log之后，然后去index中去查找 (911-900) =11这个索引或者小于11最近的索引,在这里通过二分法我们找到了索引是[10,1367]然后我们通过这条索引的物理位置1367，开始往后找，直到找到911条数据。

上面讲的是如果要找某个offset的流程，但是我们大多数时候并不需要查找某个offset,只需要按照顺序读即可，而在顺序读中，操作系统会对内存和磁盘之间添加page cahe，也就是我们平常见到的预读操作，所以我们的顺序读操作时速度很快。但是kafka有个问题，如果分区过多，那么日志分段也会很多，写的时候由于是批量写，其实就会变成随机写了，随机I/O这个时候对性能影响很大。所以一般来说Kafka不能有太多的partition。针对这一点，RocketMQ把所有的日志都写在一个文件里面，就能变成顺序写，通过一定优化，读也能接近于顺序读。

> 可以思考一下:1.为什么需要分区，也就是说主题只有一个分区，难道不行吗？2.日志为什么需要分段

## 9. Zookeeper 在 Kafka 中的作用
Reference
[https://www.jianshu.com/p/a036405f989c](https://www.jianshu.com/p/a036405f989c)
##### Broker注册
Broker是分布式部署并且相互之间相互独立，但是需要有一个注册系统能够将整个集群中的Broker管理起来，此时就使用到了Zookeeper。在Zookeeper上会有一个专门用来进行Broker服务器列表记录的节点
##### Topic注册
在Kafka中，同一个Topic的消息会被分成多个分区并将其分布在多个Broker上，这些分区信息及与Broker的对应关系也都是由Zookeeper在维护，由专门的节点来记录
##### 生产者负载均衡
由于同一个Topic消息会被分区并将其分布在多个Broker上，因此，生产者需要将消息合理地发送到这些分布式的Broker上，那么如何实现生产者的负载均衡，Kafka支持传统的四层负载均衡，也支持Zookeeper方式实现负载均衡。
##### 消费者负载均衡
与生产者类似，Kafka中的消费者同样需要进行负载均衡来实现多个消费者合理地从对应的Broker服务器上接收消息，每个消费者分组包含若干消费者，每条消息都只会发送给分组中的一个消费者，不同的消费者分组消费自己特定的Topic下面的消息，互不干扰。
##### 分区 与 消费者 的关系
消费组 (Consumer Group)：
consumer group 下有多个 Consumer（消费者）。
对于每个消费者组 (Consumer Group)，Kafka都会为其分配一个全局唯一的Group ID，Group 内部的所有消费者共享该 ID。订阅的topic下的每个分区只能分配给某个 group 下的一个consumer(当然该分区还可以被分配给其他group)。
同时，Kafka为每个消费者分配一个Consumer ID，通常采用"Hostname:UUID"形式表示。
在Kafka中，规定了每个消息分区 只能被同组的一个消费者进行消费，因此，需要在 Zookeeper 上记录 消息分区 与 Consumer 之间的关系，每个消费者一旦确定了对一个消息分区的消费权力，需要将其Consumer ID 写入到 Zookeeper 对应消息分区的临时节点上
##### 消息 消费进度Offset 记录
在消费者对指定消息分区进行消息消费的过程中，需要定时地将分区消息的消费进度Offset记录到Zookeeper上，以便在该消费者进行重启或者其他消费者重新接管该消息分区的消息消费后，能够从之前的进度开始继续进行消息消费。Offset在Zookeeper中由一个专门节点进行记录，其节点路径为:
` /consumers/[group_id]/offsets/[topic]/[broker_id-partition_id] `
节点内容就是Offset的值。
##### 消费者注册
消费者服务器在初始化启动时加入消费者分组的步骤如下

注册到消费者分组。每个消费者服务器启动时，都会到Zookeeper的指定节点下创建一个属于自己的消费者节点，例如/consumers/[group_id]/ids/[consumer_id]，完成节点创建后，消费者就会将自己订阅的Topic信息写入该临时节点。

**对 消费者分组 中的 消费者 的变化注册监听**。每个 消费者 都需要关注所属 消费者分组 中其他消费者服务器的变化情况，即对/consumers/[group_id]/ids节点注册子节点变化的Watcher监听，一旦发现消费者新增或减少，就触发消费者的负载均衡。

**对Broker服务器变化注册监听**。消费者需要对/broker/ids/[0-N]中的节点进行监听，如果发现Broker服务器列表发生变化，那么就根据具体情况来决定是否需要进行消费者负载均衡。

**进行消费者负载均衡**。为了让同一个Topic下不同分区的消息尽量均衡地被多个 消费者 消费而进行 消费者 与 消息 分区分配的过程，通常，对于一个消费者分组，如果组内的消费者服务器发生变更或Broker服务器发生变更，会发出消费者负载均衡。

##### 补充
早期版本的 kafka 用 zk 做 meta 信息存储，consumer 的消费状态，group 的管理以及 offse t的值。考虑到zk本身的一些因素以及整个架构较大概率存在单点问题，新版本中确实逐渐弱化了zookeeper的作用。新的consumer使用了kafka内部的group coordination协议，也减少了对zookeeper的依赖

**以下是kafka在zookeep中的详细存储结构图：**
![image.png](https://upload-images.jianshu.io/upload_images/15620186-c76226d4df7c58d0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 10. 如何保证消息没有被重复消费
##### 为什么会出现消息被重复消费
其中很大的一个原因就是网络不稳定，比如kafka在处理完消息准备commit时出现网络波动（或者重启了）提交失败，那么服务端是不知道这条消息已经被消费了.

##### 怎么避免重复消费
没有根本的解决办法，只能从以下两个层面尽量规避：
1. 消费端处理消息的业务逻辑保持幂等性
就是不管来多少重复消息，最终的处理结果都一样，比如：一个金额累加的消息，不能设计成每次只传一个增加的金额，这样同一个消息重复N次，那么总金额也会累加N次；应该在消息体里传入增加的金额和增加后的金额.

2. 保证每条消息都一个唯一编号并且消息的处理成功和去重表的记录同时出现
消费端每消费一条消息记录一次，可以通过数据库的唯一索引，如果这个编号是自增的可以通过redis等方式，同一条消息消费时，查询数据库发现已经消费则过滤掉；这种方式势必会对消息队列的吞吐量和性能带来影响，如果不是特别重要的业务可以不用处理，因为重复消息的概率很低.

## 11. 如何处理消息丢失的问题
##### 为什么会出现消息丢失
消息丢失的问题，可能出现在生产者、MQ、消费者，以RabbitMQ为例：
![image.png](https://upload-images.jianshu.io/upload_images/15620186-733a808752b662c3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
##### 消费端弄丢了数据
出现的情况可能是由于消费端获得了消息后**自动提交了offset**，让kafka以为你已经消费好了这条消息，但是你正准备处理这条消息时，挂掉了，消息就丢失了.

解决办法是：**关闭自动提交offset**，在出来完之后手动提交；
这可能带来**重复消费**的问题，所以必须要的话，需要自己保证幂等性.

##### kafka弄丢了数据
出现的情况可能是由于某个broker挂掉了，然后重新选举partition的leader，但是有可能follower数据还没同步好，这时候就会丢数据

解决办法是：生产端设置acks=all，即必须是**写入所有 replica 之后，才能认为是写成功了**；然后设置**retries=MAX**，要求一旦写入失败，就无限重试，卡在这里了；当然这会影响kafka写的性能.

##### 生产端弄丢了数据
如果按照上述的思路设置了 acks=all，一定不会丢