# Java 并发编程总结-并发数据结构

[TOC]

#### Barrier 和 CountDownLatch 的区别
1. Barrier 关注的是线程计数。一般来说 Barrier 允许重置以反复使用。
2. CountDown 关注的是事件计数。事件完成后即不可重复使用。

#### ConcurrentLinkeQueue 在 Java5、Java7、Java8 有什么区别？版本演进的目标是？

#### ConcurrentHashMap 在 Java5 和 Java8 的区别。

#### ConcurrentHashMap 是如何实现线程安全的?
+ 读线程和写线程之间是利用 volatile 类型的字段 count 来实现线程安全的。
+ 写线程和写线程之间是利用分段锁来实现线程安全的。

#### ConcurrentHashmap 中 modCount 字段是干什么用的？
+ 是用来区分统计比如 size() 方法中统计 size 个数有没有被修改。如果没有被修改，说明统计的 size 是正确的；如果被修改了，就使用上锁的方式重新计算 size()。

##### ConcurrentHashMap 的 get 方法是无锁的，那么它是怎么保证读线程和写线程是线程安全的呢？
+ 在 ConcurrentHashMap 5 中，通过 volatile 的 count 字段实现的线程安全性。每一个分段区间内都维护了一个 volatile 的 count 字段。并且其文档要求，在所有的对 table 的写操作最后都必须更新 count 字段，而所有对外提供的读取操作实现中，第一个动作就是要先读取 volatile 修饰的 count 字段。由于 volatile 语义保证，所以对 volatile 字段的写之前发生的动作，都会在 volatile 读操作之前发生，所以 volatile 读及其之后的读取操作会看见最新 table。并且由于在 ConcurrentHashMap 5 中，采用的是头插法，且其结点的 next 属性是 final 的，所以能够保证遍历过程中的读和写的安全性。仅有一个结点中的值 value 是 volatile 的，在它构造完毕后，其他线程可能看见过，对此，也采取了上锁操作来提供线程安全性。
+ 在 ConcurrentHashMap 8 中，分为三个方面来实现整体读和写的安全性
  + 在哈希桶中，对其数组元素的访问通过 Unsafe.getObjectVolatile 和 putObjectVolatile 其语义和 volatile 类似。也是对数组元素的写动作 happens-before 对数组元素的读。当被访问的元素被哈希映射到一个空桶中时，通过该方法来提供其安全性。
  + 运用尾插法，并在结点中使用 volatile 的 next 属性来提供安全性。当被访问的元素被哈希映射到一个不是空桶且不存在扩容冲突时。该方法用来提供这种情况的安全性。
  + 当被映射到的桶正在发生扩容且元素在新表位置确定时，通过一个占位结点，并通过占位结点找到新表进行安全性访问。当正在发生扩容且元素所在链表在新表位置没有完全确定时，依然读取旧表，并且在扩容时，旧表元素不会被删除。也就是说，移动完成之后就读新表。移动完成之前读旧表。通过在头部插入 ForwordingNode 结点来提供安全性。

另外简单提一下。不管是哪个版本的 ConcurrentHashMap，对于映射到同一个桶的两个写线程是通过锁来实现互斥的。虽然其最终提供的可伸缩性不同。Java 5 固定默认 16 个桶(不可动态扩容），而 Java 8 是每个链表的头结点作为锁。
