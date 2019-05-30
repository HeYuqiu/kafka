引用：
> [https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/why-mq.md](https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/why-mq.md)
[https://juejin.im/post/5b59c6055188257bcc16738c](https://juejin.im/post/5b59c6055188257bcc16738c)

## 1、消息队列的使用场景（作用、优点）
*   消峰
*   异步
*   解耦

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
