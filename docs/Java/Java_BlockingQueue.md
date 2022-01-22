## Java.Util.Concurrent 文档集合

[BlockingQueue Overview](#blockingqueue-overview)
[1. BlockingQueue](#1-blockingqueue)
[2. ArrayBlockingQueue](#2-arrayblockingqueue)
[3. LinkedBlockingQueue](#3-linkedblockingqueue)
[4. LinkedBlockingDeque](#4-linkedblockingdeque)
[5. PriorityBlockingQueue](#5-priorityblockingqueue)
[6. DelayQueue](#6-delayqueue)
[7. SynchronousQueue](#7-synchronousqueue)
[8. LinkedTransferQueue](#8-linkedtransferqueue)

### BlockingQueue Overview

### 1. BlockingQueue
A Queue that additionally supports operations that wait for the queue to become nonempty when retrieving an element, and wait for space to become available in the queue when storing an element.

一种队列，此外，它还支持在检索元素时等待队列变为非空，在存储元素时等待队列中的空间变为可用的操作。

BlockingQueue methods come in four forms, with different ways of handling operations that cannot be satisfied immediately, but may be satisfied at some point in the future: one throws an exception, the second returns a special value (either null or false, depending on the operation), the third blocks the current thread indefinitely until the operation can succeed, and the fourth blocks for only a given maximum time limit before giving up. These methods are summarized in the following table:

BlockingQueue方法有四种形式，有不同的处理操作的方法，这些操作不能立即满足，但在将来的某个时候可能会满足：一种抛出异常，另一种返回特殊值（null 或 false，取决于操作），第三个线程无限期地阻塞当前线程，直到操作成功，第四个线程在放弃之前只阻塞给定的最大时间限制。下表总结了这些方法：

------ | Throws exception  | Special Value | Blocks | Time out 
------ | ----------------  | ------------- | ------ | -----------
Insert | add(e)            | offer(e)      | put(e) | offer(e, time, unit)
Remove | remove()          | poll()        | take() | poll(time, unit)
Examine| element()         | peek()        | ------ | -----

A BlockingQueue does not accept null elements. Implementations throw NulllPointerException on attempts to add, put or offer a null. A null is used as a sentinel value to indicate failure of poll operations.

BlockingQueue不接受空元素。实现在尝试 add、put 或 offer a null 时抛出NullPointerException。null 用作 sentinel 值，以指示轮询操作失败。

A BlockingQueue may be capacity bounded. At any given time it may have a remainingCapacity beyond which no additional elements can be put without blocking. A BlockingQueue without any intrinsic capacity constraints always reports a remaining capacity of Integer.MAX_VALUE.

阻塞队列可能有容量限制。在任何给定的时间，它都可能有一个剩余容量，超过这个容量，就不能不阻塞地放置额外的元件。没有任何内在容量约束的 BlockingQueue 的剩余容量始终报告整数的最大值。

BlockingQueue implementations are designed to be used primarily for producer-consumer queues, but additionally support the Collection interface. So, For example, it is possible to remove an arbitrary element from a queue using remove(x). However, such operations are in general not performed very efficiently, and the intended for only occasional use, such as when a queued message is cancelled.

BlockingQueue 实现主要用于生产者-消费者队列，但还支持 Collection 接口。因此，例如，可以使用 remove（x）从队列中删除任意元素。然而，这样的操作通常不是非常有效地执行的，并且仅用于偶尔使用，例如当队列消息被取消时。

BlockingQueue implementations are thread-safe. All queuing methods achieve their effects atomically using internal locks or other forms of concurrency control. However, the bulk Collection operations addAll, containsAll, retainAll and removeAll are not necessarily performed atomically unless specified otherwise in an implementation. So it is possible, for example, for addAll(c) to fail (throwing an exception) after adding only some of the elements in c.

BlockingQueue 实现是线程安全的。所有排队方法都使用内部锁或其他形式的并发控制以原子方式实现其效果。但是，除非在实现中另有规定，否则批量收集操作 addAll、containsAll、retainal 和 removeAll 不一定以原子方式执行。因此，例如，addAll（c）在只添加 c 中的一些元素之后失败（引发异常）是可能的。

A BlockingQueue does not intrinsically support any kind of "close" or "shutdown" operation to indicate that no more items will be added. The needs and usage of such features tend to be implementation-dependent. For example, a common tactic is for producers to insert sepcial end-of-stream or poison objects, that are interpreted accordingly when taken by consumers.

BlockingQueue 本质上不支持任何类型的“关闭”或“关闭”操作，以指示不再添加任何项目。这些特性的需求和使用往往取决于实现。例如，一种常见的策略是生产商插入特殊的流末或有毒对象，当消费者使用这些对象时，会相应地进行解释。

Usage example, based on a typical producer-consumer scenario. Note that a BlockingQueue can safely be used with multiple producers and multiple consumers.

使用示例，基于典型的生产者-消费者场景。请注意，BlockingQueue可以安全地用于多个生产者和多个消费者。

```
    class Producer implements Runnable {
        private final BlockingQueue queue;
        Producer(BlockingQueue q) { queue = q; }
        public void run() {
            try {
                while (true) {
                    queue.put(produce());
                }
            } catch (InterruptedException ex) {
                ... handle ...
            }
        }
        Object produce() {...};
    }

    class Consumer implements Runnable {
        private final BlockingQueue queue;
        Consumer(BlockingQueue q) {
            queue = q;
        }
        public void run() {
            try {
                while (true) {
                    consume(queue.take());
                }
            } catch (InterruptedException ex) {
                ... handle ...
            }
        }
        void consume(Object x) { ... }
    }

    class Setup {
        void main() {
            BlockingQueue q = new SomeQueueImplementation();
            Producer p = new Producer(q);
            Consumer c1 = new Consumer(q);
            Consumer c2 = new Comsumer(q);
            new Thread(p).start();
            new Thread(c1).start();
            new Thread(c2).start();
        }
    }

```

Memeory consistency effects: As with other concurrent collections, actions in a thread prior to placing an object into a BlockingQueue happen-before actions subsequent to the access or removal of that element from the BlockingQueue in another thread.

内存一致性影响：与其他并发集合一样，在将对象放入 BlockingQueue 之前，线程中的操作发生在另一个线程中从 BlockingQueue 访问或移除该元素之后的操作之前。

### 2. ArrayBlockingQueue
A bounded blocking queue backed by an array. This queue orders elements FIFO (first-in-first-out). The head of the queue is that element that has been on the queue the longest time. The tail of the queue is that element that has been on the queue the shortest time. New elements are inserted at the tail of the queue, and the queue retrieval operations obtain elements at the head of the queue.

由数组支持的有界阻塞队列。该队列对元素进行 FIFO 排序（先进先出）。队列的头是在队列上停留时间最长的元素。队列的尾部是在队列上停留时间最短的元素。新元素插入到队列的尾部，队列检索操作获取队列头部的元素。

This is a classic "bounded buffer", in which a fixed-sized array holds elements inserted by producers and extracted by consumers. Once created, the capacity cannot be changed. Attempts to put an element into a full queue will result in the operation blocking; attempts to take an element from an empty queue will similarly block.

这是一个经典的“有界缓冲区”，其中一个固定大小的数组保存生产者插入的元素和消费者提取的元素。一旦创建，容量就无法更改。尝试将元素放入完整队列将导致操作阻塞；尝试从空队列中获取元素也会被阻塞。

This class supports an optional fairness policy for ordering waiting producer and consumer threads. By default, this ordering is not guaranteed. However, a queue constructed with fairness set to true grants threads access in FIFO order. Fairness generally decreases throughput but reduces variability and avoids starvation.

此类支持一个可选的公平策略，用于排序等待的生产者线程和消费者线程。默认情况下，不保证此顺序。然而，公平性设置为 true 的队列以 FIFO 顺序授予线程访问权限。公平性通常会降低吞吐量，但会降低可变性并避免饥饿.


### 3. LinkedBlockingQueue
An optionally-bounded blocking queue based on linked nodes. This queue orders elements FIFO (first-in-first-out). The head of the queue is that element that has been on the queue the longest time. The tail of the queue is that element that has been on the queue the shortest time. New elements are inserted at the tail of the queue, and the queue retrieval operations obtain elements at the head of the queue. Linked queues typically have higher throughput than array-based queues but less predictable performance in most concurrent applications.

基于链接节点实现的可选容量的有界阻塞队列。该队列对元素进行FIFO排序（先进先出）。队列的头是在队列上停留时间最长的元素。队列的尾部是在队列上停留时间最短的元素。新元素插入到队列的尾部，队列检索操作获取队列头部的元素。链接队列通常比基于数组的队列具有更高的吞吐量，但在大多数并发应用程序中性能不太可预测。

The optional capacity bound constructor argument serves as a way to prevent excessive queue expansion. The capacity, if unspecified, is equal to Integer.MAX_VALUE. Linked nodes are dynamically created upon each insertion unless this would bring the queue above capacity.

可选的容量界限构造函数参数用作防止队列过度扩展的方法。容量（如果未指定）等于整数的最大值。链接节点在每次插入时动态创建，除非这会使队列超出容量。

#### LinkedBlockingQueue.Node
A variant of the "two lock queue" algorithm. The putLock gates entry to put (and offer), and has an associated condition for waiting puts. Similarly for the takeLock. The "count" field that they both rely on is maintained as an atomic to avoid needing to get both locks in most cases. Also, to minimize need for puts to get takeLock and vice-versa, cascading notifies are used. When a put notices that is has enabled at least one take, it signals taker. That taker in turn signals others if more items have been entered since the signal. And symmetrically for takes signalling puts. Operations such as remove(Object) and iterators acquire both locks.

“双锁队列”算法的一种变体。一个 putLock 锁定 put 和 offer 操作，并且其关联着一个等待 put 等操作的条件队列。takeLock 与此相似。它们都依赖维护一个 “count” 的原子字段，以避免在大多数情况下需要获得两个锁。此外，此外，为了最大限度地减少 put 获取 takeLock 的需要（反之亦然），使用了临界通知。当至少可以一次 take 时，put 通知会启用，它会向 taker 发送信号。如果信号发出后输入了更多的 items，则该 taker 依次向其他 taker 发出信号。takes 操作 给 puts 操作发信号与此对称。诸如 remove(Obect) 这类的操作将会两个锁都需要获得。

Visibility between writers and readers is provided as follows:

写者和读者之间的可见性如下所示：

Whenever an element is enqueued, the putLock is acquired and count updated. A subsequent reader guarantees visibility to the enqueued Node by either acquiring the putLock (via fullyLock) or by acquiring the takeLock, and the reading n = count.get(); this gives visibility to the first n items.

每当一个元素入队时，就会获取 putLock 并更新计数。随后的读者通过获取 putLock（通过fullyLock）或获取 takeLock 以及读取 n = count.get() 来保证入队节点的可见性。这样就可以看到前n个项目。

To implement weakly consistent iterators, it appears we need to keep all Nodes GC-reachable from a predecessor dequeued Node. That would cause two problems:
- allow a rogue Iterator to cause unbounded memory retention
- cause cross-generational linking of old Nodes to new Nodes if a Node was tenured while live, which generational GCs have a hard time dealing with, causing repeated major collections.

要实现弱一致性迭代器，我们似乎需要保持所有节点都可以从先前的出列节点 GC 访问。这将导致两个问题：
- 允许恶意迭代器导致无限内存保留
- 如果某个节点在使用期间是长期存在的，则会导致旧节点与新节点的跨代链接，这一点各代 GCs 很难处理，从而导致重复的 major collections。
  
However, only non-deleted Nodes need to be reachable from dequeued Nodes, and reachability does not necesarily have to be of the kind understood by the GC. We use the trick of linking a Node that has just been dequeued to itself. Such a self-link implicitly means to advance to head.next.

然而，只有未删除的节点才需要能够从出列的节点访问，并且访问不一定必须是 GC 所理解的那种。我们使用的技巧是将刚出列的节点链接到自身。这样的自我链接显式的意味着 advance to head.next.


### 4. LinkedBlockingDeque

An optionally-bounded blocking deque based on linked nodes.

基于链接节点实现的可选容量有界阻塞双端队列。

The optional capacity bound constructor argument serves as a way to prevent excessive expansion. The capacity, If unspecified, is equal to Integer.MAX_VALUE. Linked nodes are dynamically created upon each insertion unless this would bring the deque above capacity.

可选的容量限制构造函数参数用作防止过度扩展的方法。容量（如果未指定）等于整数的最大值。链接节点在每次插入时动态创建，除非这会使 deque 超过容量。

Most operations run in constant time (ignoring time spent blocking). Exceptions include remove, removeFirstOccurrence, removeLastOccurrence, contains, iterator.remove(), and the bulk operations, all of which run in linear time.

大多数操作在常量时间内运行（忽略阻塞所花费的时间）。例外情况包括 remove、removeFirstOccurrence、removeLastOccurrence、contains、iterator.remove() 和批量操作，所有这些操作都以线性时间运行。

#### LinkedBlockingDeque.Node
Implemented as a simple doubly-linked list protected by a single lock and using conditions to manage blocking.

实现为一个简单的双链接列表，由一个锁保护，并使用条件管理阻塞。

To implement weakly consistent iterators, it appears we need to keep all Nodes GC-reachable from a predecessor dequeued Node. That would cause two problems:
- allow a rogue Iterator to cause unbounded memory retention
- cause cross-generational linking of old Nodes to new Nodes if a Node was tenured while live, which generational GCs have a hard time dealing with, causing repeated major collections.

要实现弱一致性迭代器，我们似乎需要保持所有节点都可以从先前的出列节点GC访问。这将导致两个问题：
- 允许恶意迭代器导致无限内存保留
- 如果某个节点在使用期间是长期存在的，则会导致旧节点与新节点的跨代链接，这一点各代GCs很难处理，从而导致重复的 major collections。 
  
However, only non-deleted Nodes need to be reachable from dequeued Nodes, and reachability does not necessorily have to be of the kind understood by the GC. We use the trick of linking a Node that has just been dequeued to itself. Such a self-link implicity means to jump to "first" (for next links) or "last" (for prev links).

然而，只有未删除的节点才需要能够从出列的节点访问，并且访问不一定必须是 GC 所理解的那种。我们使用的技巧是将刚出列的节点链接到自身。这种自链接隐式意味着跳到“第一个”（下一个链接）或“最后一个”（上一个链接）。

### 5. PriorityBlockingQueue

> The implementation uses an array-based binary heap, with public operations protected with a single lock. However, allocation during resizing uses a simple spinlock(used only while not holding main lock) in order to allow takes to operate concurrently with allocation. This avoids repeated postponement of waiting consumers and consequent element build-up. The need to back away from lock during allocation makes it impossible to simply wrap delegated java.util.PriorityQueue operations within a lock, as was done in a previous version of this class. To maintain interoperability, a plain PriorityQueue is still used during serialization, which maintains compatibility at the expense of transiently doubling overhead.
> 
> 该实现使用基于数组的二叉堆，用单锁来保护其公共操作。但是，调整大小期间的分配使用一个简单的自旋锁（仅在不持有主锁时使用），以便允许 take 与 allocation 同时运行。这避免了等待消费者的重复延迟和随后的元素堆积。在分配过程中需要远离锁，因此不可能简单地用锁包装代理java.util.PriorityQueue操作，如在此类的早期版本中所做的。为了保持互操作性，在序列化过程中仍然使用普通优先级队列，这以暂时加倍开销为代价来保持兼容性。

An unbounded blocking queue that uses the same ordering rules as class PriorityQueue and supplies blocking retrieval operations. While this queue is logically unbounded, attempted additions may fail due to resource exhaustion (causing OutOfMemoryError). This class does not permit null elements. A priority queue relying on natural ordering also does not permit insertion of non-comparable objects (doing so results in ClassCastException).

使用与类 PriorityQueue 相同的排序规则并提供阻塞检索操作的无界阻塞队列。虽然此队列在逻辑上是无界的，但由于资源耗尽（导致 OutOfMemoryError），尝试添加可能会失败。此类不允许存在空元素。依赖于自然顺序的优先级队列也不允许插入不可比较的对象（这样做会导致 ClassCastException）。

This class and its iterator implement all of the optional methods of the Collection and Iterator Interfaces. The Iterator provided in method interator() is not guranteed to traverse the elements of the PriorityBlockingQueue in any particular order. If you need ordered traversal, consider using Arrays.sort(pq.toArray()). Also, method drainTo can be used to remove some or all elements in priority order and place them in another collection.

此类及其迭代器实现了 Collection 和 Iterator 接口的所有可选方法。Iterator 提供的iterator() 方法不保证以任何特定顺序遍历 PriorityBlockingQueue 的元素。如果需要有序遍历，请使用 Arrays.sort(pq.toArray()). 此外，drainTo 方法可用于按优先级顺序删除某些或所有元素，并将它们放置在另一个集合中。

Operations on this class make no guarantees about the ordering of elements with equal priority. If you need to enforce an ordering, you can define custom classes or comparators that use a secondary key to break ties in primary priority values. For example, here is a class that applies first-in-first-out tie-breaking to comparable elements. To use it, you would insert a new FIFOEntry(anEntry) instead of a plain entry object.

此类上的操作不能保证具有同等优先级的元素的顺序。如果需要强制执行排序，可以定义自定义类或比较器，这些类或比较器使用辅助键断开主优先级值中的关联。例如，这里有一个类，它将先进先出的连接中断应用于可比较的元素。要使用它，您需要插入一个新的 FIFOEntry（anEntry），而不是一个普通的entry对象。

```
    class FIFOEntry<E extends Comparable<? super E>> implements Comparable<FIFOEntry<E>> {
        static final AtomicLong seq = new AtomicLong(0);
        final long seqNum;
        final E entry;
        public FIFOEntry(E entry) {
            seqNum = seq.getAndIncrement();
            this.entry = entry;
        }

        public E getEntry() {
            return entry;
        }

        public int compareTo(FIFOEntry<E> other) {
            int res = entry.compareTo(other.entry);
            if (res == 0 && other.entry != this.entry) {
                res = (seqNum < other.seqNum ? -1 : 1);
            }
            return res;
        }
    }
```

### 6. DelayQueue
An unbounded blocking queue of Delayed elements, in which an element can only be taken when its delay has expired. The head of the queue is that Delayed element whose delay expired furthest int past. If no delay has expired there is no head and poll will return null. Expiration occurs when an element's getDelay(TimeUnit.NANOSECONDS) method returns a value less than or equal to zero. Even though unexpired elements cannot be removed using take or poll, they are otherwise treated as normal elements. For example, the size method returns the count of both expired and unexpired elements. This queue does not permit null elements.

一种延迟元素的无界阻塞队列，其中一个元素只有在其延迟过期时才能被取走。队列的头是延迟过期时间最长的延迟元素。如果没有延迟过期，则没有 head，poll 将返回 null。当元素的getDelay（TimeUnit.NANOSECONDS）方法返回小于或等于零的值时，将发生过期。即使无法使用 take 或 poll 删除未过期的元素，它们也会被视为正常元素。例如，size 方法返回过期和未过期元素的计数。此队列不允许空元素。

This class and its iterator implement all of the optional methods of the Collection and Iterator interfaces. The Iterator provided in method iterator() is not guaranteed to traverse the elements of the DelayQueue in any particular order.

此类及其迭代器实现了 Collection 和 Iterator 接口的所有可选方法。Iterator 提供的iterator() 方法不保证以任何特定顺序遍历 DelayQueue 的元素。

```
    public class DelayQueue<E extends Delayed> ... {
        ...
        
        /**
        
        Thread designated to wait for the element at the head of the queue. This variant of the Leader-Follower pattern serves to minimize unnecessary timed waiting. When a thread becomes the leader, it waits only for the next delay to elapse, but other threads await indefinitely. The leader thread must signal some other thread before returning from take() or poll(..), unless some other thread becomes leader in the interim. Whenever the head of the queue is replaced with an element with an earlier expiration time, the leader field is invalidated by being reset to null, and some waiting thread, but not necessarily the current leader, is signalled. So waiting threads must be prepared to acquire and lose leadership while waiting.
        
        指定在队列头部等待元素的线程。这种 leader/follower 模式的变体用于最小化不必要的定时等待。当一个线程成为 leader 时，它只等待下一个延迟过去，而其他线程则无限期地等待。在从take（）或poll（..）返回之前，leader 线程必须向其他线程发送信号，除非其他线程在此期间已经成为 leader。每当队列的头被一个过期时间较早的元素替换时，leader字段就会被重置为 null 而失效，并且一些等待的线程（不一定是当前的 leader）会收到信号。因此，等待线程必须做好准备，在等待过程中获得并失去领导权。

        */

        private Thread leader = null;

        ...
    }
```

#### Delayed
A mix-in style interface for marking objects that should be acted upon after a given delay.

混合接口，用于标记在给定延迟后应执行操作的对象。

An implementation of this interface must define a compareTo method that provides an ordering consistent with its getDelay method.

此接口的实现必须定义一个 compareTo 方法，该方法提供与其 getDelay 方法一致的顺序。

```
public interface Delayed extends Comparable<Delayed> {

    /**
        Returns the remaining delay associated with this object, in the given time unit.
        返回给定时间单位内与此对象关联的剩余延迟。
    */

    long getDelay(TimeUnit unit);
}

```

### 7. SynchronousQueue
A blocking queue in which each insert operation must wait for a corresponding remove operation by another thread, and vice versa. A synchronous queue does not have any internel capacity, not even a capacity of one. You cannot peek at a synchronous queue because an element is only present when you try to remove it; you cannot insert an element (using any method) unless another thread is trying to remove it; you cannot iterate as there is nothing to iterate. The head of the queue is the element that the first queued inserting thread is trying to add to the queue; If there is no such queued thread then no element is available for removal and poll() will return null. For purposes of other Collection methods (for example contains), a SynchronousQueue acts as an empty collection. This queue does not permit null elements.

一种阻塞队列，其中每个插入操作必须等待另一个线程执行相应的删除操作，反之亦然。同步队列没有任何内部容量，甚至没有 1 的容量。您无法查看 (peek) 同步队列，因为只有在尝试删除某个元素时该元素才存在; 除非另一个线程试图删除某个元素，否则使用任何方法都不能插入一个元素；您不能迭代，因为没有可迭代的内容。队列头是第一个排队插入线程试图添加到队列中的元素；如果没有这样的排队线程，则没有可用于删除的元素，poll(）将返回 null。对于其他的集合方法，例如 contains，同步队列都被视作一个空集合。该队列不允许空元素。

Synchronous queues are similar to rendezvous channels used in CSP and Ada. They are well suited for handoff designs, in which an object running in one thread must sync up with an object running in another thread in order to hand it some information, event, or task.

同步队列类似于 CSP 和 Ada 中使用的会合通道。它们非常适合于切换设计，在切换设计中，一个线程中运行的对象必须与另一个线程中运行的对象同步，以便向其传递一些信息、事件或任务。

This class supports an optional fairness policy for ordering waiting producer and consumer threads. By default, this ordering is not guaranteed. However, a queue constructed with fairness set to true grants threads access in FIFO order.

此类支持一个可选的公平策略，用于排序等待的生产者线程和消费者线程。默认情况下，不保证此顺序。然而，公平性设置为 true 的队列以 FIFO 顺序授予线程访问权限。

#### Transferer
This class implements extensions of the dual stack and dual queue algorithms described in "Nonblocking Concurrent Objects with Condition Synchronization", by W. N. Scherer III and M. L. Scott. 18th Annual Conf. on Distributed Computing, Oct. 2004 (see also https://www.cs.rochester.edu/research/synchronization/pseudocode/duals.html). 

此类实现了 [Nonblocking Concurrent Objects with Condition Synchronization](https://www.cs.rochester.edu/research/synchronization/pseudocode/duals.html) 中描述的双端堆栈和双端队列算法的扩展。

The (Lifo) stack is used for non-fair mode, and the (Fifo) queue for fair mode. The performance of the two is generally similar. Fifo usually supports higher throughput under contention but Lifo maintains higher thread locality in common applications.

（后进先出）栈用于非公平模式，而（先进先出）队列用于公平模式。两者的表现大体相似。Fifo 通常在争用情况下支持更高的吞吐量，但 Lifo 在常见应用程序中保持更高的线程局部性。（译者注：此处争用情况支持更高的吞吐量是指队列和入队出队是争用不同的共享结点，而栈则是争用同一个共享结点。此处局部性是指 Cache 和 TLB）

A dual queue (and similarly stack) is one that at any given time either holds "data" -- items provided by put operations, or "requests" -- slots representing take operations, or is empty. A call to "fulfill" (i.e.. a call requesting an item from a queue holding data or vice versa) dequeues a complementary node. The most interesting feature of these queues is that any operation can figure out which mode the queue is in, and act accordingly without needing locks.

双队列（和类似的堆栈）是一个在任何给定时间保存“数据”（put 操作提供的数据）或“请求”（表示 take 操作的插槽）或为空的队列。“fulfill” 的调用（即，从包含数据的队列请求数据的调用，或反之亦然）使补充节点退出队列。这些队列最有趣的特性是，任何操作都可以确定队列处于哪种模式，并在不需要锁的情况下进行相应的操作。

Both the queue and stack extend abstract class Transferer defining the single method transfer that does a put or a take. These are unified into a single method because in dual data structures, the put and take operations are symmetrical, so nearly all code can be combined. The resulting transfer methods are on the long side, but are easier to follow than they would be if broken up into nearly-duplicated parts.

队列和堆栈扩展了定义了一个 transfer 方法的抽象类 Transferer 来做 put 或 take 的操作。因为在双数据结构中，put 和 take 操作是对称的，所以几乎所有代码都可以组合在一起，所以它们被统一到一个方法中。由此产生的 transfer 方法比较长，但比将其分解为几乎重复的部分更容易跟踪。

The queue and stack data structures share many conceptual similarities but very few concrete details. For simplicity, they are kept distinct so that they can later evolve separately.

队列和堆栈数据结构在概念上有许多相似之处，但具体细节却很少。为了简单起见，它们保持不同，以便以后可以单独演化。

The algorithms here differ from the versions in the above paper in extending them for use in synchronous queues, as well as dealing with cancellation. The main differences include:
1. The original algorithms used bit-marked pointers, but the ones here use mode bits in nodes, leading to a number of further adaptations.
2. SynchronousQueues must block threads waiting to become fulfilled.
3. Support For cancellation via timeout and interrupts, including cleaning out cancelled nodes/threads from lists to avoid garbage retention and memory depletion.

这里的算法在扩展它们以用于同步队列以及处理取消方面不同于上面文章中的版本。主要区别包括：
1. 最初的算法使用位标记指针，但这里的算法在节点中使用模式位，这导致了一些进一步的调整。
2. 支持通过超时和中断进行取消，包括从列表中清除已取消的节点/线程，以避免垃圾保留和内存耗尽。

Blocking is mainly accomplished using lockSupport park/unpark, except that nodes that appear to be the next ones to become fulfilled first spin a bit (an multiprocessors only). On very busy synchronous queues, spinning can dramatically improve throughput. And on less busy ones, the amount of spinning is small enough not to be noticeable.

阻塞主要是通过使用 lockSupport park/unpark 来完成的，除了看起来是下一个完成的节点先旋转一点（仅限多处理器）之外。在非常繁忙的同步队列上，旋转可以显著提高吞吐量。在不太繁忙的地方，旋转的数量很小，以至于不明显。

Cleaning is done in different ways in queus vs stacks. For queues, we can almost always remove a node immediately in O(1) time (modulo retries forconsistency checks) when it is cancelled. But if it may be pinned as the current tail, it must wait until some subsequent cancellation. For stacks, we need a potentially O(n) traversal to be sure that we can remove the node, but this can run concurrently with other threads accessing the stack.

在 queus 和 stacks 中，clean 是以不同的方式进行的。对于队列，当节点被取消时，我们几乎总是可以在O（1）时间内立即删除它（一致性检查的模重试）。但如果它可能被固定为当前尾部，它必须等待后续的取消。对于堆栈，我们需要一个潜在的O（n）遍历，以确保可以删除节点，但这可以与访问堆栈的其他线程同时运行。

While garbage collection takes care of most node reclamation issues that otherwise complicate nonblocking algorithms, care is taken to "forget" references to data, other nodes, and threads that might be held on to long-term by blocked threads. In cases where setting to null would otherwise conflict with main algorithms, this is done by changing a node's link to now point to the node itself. This doesn't arise much for Stack nodes (because blocked threads do not hang on to old head pointers), but references in Queue nodes must be aggressively forgotten to avoid reachability of everything any node has ever referred to since arrival.

虽然垃圾收集处理大多数节点回收问题，否则会使非阻塞算法复杂化，但要注意“忘记”对数据、其他节点和线程的引用，这些引用可能会被阻塞的线程长期保留。如果设置为 null 会与主算法冲突，则可以通过将节点的链接更改为指向节点本身来实现。对于堆栈节点来说，这不会出现太多问题（因为阻塞的线程不会挂起旧的头指针），但必须积极地忘记队列节点中的引用，以避免任何节点到达后所引用的所有内容的可达性。

### 8. LinkedTransferQueue
An unbounded TransferQueue based on linked nodes. This queue orders elements FIFO (first-in-first-out) with respect to any given producer. The head of the queue is that element that has been on the queue the longest time for some producer. The tail of the queue is that element that has been on the queue the shortest time for some producer.

基于链接节点的无界 TransferQueue。该队列针对任何给定的生产者对元素 FIFO（先进先出）进行排序。队列的头是某些生产者在队列中停留时间最长的元素。队列的尾部是某些生产者在队列上停留时间最短的元素。

Beware that, unlike in most collections, the size method is NOT a constant-time operation. Because of the asynchronous nature of these queues, determining the current number of elements requires a traversal of the elements, and so may report inaccurate results if this collection is modified during traversal. Additionally, the bulk operations addAll, removeAll, retainAll, containsAll, equals, and toArray are not guaranteed to be performed atomically. For example, an iterator operating concurrently with an addAll operation might view only some of the added elements.

请注意，与大多数集合不同，siz e方法不是常数时间操作。由于这些队列的异步性质，确定当前元素数需要遍历元素，因此如果在遍历期间修改此集合，则可能会报告不准确的结果。此外，批量操作 addAll、removeAll、retainal、containsAll、equals 和 toArray 不保证以原子方式执行。例如，与 addAll 操作同时运行的迭代器可能只查看一些添加的元素。

This class and its iterator implement all of the optional methods of the Collection and Iterator interfaces.

此类及其迭代器实现集合和迭代器接口的所有可选方法。

Memory consistency effects: As with other concurrent collections, actions in a thread prior to placing an object into a LinkedTransferQueue happen-before actions subsequent to the access or removal of that element from the LinkedTransferQueue in another thread.

内存一致性影响：与其他并发集合一样，在将对象放入 LinkedTransferQueue 之前，线程中的操作发生在从另一个线程中的 LinkedTransferQueue 访问或删除该元素之后的操作之前。

#### Overview of Dual Queues with Slack
Dual Queues, introduced by Scherer and Scott are (linked) queues in which nodes may represent either data or requests. When a thread tries to enqueue a data node, but encounters a request node, it instead "matches" and removes it; and vice versa for enqueuing requests. Blocking Dual Queues arrange that threads enqueuing unmatched requests block until other threads provide the match. Dual Synchronous Queues (see Scherer, Lea, & Scott http://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf) additionally arrange that threads enqueuing unmatched data also block. Dual Transfer Queues support all of these modes, as dictated by callers.

Scherer 和 Scott 引入的双队列是（链接的）队列，其中节点可以表示数据或请求。当一个线程试图让一个数据节点入队，但遇到一个请求节点时，它反而“匹配”并删除它；对于入队请求结点，反之亦然。Blocking Dual Queues 安排线程对不匹配的请求进行排队阻塞，直到其他线程提供匹配。Dual Synchronous Queues 另外将不匹配数据入队的线程也阻塞。Dual Transfer Queues 根据调用者者的指示，支持所有这些模式。

A FIFO dual queue may be implemented using a variation of the Michael & Scott (M&S) lock-free queue algorithm (http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf). It maintains two pointer fields, "head", pointing to a (matched) node that in turn points to the first actual (unmatched) queue node (or null if empty); and "tail" that points to the last node on the queue (or again null if empty). For example, here is a possible queue with four data elements:

FIFO 双队列可以使用 Michael&Scott（M&S）无锁队列算法的变体来实现。它维护两个指针字段，“head”，指向一个（匹配的）节点，该节点依次指向第一个实际（不匹配的）队列节点（ 如果为空则为 null）; "tail" 则指向队列上最后一个节点（如果为空，则再次为null）。例如，下面是一个可能包含四个数据元素的队列：

```
  head                tail
    |                   |
    v                   v
    M -> U -> U -> U -> U
```

The M&S queue algorithm is known to be prone to scalability and overhead limitations when maintaining (via CAS) these head and tail pointers. This has led to the development of contention-reducing variants such as elimination arrays (see Moir et al http://portal.acm.org/citation.cfm?id=1074013) and optimistic back pointers (see Ladan-Mozes & Shavit http://people.csail.mit.edu/edya/publications/OptimisticFIFOQueue-journal.pdf). However, the nature of dual queues enables a simpler tactic for improving M&S-style implementations when dual-ness is needed.

众所周知，M&S 队列算法在维护（通过CAS）这些头指针和尾指针时容易受到可伸缩性和开销限制。这导致了竞争减少变体的发展，如消除数组和乐观的反向指针。然而，当需要双重队列时，双重队列的性质使得改进 M&S 风格实现的策略更加简单。

In a dual queue, each node must atomically maintain its match status. While there are other possible variants, we implement this here as: for a data-mode node, matching entails CASing an "item" field from a non-null data value to null upon match, and vice-versa for request nodes, CASing from null to a data value. (Note that the linearization properties of this style of queue are easy to verify -- elements are made available by linking, and unavailable by matching.) Compared to plain M&S queues, this property of dual queues requires one additional successful atomic operation per enq/deq pair. But it also enables lower cost variants of queue maintenance mechanics. (A variation of this idea applies even for non-dual queues that support deletion of interior elements, such as j.u.c.ConcurrentLinkedQueue.)

在双队列中，每个节点必须原子地保持其匹配状态。虽然还有其他可能的变体，但我们在这里实现为：对于数据模式节点，匹配需要在匹配时将“item”字段从非 null 数据值 cas 更新为null 以完成匹配，反之亦然，对于请求节点，则从 null 大小 cas 更新为数据值。（请注意，这种队列样式的线性化属性很容易验证——元素通过链接可用，通过匹配不可用。）。与普通 M&S 队列相比，双队列的这种特性要求每个 enq/deq 对另外一个成功的原子操作。但它也支持队列维护机制的低成本变体。（这种想法的一种变体甚至适用于支持删除内部元素的非双队列，例如j.u.c.ConcurrentLinkedQueue。）

Once a node is matched, its match status can never again change.  We may thus arrange that the linked list of them contain a prefix of zero or more matched nodes, followed by a suffix of zero or more unmatched nodes. (Note that we allow both the prefix and suffix to be zero length, which in turn means that we do not use a dummy header.)  If we were not concerned with either time or space efficiency, we could correctly perform enqueue and dequeue operations by traversing from a pointer to the initial node; CASing the item of the first unmatched node on match and CASing the next field of the trailing node on appends. (Plus some special-casing when initially empty).  While this would be a terrible idea in itself, it does have the benefit of not requiring ANY atomic updates on head/tail fields.

一旦节点匹配，其匹配状态就再也不能更改。因此，我们可以安排它们的链接列表包含零个或多个匹配节点的前缀，后跟零个或多个不匹配节点的后缀。（请注意，我们允许前缀和后缀的长度均为零，这反过来意味着我们不使用虚头结点。）。如果我们不关心时间或空间效率，我们可以通过从指针到初始节点的遍历来正确执行排队和出列操作；在 match 上对第一个不匹配节点的 item 进行 cas 更新，并在尾部追加节点时对 next 字段 cas 更新。（最初为空时，加上一些特殊外壳）。虽然这本身就是一个糟糕的想法，但它的好处是不需要对 head/tail 字段进行任何原子更新。

We introduce here an approach that lies between the extremes of never versus always updating queue (head and tail) pointers. This offers a tradeoff between sometimes requiring extra traversal steps to locate the first and/or last unmatched nodes, versus the reduced overhead and contention of fewer updates to queue pointers. For example, a possible snapshot of a queue is:

我们在这里介绍一种介于从不更新和总是更新队列（头和尾）指针之间的方法。这在有时需要额外的遍历步骤来定位第一个和/或最后一个不匹配的节点，与减少队列指针更新的开销和争用之间提供了折衷。例如，队列的可能快照是：

```
  head           tail
    |              |
    v              v
    M -> M -> U -> U -> U -> U
```

The best value for this "slack" (the targeted maximum distance between the value of "head" and the first unmatched node, and similarly for "tail") is an empirical matter. We have found that using very small constants in the range of 1-3 work best over a range of platforms. Larger values introduce increasing costs of cache misses and risks of long traversal chains, while smaller values increase CAS contention and overhead.

此“松弛”的最佳值（“头部”值与第一个不匹配节点之间的目标最大距离，与“尾部”类似）是一个经验问题。我们发现，在 1 - 3 范围内使用非常小的常数在一系列平台上效果最好。较大的值会增加缓存未命中的成本和长遍历链的风险，而较小的值会增加 CAS 争用和开销。

Dual queues with slack differ from plain M&S dual queues by virtue of only sometimes updating head or tail pointers when matching, appending, or even traversing nodes; in order to maintain a targeted slack.  The idea of "sometimes" may be operationalized in several ways. The simplest is to use a per-operation counter incremented on each traversal step, and to try (via CAS) to update the associated queue pointer whenever the count exceeds a threshold. Another, that requires more overhead, is to use random number generators to update with a given probability per traversal step.

具有松弛的双队列不同于普通 M&S 双队列，因为在匹配、追加甚至遍历节点时，只会偶尔更新头指针或尾指针；以保持目标松弛度。“有时”的想法可以通过几种方式实现。最简单的方法是在每个遍历步骤上使用递增的每操作计数器，并在计数超过阈值时尝试（通过CAS）更新关联的队列指针。另一个需要更多开销的方法是使用随机数生成器，以每个遍历步骤给定的概率进行更新。

In any strategy along these lines, because CASes updating fields may fail, the actual slack may exceed targeted slack. However, they may be retried at any time to maintain targets.  Even when using very small slack values, this approach works well for dual queues because it allows all operations up to the point of matching or appending an item (hence potentially allowing progress by another thread) to be read-only, thus not introducing any further contention.  As described below, we implement this by performing slack maintenance retries only after these points.

在沿着这些路线的任何策略中，由于更新字段的情况可能会失败，因此实际的空闲时间可能会超过目标空闲时间。但是，可以随时重试以保持目标。即使在使用非常小的松弛值时，这种方法也适用于双队列，因为它允许直到匹配或追加项为止的所有操作（因此可能允许另一个线程的进度）都是只读的，因此不会引入任何进一步的争用。如下所述，我们通过仅在这些点之后执行松弛维护重试来实现这一点。

As an accompaniment to such techniques, traversal overhead can be further reduced without increasing contention of head pointer updates: Threads may sometimes shortcut the "next" link path from the current "head" node to be closer to the currently known first unmatched node, and similarly for tail. Again, this may be triggered with using thresholds or randomization.

作为此类技术的一种伴随，可以在不增加头指针更新争用的情况下进一步减少遍历开销：线程有时可以从当前“头”节点缩短“下一个”链接路径，以更接近当前已知的第一个不匹配节点，对于尾节点也是如此。同样，这可能通过使用阈值或随机化触发。

These ideas must be further extended to avoid unbounded amounts of costly-to-reclaim garbage caused by the sequential "next" links of nodes starting at old forgotten head nodes: As first described in detail by Boehm (http://portal.acm.org/citation.cfm?doid=503272.503282) if a GC delays noticing that any arbitrarily old node has become garbage, all newer dead nodes will also be unreclaimed.
(Similar issues arise in non-GC environments.)  To cope with this in our implementation, upon CASing to advance the head pointer, we set the "next" link of the previous head to point only to itself; thus limiting the length of connected dead lists. (We also take similar care to wipe out possibly garbage retaining values held in other Node fields.)  However, doing so adds some further complexity to traversal: If any "next" pointer links to itself, it indicates that the current thread has lagged behind a head-update, and so the traversal must continue from the "head".  Traversals trying to find the current tail starting from "tail" may also encounter self-links, in which case they also continue at "head".

这些思想必须进一步扩展，以避免由于从旧的被遗忘的头节点开始的节点的顺序“next”链接而导致的回收垃圾的无界数量的昂贵成本：Boehm首先详细描述了这一点如果 GC 延迟注意到任意旧节点已成为垃圾，则所有新的垃圾节点也将被取消收回。（在非GC环境中也会出现类似问题。）为了在我们的实现中解决这个问题，在上面 cas 推进头部指针时，我们将前一个头部的“next”链接设置为只指向自身；从而限制了连接的死列表的长度。（我们还采取类似的措施清除其他节点字段中可能保留的垃圾值。）然而，这样做增加了遍历的复杂性：如果任何“next”指针链接到自身，则表明当前线程落后于头更新，因此遍历必须从“head”继续。尝试从“tail”开始查找当前尾部的遍历也可能会遇到自链接，在这种情况下，它们也会在“head”处继续。

It is tempting in slack-based scheme to not even use CAS for updates (similarly to Ladan-Mozes & Shavit). However, this cannot be done for head updates under the above link-forgetting mechanics because an update may leave head at a detached node. And while direct writes are possible for tail updates, they increase the risk of long retraversals, and hence long garbage chains, which can be much more costly than is worthwhile considering that the cost difference of performing a CAS vs write is smaller when they are not triggered on each operation (especially considering that writes and CASes equally require additional GC bookkeeping ("write barriers") that are sometimes more costly than the writes themselves because of contention).

在基于 slack 的方案中，甚至不使用 CAS 进行更新（类似于 ladanmozes 和 Shavit ）。但是，对于上述 link-forgetting 机制下的头部更新，这是无法做到的，因为更新可能会将头部留在分离的节点上。虽然尾部更新可以直接写入，但它们增加了长时间重新转换的风险，从而增加了长垃圾链，考虑到每次操作未触发时，执行 CAS 与写入的成本差异较小（特别是考虑到写入和案例同样需要额外的GC簿记（“写入障碍”），这有时比写入本身的成本更高，因为争论）。

#### Overview of implementation
We use a threshold-based approach to updates, with a slack threshold of two -- that is, we update head/tail when the current pointer appears to be two or more steps away from the first/last node. The slack value is hard-wired: a path greater than one is naturally implemented by checking equality of traversal pointers except when the list has only one element, in which case we keep slack threshold at one. Avoiding tracking explicit counts across method calls slightly simplifies an already-messy implementation. Using randomization would probably work better if there were a low-quality dirt-cheap per-thread one available, but even ThreadLocalRandom is too heavy for these purposes.

我们使用基于阈值的方法进行更新，松弛阈值为2——也就是说，当当前指针距离第一个/最后一个节点两步或更多步时，我们更新 head/tail。slack 值是硬连线的：大于 1 的路径自然是通过检查遍历指针的相等性来实现的，除非列表只有一个元素，在这种情况下，我们将 slack 阈值保持在 1。避免跟踪方法调用中的显式计数会稍微简化已经很混乱的实现。使用随机化可能会更好，如果有一个低质量的非常便宜的每线程一个可用的，但即使是ThreadLocalRandom 是太重了对于这些目标。

With such a small slack threshold value, it is not worthwhile to augment this with path short-circuiting (i.e., unsplicing interior nodes) except in the case of cancellation/removal (see below).

对于如此小的松弛阈值，除了在取消/移除的情况下（见下文），不值得通过路径短路（即，未显示内部节点）来增加该阈值。

We allow both the head and tail fields to be null before any nodes are enqueued; initializing upon first append.  This simplifies some other logic, as well as providing more efficient explicit control paths instead of letting JVMs insert implicit NullPointerExceptions when they are null.  While not currently fully implemented, we also leave open the possibility of re-nulling these fields when empty (which is complicated to arrange, for little benefit.)

在任何节点排队之前，我们允许 head 和 tail 字段都为null；在第一次追加时初始化。这简化了其他一些逻辑，并提供了更高效的显式控制路径，而不是让 JVM 在 null 时插入隐式NullPointerException。虽然目前还没有完全实现，但我们也保留了在这些字段为空时重新清空这些字段的可能性（这很复杂，没有什么好处）

All enqueue/dequeue operations are handled by the single method "xfer" with parameters indicating whether to act as some form of offer, put, poll, take, or transfer (each possibly with timeout). The relative complexity of using one monolithic method outweighs the code bulk and maintenance problems of using separate methods for each case.

所有入/出队列操作都由单个方法“xfer”处理，参数指示是否作为某种形式的提供、放置、轮询、获取或传输（每个可能都有超时）。使用单一方法的相对复杂性超过了为每种情况使用单独方法的代码量和维护问题。

Operation consists of up to three phases. The first is implemented within method xfer, the second in tryAppend, and the third in method awaitMatch.

操作分为三个阶段。第一个在方法 xfer 中实现，第二个在 tryAppend 中实现，第三个在方法 waitmatch 中实现。

##### 1. Try to match an existing node

Starting at head, skip already-matched nodes until finding an unmatched node of opposite mode, if one exists, in which case matching it and returning, also if necessary updating head to one past the matched node (or the node itself if the list has no other unmatched nodes). If the CAS misses, then a loop retries advancing head by two steps until either success or the slack is at most two. By requiring that each attempt advances head by two (if applicable), we ensure that the slack does not grow without bound. Traversals also check if the initial head is now off-list, in which case they start at the new head.

从 head 开始，跳过已匹配的节点，直到找到相反模式的未匹配节点（如果存在），在这种情况下，匹配该节点并返回，如有必要，还将 head 更新为超过匹配节点的节点（如果列表中没有其他未匹配节点，则更新节点本身）。如果 CAS 未命中，则循环将尝试前进头部两步，直到成功或松弛最多两步。通过要求每次尝试向前推进两步（如果适用），我们确保松弛不会毫无限制地增长。遍历还检查初始头部现在是否在列表之外，在这种情况下，它们从新头部开始。

If no candidates are found and the call was untimed poll/offer, (argument "how" is NOW) return.

如果找不到候选人，调用是无限期的 poll/offer 返回（参数“how”现在是）返回。

##### 2. Try to append a new node (method tryAppend)

Starting at current tail pointer, find the actual last node and try to append a new node (or if head was null, establish the first node). Nodes can be appended only if their predecessors are either already matched or are of the same mode. If we detect otherwise, then a new node with opposite mode must have been appended during traversal, so we must restart at phase 1. The traversal and update steps are otherwise similar to phase 1: Retrying upon CAS misses and checking for staleness.  In particular, if a self-link is encountered, then we can safely jump to a node on the list by continuing the traversal at current head.

从当前尾部指针开始，找到实际的最后一个节点并尝试附加一个新节点（或者如果 head 为null，则建立第一个节点）。仅当节点的前驱节点已匹配或具有相同模式时，才能追加节点。如果我们检测到其他情况，那么在遍历过程中必须附加一个具有相反模式的新节点，因此我们必须在阶段 1 重新启动。遍历和更新步骤在其他方面类似于阶段 1：在 CAS 未命中时重试并检查过时性。特别是，如果遇到自链接，那么我们可以通过在当前头部继续遍历，安全地跳到列表上的节点。

On successful append, if the call was ASYNC, return.

成功追加时，如果调用是异步的，则返回。

##### 3. Await match or cancellation (method awaitMatch)
Wait for another thread to match node; instead cancelling if the current thread was interrupted or the wait timed out. On multiprocessors, we use front-of-queue spinning: If a node appears to be the first unmatched node in the queue, it spins a bit before blocking. In either case, before blocking it tries to unsplice any nodes between the current "head" and the first unmatched node.

等待另一个线程匹配节点；如果当前线程被中断或等待超时，则取消。在多处理器上，我们使用队列前旋转：如果一个节点似乎是队列中第一个不匹配的节点，它在阻塞之前旋转一点。无论哪种情况，在阻塞之前，它都会尝试取消当前“头”和第一个不匹配节点之间的任何节点的阻塞。

Front-of-queue spinning vastly improves performance of heavily contended queues. And so long as it is relatively brief and "quiet", spinning does not much impact performance of less-contended queues.  During spins threads check their interrupt status and generate a thread-local random number to decide to occasionally perform a Thread.yield. While yield has underdefined specs, we assume that it might help, and will not hurt, in limiting impact of spinning on busy systems.  We also use smaller (1/2) spins for nodes that are not known to be front but whose predecessors have not blocked -- these "chained" spins avoid artifacts of front-of-queue rules which otherwise lead to alternating nodes spinning vs blocking. Further, front threads that represent phase changes (from data to request node or vice versa) compared to their predecessors receive additional chained spins, reflecting longer paths typically required to unblock threads during phase changes.

Front-of-queue spinning 极大地提高了激烈竞争的队列的性能。只要是相对简短且“安静”的，旋转并不会对竞争较少的队列的性能产生太大影响。在旋转过程中，线程检查其中断状态并生成线程本地随机数，以决定偶尔执行线程。产量虽然成品率的规格定义不明确，但我们认为它可能有助于限制在繁忙系统上旋转的影响，而不会造成伤害。我们还使用较小的（1/2）旋转，用于未知为前端但其前驱结点未阻塞的节点——这些“链式”旋转避免了队列前端规则的瑕疵，否则会导致交替的节点旋转与阻塞。此外，与前一个线程相比，表示阶段变化（从数据到请求节点，反之亦然）的前一个线程接收额外的链式旋转，反映出在阶段变化期间解除阻塞线程通常需要更长的路径。

#### Unlinking removed interior nodes

In addition to minimizing garbage retention via self-linking described above, we also unlink removed interior nodes. These may arise due to timed out or interrupted waits, or calls to remove(x) or Iterator.remove.  Normally, given a node that was at one time known to be the predecessor of some node s that is to be removed, we can unsplice s by CASing the next field of its predecessor if it still points to s (otherwise s must already have been removed or is now offlist). But there are two situations in which we cannot guarantee to make node s unreachable in this way: (1) If s is the trailing node of list (i.e., with null next), then it is pinned as the target node for appends, so can only be removed later after other nodes are appended. (2) We cannot necessarily unlink s given a predecessor node that is matched (including the case of being cancelled): the predecessor may already be unspliced, in which case some previous reachable node may still point to s. (For further explanation see Herlihy & Shavit "The Art of Multiprocessor Programming" chapter 9).  Although, in both cases, we can rule out the need for further action if either s or its predecessor are (or can be made to be) at, or fall off from, the head of list.

除了通过如上所述的自链接最小化垃圾保留外，我们还取消了已移除内部节点的链接。这些问题可能是由于超时或中断的等待，或调用 remove（x）或 Iterator 引起的。去除通常情况下，如果某个节点在某个时间已知是要删除的某些节点的前置节点，我们可以通过在其前置节点的下一个字段中加上大小写（如果该字段仍然指向 s，则 s 必须已被删除或现在不在列表中）来取消切片。但有两种情况下，我们无法保证以这种方式使节点s不可访问：（1）如果 s 是列表的尾随节点（即，下一步为 null），则它被固定为附加的目标节点，因此只能在以后附加其他节点后删除。（2）给定匹配的前置节点（包括被取消的情况），我们不一定取消 s 的链接：前置节点可能已经被取消许可，在这种情况下，一些先前可到达的节点可能仍然指向 s（有关进一步的解释，请参阅 Herlihy&Shavit “多处理器编程的艺术”第9章）。虽然，在这两种情况下，如果 s 或其前身（或可以被设定为）处于或脱离名单之首，我们可以排除采取进一步行动的必要性。

Without taking these into account, it would be possible for an unbounded number of supposedly removed nodes to remain reachable.  Situations leading to such buildup are uncommon but can occur in practice; for example when a series of short timed calls to poll repeatedly time out but never otherwise fall off the list because of an untimed call to take at the front of the queue.

如果不考虑这些因素，可能会有无限数量的假定已删除的节点保持可访问状态。导致此类累积的情况并不常见，但在实践中可能会发生；例如，一系列用于短时间重复调用 poll 超时，但不会因为队列前面的无限期的调用而从列表中消失。

When these cases arise, rather than always retraversing the entire list to find an actual predecessor to unlink (which won't help for case (1) anyway), we record a conservative estimate of possible unsplice failures (in "sweepVotes"). We trigger a full sweep when the estimate exceeds a threshold ("SWEEP_THRESHOLD") indicating the maximum number of estimated removal failures to tolerate before sweeping through, unlinking cancelled nodes that were not unlinked upon initial removal. We perform sweeps by the thread hitting threshold (rather than background threads or by spreading work to other threads) because in the main contexts in which removal occurs, the caller is already timed-out, cancelled, or performing a potentially O(n) operation (e.g. remove(x)), none of which are time-critical enough to warrant the overhead that alternatives would impose on other threads.

当出现这些情况时，我们不会总是重新转换整个列表以找到要取消链接的实际前置项（无论如何，这对情况（1）没有帮助），而是记录可能的未许可故障的保守估计（在“扫描投票”中）。当估计值超过一个阈值（“扫描阈值”）时，我们触发一次完全扫描，该阈值指示在扫描之前，可以容忍的估计删除失败的最大数量，取消在初始删除时未取消链接的已取消节点的链接。我们通过线程达到阈值（而不是后台线程或通过将工作分散到其他线程）来执行扫描，因为在发生删除的主上下文中，调用方已经超时、取消或执行可能的O（n）操作（例如删除（x）），所有这些线程都不具有足够的时间关键性，足以保证替代方案会给其他线程带来的开销。

Because the sweepVotes estimate is conservative, and because nodes become unlinked "naturally" as they fall off the head of the queue, and because we allow votes to accumulate even while sweeps are in progress, there are typically significantly fewer such nodes than estimated.  Choice of a threshold value balances the likelihood of wasted effort and contention, versus providing a worst-case bound on retention of interior nodes in quiescent queues. The value defined below was chosen empirically to balance these under various timeout scenarios.

由于 sweepVotes 估计值是保守的，并且由于节点从队列头上落下时“自然”断开链接，并且由于我们允许在进行扫描时累积投票，因此此类节点通常比估计值少得多。阈值的选择平衡了浪费精力和争用的可能性，而不是提供静态队列中内部节点保留的最坏情况限制。根据经验选择以下定义的值，以在各种超时情况下平衡这些值。

Note that we cannot self-link unlinked interior nodes during sweeps. However, the associated garbage chains terminate when some successor ultimately falls off the head of the list and is self-linked.

请注意，我们无法在扫描期间自链接未链接的内部节点。但是，当某个后续垃圾链最终从列表的头部脱落并自链接时，关联的垃圾链终止。