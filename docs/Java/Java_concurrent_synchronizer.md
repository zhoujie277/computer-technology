## The java.util.concurrent Synchronizer Framework

[TOC]

### 1 AbstractOwnableSynchronizer

A synchronizer that may be exclusively owned by a thread. This class provides a basis for creating locks and related synchronizers that may entail a notion of ownership. The AbstractOwnableSynchronizer class itself does not manage or use this information. However, subclasses and tools may use appropriately maintained values to help control and monitor access and provide diagnostics.

一个线程可能独占的同步器。此类提供了创建锁和相关同步器的基础，这些锁和同步器可能包含所有权的概念。AbstractOwnableSynchronizer 类本身不管理或使用此信息。但是，子类和工具可以使用适当维护的值来帮助控制和监视访问并提供诊断。

### 2 AbstractQueuedSynchronizer
Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) that rely on first-in-first-out (FIFO) wait queues. This class is designed to be a useful basis for most kinds of synchronizers that rely on a single atomic int value to represent state. Subclasses must define the protected methods that change this state, and which define what that state means in terms of this object being acquired or released. Given these, the other methods in this class carry out all queuing and blocking mechanics. Subclasses can maintain other state fields, but only the atomically updated int value manipulated using methods getState, setState and compareAndSetState is tracked with respect to synchronization.

提供一个框架，用于实现依赖先进先出（FIFO）等待队列的阻塞锁和相关同步器（信号量、事件等）。此类被设计为大多数类型的同步器的有用基础，这些同步器依赖于单个原子 int 值来表示状态。子类必须定义更改此状态的受保护方法，以及定义此状态在获取或释放此对象方面的含义。鉴于这些，该类中的其他方法执行所有排队和阻塞机制。子类可以维护其他状态字段，但只有使用 getState、setState 和 compareAndSetState方法操作的原子更新的 int 值才能跟踪同步。

Subclasses should be defined as non-public internal helper classes that are used to implement the synchronization properties of their enclosing class. Class AbstractQueuedSynchronizer does not implement any synchronization interface. Instead it defines methods such as acquireInterruptibly that can be invoked as appropriate by concrete locks and related synchronizers to implement their public methods.

子类应定义为非公共内部帮助器类，用于实现其封闭类的同步属性。类 AbstractQueuedSynchronizer 未实现任何同步接口。相反，它定义了一些方法，例如 acquireInterruptibly，具体锁和相关同步器可以适当地调用这些方法来实现它们的公共方法。

This class supports either or both a default exclusive mode and a shared mode. When acquired in exclusive mode, attempted acquires by other threads cannot succeed. Shared mode acquires by multiple threads may (but need not) succeed. This class does not "understand" these differences except in the mechanical sense that when a shared mode acquire succeeds, the next waiting thread (if one exists) must also determine whether it can acquire as well. Threads waiting in the different modes share the same FIFO queue. Usually, implementation subclasses support only one of these modes, but both can come into play for example in a ReadWriteLock. Subclasses that support only exclusive or only shared modes need not define the methods supporting the unused mode.

此类支持默认独占模式和共享模式之一或两者。在独占模式下获取时，其他线程尝试的获取无法成功。多线程获取共享模式可能（但不需要）成功。这个类不“理解”这些差异，除非在机械意义上，当共享模式获取成功时，下一个等待线程（如果存在）也必须确定它是否可以获取。在不同模式下等待的线程共享相同的 FIFO 队列。通常，实现子类只支持其中一种模式，但这两种模式都可以发挥作用，例如在 ReadWriteLock 中。仅支持独占或共享模式的子类不需要定义支持未使用模式的方法。

This class defines a nested AbstractQueuedSynchronizer.ConditionObject class that can be used as a Condition implementation by subclasses supporting exclusive mode for which method isHeldExclusively reports whether synchronization is exclusively held with respect to the current thread, method release invoked with the current getState value fully releases this object, and acquire, given this saved state value, eventually restores this object to its previous acquired state. No AbstractQueuedSynchronizer method otherwise creates such a condition, so if this constraint cannot be met, do not use it. The behavior of AbstractQueuedSynchronizer.ConditionObject depends of course on the semantics of its synchronizer implementation.

此类定义嵌套的 AbstractQueuedSynchronizer.ConditionObject 类，可由支持独占模式的子类用作条件实现，方法 isHeldExclusively 为其报告是否以独占方式保持与当前线程的同步，使用当前 getState 值调用的方法 release 完全释放此对象，并获取，给定此保存的状态值，最终将此对象恢复到其先前获取的状态。没有任何 AbstractQueuedSynchronizer 方法会创建这样的条件，因此如果无法满足此约束，请不要使用它。AbstractQueuedSynchronizer.ConditionObject 的行为当然取决于其同步器实现的语义。

This class provides inspection, instrumentation, and monitoring methods for the internal queue, as well as similar methods for condition objects. These can be exported as desired into classes using an AbstractQueuedSynchronizer for their synchronization mechanics.

此类提供内部队列的检查、检测和监视方法，以及条件对象的类似方法。可以根据需要使用AbstractQueuedSynchronizer 将它们导出到类中，作为它们的同步机制。

Serialization of this class stores only the underlying atomic integer maintaining state, so deserialized objects have empty thread queues. Typical subclasses requiring serializability will define a readObject method that restores this to a known initial state upon deserialization.

此类的序列化只存储底层原子整数维护状态，因此反序列化对象具有空线程队列。需要序列化的典型子类将定义一个 readObject 方法，该方法在反序列化时将其恢复到已知的初始状态。

#### 2.1 Usage
To use this class as the basis of a synchronizer, redefine the following methods, as applicable, by inspecting and/or modifying the synchronization state using getState, setState and/or compareAndSetState:

要将此类用作同步器的基础，请通过使用 getState、setState 和/或 compareAndSetState 检查和/或修改同步状态，重新定义以下方法（如适用）：

+ tryAcquire
+ tryRelease
+ tryAcquireShared
+ tryReleaseShared
+ isHeldExclusively

Each of these methods by default throws UnsupportedOperationException. Implementations of these methods must be internally thread-safe, and should in general be short and not block. Defining these methods is the only supported means of using this class. All other methods are declared final because they cannot be independently varied.

默认情况下，这些方法中的每一个都会引发 UnsupportedOperationException。这些方法的实现必须是内部线程安全的，并且通常应该是简短的，而不是阻塞的。定义这些方法是使用此类的唯一受支持的方法。所有其他方法都被宣布为最终方法，因为它们不能独立地改变。

You may also find the inherited methods from AbstractOwnableSynchronizer useful to keep track of the thread owning an exclusive synchronizer. You are encouraged to use them -- this enables monitoring and diagnostic tools to assist users in determining which threads hold locks.

您还可能发现从 AbstractOwnableSynchronizer 继承的方法对于跟踪拥有独占同步器的线程非常有用。我们鼓励您使用它们——这使监视和诊断工具能够帮助用户确定哪些线程持有锁。

Even though this class is based on an internal FIFO queue, it does not automatically enforce FIFO acquisition policies. The core of exclusive synchronization takes the form:

即使此类基于内部 FIFO 队列，它也不会自动强制执行 FIFO 采集策略。独占同步的核心采用以下形式：

```
    Acquire:
        while (!tryAcquire(arg)) {
            enqueue thread if it is not already queued;
            possibly block current thread;
        }

    Release:
        if (tryRelease(arg))
            unblock the first queued thread;

```
(Shared mode is similar but may involve cascading signals.)

Because checks in acquire are invoked before enqueuing, a newly acquiring thread may barge ahead of others that are blocked and queued. However, you can, if desired, define tryAcquire and/or tryAcquireShared to disable barging by internally invoking one or more of the inspection methods, thereby providing a fair FIFO acquisition order. In particular, most fair synchronizers can define tryAcquire to return false if hasQueuedPredecessors (a method specifically designed to be used by fair synchronizers) returns true. Other variations are possible.

由于 acquire 中的检查是在入队之前调用的，因此新的 acquiring 线程可能会抢先阻塞并排队的其他线程。但是，如果需要，您可以通过内部调用一种或多种检查方法来定义 tryAcquire 和/或tryAcquireShared 以禁用闯入，从而提供公平的 FIFO 获取命令。特别是，如果hasQueuedPredecessors（一种专门为公平同步器设计的方法）返回 true，大多数公平同步器可以定义tryAcquire 以返回 false。其他变化也是可能的。

Throughput and scalability are generally highest for the default barging (also known as greedy, renouncement, and convoy-avoidance) strategy. While this is not guaranteed to be fair or starvation-free, earlier queued threads are allowed to recontend before later queued threads, and each recontention has an unbiased chance to succeed against incoming threads. Also, while acquires do not "spin" in the usual sense, they may perform multiple invocations of tryAcquire interspersed with other computations before blocking. This gives most of the benefits of spins when exclusive synchronization is only briefly held, without most of the liabilities when it isn't. If so desired, you can augment this by preceding calls to acquire methods with "fast-path" checks, possibly prechecking hasContended and/or hasQueuedThreads to only do so if the synchronizer is likely not to be contended.

默认闯入（也称为贪婪、放弃和避免护航）策略的吞吐量和可伸缩性通常最高。虽然这不能保证公平或无饥饿，但允许较早排队的线程在稍后排队的线程之前重新调度，并且每个重新调度都有一个针对传入线程的无偏向的成功机会。此外，虽然获取不会在通常意义上“旋转”，但它们可能会在阻塞之前执行多个 tryAcquire 调用，并穿插其他计算。当独占同步仅短暂保持时，这提供了旋转的大部分好处，而当不保持独占同步时，则没有大部分责任。如果需要，您可以通过前面的调用来增强这一点，以获取具有“快速路径”检查的方法，可能只在同步器可能不会被争用的情况下，预先检查 HasContemped 和/或 hasQueuedThreads 以执行此操作。

该类为同步提供了一个高效且可扩展的基础，部分是通过将其使用范围专门化为可依赖于 int state、acquire 和 release 参数以及内部 FIFO 等待队列的同步器。当这还不够时，您可以使用原子类、您自己的自定义 java.util.Queue 类和 LockSupport 支持的阻塞从较低级别构建同步器。

#### 2.2 Usage Example
Here is a non-reentrant mutual exclusion lock class that uses the value zero to represent the unlocked state, and one to represent the locked state. While a non-reentrant lock does not strictly require recording of the current owner thread, this class does so anyway to make usage easier to monitor. It also supports conditions and exposes one of the instrumentation methods:

这是一个不可重入的互斥锁类，它使用值 0 表示解锁状态，使用值 1 表示锁定状态。虽然不可重入的锁并不严格要求记录当前所有者线程，但该类仍然这样做，以便更容易监视使用情况。它还支持条件并公开其中一种检测方法：

```
    class Mutex implements Lock, java.io.Serializable {
        
        // Our internal helper class
        private static class Sync extends AbstractQueuedSynchronizer {
            // Reports whether in locked state
            protected boolean isHeldExclusively() {
                return getState() == 1;
            }

            // Acquires the lock if state is zero
            public boolean tryAcquire(int acquires) {
                assert acquires == 1;   //Otherwise unused
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            // Releases the lock by setting state to zero
            protected boolean tryRelease(int releases) {
                assert releases == 1;   // Otherwise unused
                if (getState() == 0) throw new IllegalMonitorStateException();
                setExclusieOwnerThread(null);
                setState(0);
                return true;
            }

            // Provides a Condition
            Condition newCondition() { return new ConditionObject(); }

            // Deserializes properly
            private void readObject(ObjectInputStream s) 
                throws IOException, ClassNotFoundException {
                s.defaultReadObject();
                setState(0);    // reset to unlocked state
            }
        }

        // The sync object does all the hard work. We just forward to it.
        private final Sync sync = new Sync();

        public void lock()  { sync.acquire(1); }
        public boolean tryLock()    { return sync.tryAcquire(1); }
        public void unlock() { sync.release(1); }
        public Condition newCondition() { return sync.newCondition(); }
        public boolean isLocked()   { return sync.isHeldExclusively(); }
        public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        public boolean tryLock(long timeout, TimeUnit unit) 
            throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }
    }
```

Here is a latch class that is like a CountDownLatch except that it only requires a single signal to fire. Because a latch is non-exclusive, it uses the shared acquire and release methods.

这里有一个类似倒计时锁存器的锁存器类，只是它只需要一个信号就可以触发。因为闩锁是非独占的，所以它使用共享的获取和释放方法。

```
    class BooleanLatch {
        private static class Sync extends AbstractQueuedSychronizer {
            boolean isSignalled() { return getState() != 0; }

            protected int tryAcquireShared(int ignore) {
                return isSignalled() ? 1 : -1;
            }

            protected boolean tryReleaseShared(int ignore) {
                setState(1);
                return true;
            }
        }

        private final Sync sync = new Sync();
        public boolean isSignalled() { return sync.isSignalled(); }
        public void signal()    { sync.releaseShared(1); }
        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
    }
```

#### 2.3 AbstractQueuedSynchronizer.Node
Wait queue node class.

等待队列节点类。

The wait queue is a variant of a "CLH" (Craig, Landin, and Hagersten) lock queue. CLH locks are normally used for spinlocks. We instead use them for blocking synchronizers, but use the same basic tactic of holding some of the control information about a thread in the predecessor of its node. A "status" field in each node keeps track of whether a thread should block. A node is signalled when its predecessor releases. Each node of the queue otherwise serves as a specific-notification-style monitor holding a single waiting thread. The status field does NOT control whether threads are granted locks etc though. A thread may try to acquire if it is first in the queue. But being first does not guarantee success; it only gives the right to contend. So the currently released contender thread may need to rewait.

等待队列是“CLH”（Craig、Landin和Hagersten）锁队列的变体。CLH 锁通常用于旋转锁。相反，我们使用它们来阻止同步器，但使用相同的基本策略，即在其节点的前一个线程中保存一些关于该线程的控制信息。每个节点中的“状态”字段跟踪线程是否应该阻塞。节点在其前一个节点释放时发出信号。队列的每个节点都充当一个特定的通知样式监视器，其中包含一个等待线程。但是 status 字段不控制线程是否被授予锁等。如果线程是队列中的第一个线程，它可能会尝试获取。但第一并不能保证成功；它只给了我们竞争的权利。因此，当前发布的竞争者线程可能需要重新等待。

To enqueue into a CLH lock, you atomically splice it in as new tail. To dequeue, you just set the head field.

            +------+  prev +-----+       +-----+
       head |      | <---- |     | <---- |     |  tail
            +------+       +-----+       +-----+
       
Insertion into a CLH queue requires only a single atomic operation on "tail", so there is a simple atomic point of demarcation from unqueued to queued. Similarly, dequeuing involves only updating the "head". However, it takes a bit more work for nodes to determine who their successors are, in part to deal with possible cancellation due to timeouts and interrupts.

插入到 CLH 队列只需要在 “tail” 上执行单个原子操作，因此有一个简单的从 unqueued 到 queued 的原子划分点。类似地，出列只涉及更新“head”。然而，节点需要更多的工作来确定谁是他们的继任者，部分是为了处理由于超时和中断而可能取消的问题。

The "prev" links (not used in original CLH locks), are mainly needed to handle cancellation. If a node is cancelled, its successor is (normally) relinked to a non-cancelled predecessor. For explanation of similar mechanics in the case of spin locks, see the papers by Scott and Scherer at http://www.cs.rochester.edu/u/scott/synchronization/

“prev”链接（未在原始 CLH 锁中使用）主要用于处理取消。如果节点被取消，其后续节点（通常）将重新链接到未取消的前置节点。

We also use "next" links to implement blocking mechanics. The thread id for each node is kept in its own node, so a predecessor signals the next node to wake up by traversing next link to determine which thread it is. Determination of successor must avoid races with newly queued nodes to set the "next" fields of their predecessors. This is solved when necessary by checking backwards from the atomically updated "tail" when a node's successor appears to be null. (Or, said differently, the next-links are an optimization so that we don't usually need a backward scan.)

我们还使用 “next” 链接来实现阻塞机制。每个节点的线程 id 都保存在自己的节点中，因此前置节点通过遍历下一个链接来确定它是哪个线程，从而向下一个节点发出唤醒信号。确定继任者必须避免与新排队的节点竞争，以设置其前任节点的“next”字段。当节点的后续节点显示为空时，通过从原子更新的“tail”向后检查，在必要时可以解决此问题。（或者，换言之，next 链接是一个优化，因此我们通常不需要反向扫描。）

Cancellation introduces some conservatism to the basic algorithms. Since we must poll for cancellation of other nodes, we can miss noticing whether a cancelled node is ahead or behind us. This is dealt with by always unparking successors upon cancellation, allowing them to stabilize on a new predecessor, unless we can identify an uncancelled predecessor who will carry this responsibility.

取消为基本算法引入了一些保守性。因为我们必须轮询其他节点的取消，所以我们可能会忽略已取消的节点是在我们前面还是后面。这是通过在撤销时始终取消继承人资格来解决的，允许他们稳定在新的前任上，除非我们能够确定一个未被撤销的前任将承担这一责任。

CLH queues need a dummy header node to get started. But we don't create them on construction, because it would be wasted effort if there is never contention. Instead, the node is constructed and head and tail pointers are set upon first contention.

CLH 队列需要一个虚拟头节点来启动。但我们不会在构建时创建它们，因为如果不存在争用，这将是徒劳的。相反，在第一次争用时构造节点并设置头指针和尾指针。

Threads waiting on Conditions use the same nodes, but use an additional link. Conditions only need to link nodes in simple (non-concurrent) linked queues because they are only accessed when exclusively held. Upon await, a node is inserted into a condition queue. Upon signal, the node is transferred to the main queue. A special value of status field is used to mark which queue a node is on.

Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill Scherer and Michael Scott, along with members of JSR-166 expert group, for helpful ideas, discussions, and critiques on the design of this class.

等待条件的线程使用相同的节点，但使用额外的链接。条件只需要链接简单（非并发）链接队列中的节点，因为它们仅在独占持有时才被访问。等待时，将节点插入到条件队列中。收到信号后，节点被转移到主队列。状态字段的特殊值用于标记节点所在的队列。

感谢Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer和Michael Scott以及JSR-166专家组的成员对本课程的设计提出了有益的想法、讨论和评论。

### 3 ReentrantLock
A reentrant mutual exclusion Lock with the same basic behavior and semantics as the implicit monitor lock accessed using synchronized methods and statements, but with extended capabilities.

可重入互斥锁，其基本行为和语义与使用同步方法和语句访问的隐式监视器锁相同，但具有扩展功能。

A ReentrantLock is owned by the thread last successfully locking, but not yet unlocking it. A thread invoking lock will return, successfully acquiring the lock, when the lock is not owned by another thread. The method will return immediately if the current thread already owns the lock. This can be checked using methods isHeldByCurrentThread, and getHoldCount.

ReentrantLock 属于上次成功锁定但尚未解锁的线程。当锁不属于某一个线程时，调用 lock 方法的线程将返回并成功获取锁。如果当前线程已经拥有锁，则该方法将立即返回。这可以使用 isHeldByCurrentThread 和 getHoldCount 方法进行检查。

The constructor for this class accepts an optional fairness parameter. When set true, under contention, locks favor granting access to the longest-waiting thread. Otherwise this lock does not guarantee any particular access order. Programs using fair locks accessed by many threads may display lower overall throughput (i.e., are slower; often much slower) than those using the default setting, but have smaller variances in times to obtain locks and guarantee lack of starvation. Note however, that fairness of locks does not guarantee fairness of thread scheduling. Thus, one of many threads using a fair lock may obtain it multiple times in succession while other active threads are not progressing and not currently holding the lock. Also note that the untimed tryLock() method does not honor the fairness setting. It will succeed if the lock is available even if other threads are waiting.

此类的构造函数接受可选的公平性参数。当设置为 true 时，在争用下，锁有利于向等待时间最长的线程授予访问权限。否则，此锁不保证任何特定的访问顺序。与使用默认设置的程序相比，使用由多个线程访问的公平锁的程序可能显示较低的总体吞吐量（即较慢；通常较慢），但在获得锁和保证无饥饿的时间上差异较小。但是请注意，锁的公平性并不能保证线程调度的公平性。因此，使用公平锁的多个线程中的一个线程可能会连续多次获得公平锁，而其他活动线程则没有进行，并且当前没有持有该锁。还要注意，untimed tryLock（）方法不支持公平性设置。如果锁可用，即使其他线程正在等待，它也会成功。

It is recommended practice to always immediately follow a call to lock with a try block, most typically in a before/after construction such as:

```
    class X {
        private final ReentrantLock lock = new ReentrantLock();
        // ...

        public void m() {
            lock.lock();    // block until condition holds
            try {
                // ... method body
            } finally {
                lock.unlock();
            }
        }
    }
```

In addition to implementing the Lock interface, this class defines a number of public and protected methods for inspecting the state of the lock. Some of these methods are only useful for instrumentation and monitoring.

除了实现锁接口外，此类还定义了许多用于检查锁状态的公共和受保护方法。其中一些方法仅适用于仪器和监测。

Serialization of this class behaves in the same way as built-in locks: a deserialized lock is in the unlocked state, regardless of its state when serialized.

此类的序列化与内置锁的行为相同：反序列化的锁处于解锁状态，而不管序列化时的状态如何。

This lock supports a maximum of 2147483647 recursive locks by the same thread. Attempts to exceed this limit result in Error throws from locking methods.

此锁支持同一线程最多 2147483647 个递归锁。试图超过此限制将导致锁定方法引发错误。

### 4 ReentrantReadWriteLock
An implementation of ReadWriteLock supporting similar semantics to ReentrantLock.
This class has the following properties:

#### 4.1 Acquisition order
This class does not impose a reader or writer preference ordering for lock access. However, it does support an optional fairness policy.

该类不会为锁的访问强加一个读者或写者的偏好排序。然而，它确实支持一个可选的公平策略。

##### 4.1.1 Non-fair mode (default)
When constructed as non-fair (the default), the order of entry to the read and write lock is unspecified, subject to reentrancy constraints. A nonfair lock that is continuously contended may indefinitely postpone one or more reader or writer threads, but will normally have higher throughput than a fair lock.

当构建为非公平（默认）时，受重入限制，进入读和写锁的顺序是未指定的。一个持续争夺的非公平锁可能会无限期地推迟一个或多个读者或写者线程，但通常会比公平锁有更高的吞吐量。

##### 4.1.2 Fair mode
When constructed as fair, threads contend for entry using an approximately arrival-order policy. When the currently held lock is released, either the longest-waiting single writer thread will be assigned the write lock, or if there is a group of reader threads waiting longer than all waiting writer threads, that group will be assigned the read lock.

当构建为公平时，线程使用近似到达顺序的策略争夺进入。当当前持有的锁被释放时，等待时间最长的单个写者线程将被分配到写锁，或者如果有一组读者线程的等待时间长于所有等待的写者线程，该组线程将被分配到读锁。

A thread that tries to acquire a fair read lock (non-reentrantly) will block if either the write lock is held, or there is a waiting writer thread. The thread will not acquire the read lock until after the oldest currently waiting writer thread has acquired and released the write lock. Of course, if a waiting writer abandons its wait, leaving one or more reader threads as the longest waiters in the queue with the write lock free, then those readers will be assigned the read lock.

如果一个线程试图获取一个公平的读锁（非重复性的），那么如果写锁被持有，或者有一个正在等待的写者线程，那么这个线程将被阻塞。该线程将不会获得读锁，直到当前等待的最古老的写者线程获得并释放了写锁之后。当然，如果一个等待中的写者放弃了它的等待，留下一个或多个读者线程作为队列中最长的等待者，并且写锁空闲，那么这些读者将被分配到读锁。

A thread that tries to acquire a fair write lock (non-reentrantly) will block unless both the read lock and write lock are free (which implies there are no waiting threads). (Note that the non-blocking ReentrantReadWriteLock.ReadLock.tryLock() and ReentrantReadWriteLock.WriteLock.tryLock() methods do not honor this fair setting and will immediately acquire the lock if it is possible, regardless of waiting threads.)

一个试图获取公平的写锁的线程（非可逆的）将会阻塞，除非读锁和写锁都是自由的（这意味着没有等待的线程）。(注意，非阻塞的 ReentrantReadWriteLock.ReadLock.tryLock() 和ReentrantReadWriteLock.WriteLock.tryLock() 方法不尊重这个公平设置，如果有可能的话，会立即获取锁，而不管是否有等待的线程。)

#### 4.2 Reentrancy
This lock allows both readers and writers to reacquire read or write locks in the style of a ReentrantLock. Non-reentrant readers are not allowed until all write locks held by the writing thread have been released.

这个锁允许读者和写者以 ReentrantLock 的方式重新获得读或写锁。在写线程持有的所有写锁被释放之前，不允许非重入的读者。

Additionally, a writer can acquire the read lock, but not vice-versa. Among other applications, reentrancy can be useful when write locks are held during calls or callbacks to methods that perform reads under read locks. If a reader tries to acquire the write lock it will never succeed.

此外，写者可以获得读锁，但反之则不行。在其他应用中，当写锁在调用或回调到在读锁下执行读的方法时，重入是有用的。如果一个读者试图获取写锁，它将永远不会成功。

#### 4.3 Lock downgrading
Reentrancy also allows downgrading from the write lock to a read lock, by acquiring the write lock, then the read lock and then releasing the write lock. However, upgrading from a read lock to the write lock is not possible.

可重入性也允许从写锁降级到读锁，方法是先获得写锁，再获得读锁，然后释放写锁。然而，从读锁升级到写锁是不可能的。

#### 4.4 Interruption of lock acquisition
The read lock and write lock both support interruption during lock acquisition.

读取锁和写入锁都支持在获取锁时中断。

#### 4.5 Condition support
The write lock provides a Condition implementation that behaves in the same way, with respect to the write lock, as the Condition implementation provided by ReentrantLock.newCondition does for ReentrantLock. This Condition can, of course, only be used with the write lock.

写锁提供了一个 Condition 的实现，其行为方式与 ReentrantLock.newCondition 为 ReentrantLock 提供的 Condition 的行为方式相同。当然，这个 Condition 只能在写锁中使用。

The read lock does not support a Condition and readLock().newCondition() throws UnsupportedOperationException.

读锁不支持 Condition，readLock().newCondition() 抛出 UnsupportedOperationException。

#### 4.6 Instrumentation
This class supports methods to determine whether locks are held or contended. These methods are designed for monitoring system state, not for synchronization control.

该类支持确定锁是否被持有或争夺的方法。这些方法是为监控系统状态而设计的，而不是为了同步控制。

#### 4.7 Sample usages
Here is a code sketch showing how to perform lock downgrading after updating a cache (exception handling is particularly tricky when handling multiple locks in a non-nested fashion):

下面是一个代码草图，展示了如何在更新缓存后执行锁的降级（当以非嵌套方式处理多个锁时，异常处理特别棘手）。

```
    class CachedData {
        Object data;
        volatile boolean cacheValid;
        final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

        void processCachedData() {
            rwl.readLock().lock();
            if (!cacheValid) {
                // Must release read lock before acquiring write lock
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                try {
                    // Recheck state because another thread might have
                    // acquired write lock and changed state before we did.
                    if (!cacheValid) {
                        data = ...
                        cacheValid = true;
                    }
                    // Downgrade by acquiring read lock before releasing 
                    // write lock
                    rwl.readLock().lock();
                } finally {
                    rwl.writeLock().unlock(); // unlock write, still hold read
                }
            }
            try {
                use(data);
            } finally {
                rwl.readLock().unlock();
            }
        }
    }
```

ReentrantReadWriteLocks can be used to improve concurrency in some uses of some kinds of Collections. This is typically worthwhile only when the collections are expected to be large, accessed by more reader threads than writer threads, and entail operations with overhead that outweighs synchronization overhead. For example, here is a class using a TreeMap that is expected to be large and concurrently accessed.

在某些类型的集合的使用中，可以使用可重入的读写锁来提高并发性。一般来说，只有当集合的规模较大，被更多的读者线程访问，并且需要进行超过同步开销的操作时，这才是值得的。例如，这里有一个使用TreeMap的类，预计会有大量的并发访问。

```
    class RWDictionary {
        private final Map<String, Data> m = new TreeMap<String, Data>();
        private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        private final Lock r = rwl.readLock();
        private final Lock w = rwl.writeLock();

        public Data get(String key) {
            r.lock();
            try { return m.get(key); }
            finally { r.unlock(); }
        }

        public String[] allKeys() {
            r.lock();
            try { return m.keySet().toArray(); }
            finally { r.unlock(); }
        }

        public Data put(String key, Data value) {
            w.lock();
            try { return m.put(key, value); }
            finally { w.unlock(); }
        }

        public void clear() {
            w.lock();
            try { m.clear(); }
            finally { w.unlock(); }
        }
    }
```

This lock supports a maximum of 65535 recursive write locks and 65535 read locks. Attempts to exceed these limits result in Error throws from locking methods.

这个锁最多支持65535个递归写锁和65535个读锁。试图超过这些限制会导致锁的方法抛出错误。

### 5 Semaphore
A counting semaphore. Conceptually, a semaphore maintains a set of permits. Each acquire blocks if necessary until a permit is available, and then takes it. Each release adds a permit, potentially releasing a blocking acquirer. However, no actual permit objects are used; the Semaphore just keeps a count of the number available and acts accordingly.

一个计数信号器。从概念上讲，一个 semaphore 维护一组许可。如果有必要的话，每个获取者都会进行阻塞，直到有一个许可可用，然后就会获取它。每次释放都会增加一个许可，可能会释放一个阻塞的获取者。然而，没有使用实际的许可对象；Semaphore 只是对可用的数量进行计数，并采取相应的行动。

Semaphores are often used to restrict the number of threads than can access some (physical or logical) resource. For example, here is a class that uses a semaphore to control access to a pool of items:

Semaphores 经常被用来限制可以访问某些（物理或逻辑）资源的线程的数量。例如，这里有一个使用semaphore 来控制对一个项目池的访问的类。

```
    class Pool {
        private static final int MAX_AVAILABLE = 100;
        private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

        public Object getItem() throws InterruptedException {
            available.acquire();
            return getNextAvailableItem();
        }

        public void putItem(Object x) {
            if (markAsUnused(x))
                available.release();
        }

        // Not a particularly efficient data structure; just for demo

        protected Object[] items = ... whatever kinds of items being managed
        protected boolean[] used = new boolean[MAX_AVAILABLE];

        protected synchronized Object getNextAvailableItem() {
            for (int i = 0; i < MAX_AVAILABLE; ++i) {
                if (!used[i]) {
                    used[i] = true;
                    return items[i];
                }
            }
            return null;    // not reached
        }

        protected synchronized boolean markAsUnused(Object item) {
            for (int i = 0; i < MAX_AVAILABLE; ++i) {
                if (item == items[i]) {
                    if (used[i]) {
                        used[i] = false;
                        return true;
                    } else 
                        return false;
                }
            }
            return false;
        }
    }
```

Before obtaining an item each thread must acquire a permit from the semaphore, guaranteeing that an item is available for use. When the thread has finished with the item it is returned back to the pool and a permit is returned to the semaphore, allowing another thread to acquire that item. Note that no synchronization lock is held when acquire is called as that would prevent an item from being returned to the pool. The semaphore encapsulates the synchronization needed to restrict access to the pool, separately from any synchronization needed to maintain the consistency of the pool itself.

在获得一个项目之前，每个线程都必须从 semaphore 那里获得一个许可，以保证一个项目可以被使用。当线程使用完该项目后，它将被送回池中，并将许可返回给 semaphore，允许其他线程获取该项目。请注意，在调用 acquisition 的时候，没有同步锁，因为那会阻止一个项目返回到池中。semaphore 封装了限制对池子的访问所需的同步，与维护池子本身的一致性所需的同步分开。

A semaphore initialized to one, and which is used such that it only has at most one permit available, can serve as a mutual exclusion lock. This is more commonly known as a binary semaphore, because it only has two states: one permit available, or zero permits available. When used in this way, the binary semaphore has the property (unlike many java.util.concurrent.locks.Lock implementations), that the "lock" can be released by a thread other than the owner (as semaphores have no notion of ownership). This can be useful in some specialized contexts, such as deadlock recovery.

一个初始化为1的信号灯，在使用时，它最多只有一个可用的许可，可以作为一个互斥锁。这就是通常所说的二元信号，因为它只有两种状态：一种是可用的许可，另一种是可用的零许可。当以这种方式使用时，二元信号量有一个属性（与许多 java.util.concurrent.locks.Lock 实现不同），即该 "锁 "可以由所有者以外的线程释放（因为信号灯没有所有权的概念）。这在一些特殊情况下很有用，比如死锁恢复。

The constructor for this class optionally accepts a fairness parameter. When set false, this class makes no guarantees about the order in which threads acquire permits. In particular, barging is permitted, that is, a thread invoking acquire can be allocated a permit ahead of a thread that has been waiting - logically the new thread places itself at the head of the queue of waiting threads. When fairness is set true, the semaphore guarantees that threads invoking any of the acquire methods are selected to obtain permits in the order in which their invocation of those methods was processed (first-in-first-out; FIFO). Note that FIFO ordering necessarily applies to specific internal points of execution within these methods. So, it is possible for one thread to invoke acquire before another, but reach the ordering point after the other, and similarly upon return from the method. Also note that the untimed tryAcquire methods do not honor the fairness setting, but will take any permits that are available.

这个类的构造函数可以选择接受一个公平性参数。当设置为 false 时，该类对线程获取许可的顺序不做任何保证。特别是，我们允许 "闯入"，也就是说，一个调用 acquisition 的线程可以在一个一直在等待的线程之前被分配一个许可--从逻辑上讲，新线程将自己放在等待线程队列的首位。当公平性被设置为 "true "时，semaphore 保证调用任何一个获取方法的线程会按照他们调用这些方法的处理顺序来选择获取许可（先进先出；FIFO）。请注意，FIFO 排序必然适用于这些方法中的特定内部执行点。因此，一个线程有可能在另一个线程之前调用获取，但在另一个线程之后到达排序点，在从方法返回时也是如此。还要注意的是，不计时的tryAcquire 方法不尊重公平性设置，但会采取任何可用的许可。

Generally, semaphores used to control resource access should be initialized as fair, to ensure that no thread is starved out from accessing a resource. When using semaphores for other kinds of synchronization control, the throughput advantages of non-fair ordering often outweigh fairness considerations.

一般来说，用于控制资源访问的信号应该被初始化为公平的，以确保没有线程在访问资源的时候被饿死。当使用信号灯进行其他类型的同步控制时，非公平排序的吞吐量优势往往超过了公平性的考虑。

This class also provides convenience methods to acquire and release multiple permits at a time. Beware of the increased risk of indefinite postponement when these methods are used without fairness set true.

这个类还提供了方便的方法，以便一次获得和释放多个许可证。当这些方法在没有公平性设置为 "true "的情况下被使用时，要注意无限期推迟的风险增加。

Memory consistency effects: Actions in a thread prior to calling a "release" method such as release() happen-before actions following a successful "acquire" method such as acquire() in another thread.

内存一致性效应。一个线程在调用 "释放 "方法（如release()）之前的操作，会发生在另一个线程的 "获取 "方法（如 acquire()）成功之后的操作之前。

### 6 CountDownLatch
A synchronization aid that allows one or more threads to wait until a set of operations being performed in other threads completes.

一种同步辅助工具，允许一个或多个线程等待，直到其他线程中正在执行的一组操作完成。

A CountDownLatch is initialized with a given count. The await methods block until the current count reaches zero due to invocations of the countDown method, after which all waiting threads are released and any subsequent invocations of await return immediately. This is a one-shot phenomenon -- the count cannot be reset. If you need a version that resets the count, consider using a CyclicBarrier.

一个 CountDownLatch 被初始化为一个给定的计数。await 方法被阻塞，直到当前的计数由于 countDown方法的调用而达到零，之后所有等待的线程被释放，任何后续的 await 调用都会立即返回。这是一个一次性的现象--计数不能被重置。如果你需要一个重置计数的版本，可以考虑使用 CyclicBarrier。

A CountDownLatch is a versatile synchronization tool and can be used for a number of purposes. A CountDownLatch initialized with a count of one serves as a simple on/off latch, or gate: all threads invoking await wait at the gate until it is opened by a thread invoking countDown. A CountDownLatch initialized to N can be used to make one thread wait until N threads have completed some action, or some action has been completed N times.

CountDownLatch 是一个多功能的同步工具，可用于多种用途。一个初始化为 1 的 CountDownLatch 可以作为一个简单的开/关锁或门：所有调用 await 的线程都在门前等待，直到调用 countDown 的线程打开它。一个初始化为 N 的 CountDownLatch 可以用来让一个线程等待，直到 N 个线程完成某个动作，或者某个动作已经完成了 N 次。

A useful property of a CountDownLatch is that it doesn't require that threads calling countDown wait for the count to reach zero before proceeding, it simply prevents any thread from proceeding past an await until all threads could pass.

CountDownLatch 的一个有用的属性是，它不要求调用 countDown 的线程在继续进行之前等待计数为零，它只是防止任何线程在所有线程都能通过之前继续进行等待。

#### 6.1 Sample usage:
Here is a pair of classes in which a group of worker threads use two countdown latches:
+ The first is a start signal that prevents any worker from proceeding until the driver is ready for them to proceed;
+ The second is a completion signal that allows the driver to wait until all workers have completed.

这里有一对类，其中一组工作线程使用两个倒计时锁存器。
+ 首先是一个启动信号，防止任何 worker 继续前进，直到 Driver 准备好让他们继续前进。
+ 第二个是一个完成信号，它允许驱动者等待，直到所有的工作者都完成。

```
    class Driver {  // ...
        void main() throws InterruptedException {
            CountDownLatch startSignal = new CountDownLatch(1);
            CountDownLatch doneSignal = new CountDownLatch(N);

            for (int i = 0; i < N; ++i)     // create and start threads
                new Thread(new Worker(startSignal, doneSignal)).start();
            
            doSomethingElse();              // don't let run yet
            startSignal.countDown();        // let all threads proceed
            doSomethingElse();
            doneSignal.await();             // wait for all to finish
        }
    }

    class Worker implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;
        Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void run() {
            try {
                startSignal.await();
                doWork();
                doneSignal.countDown();
            } catch (InterruptedException ex) {}    // return;
        }

        void doWork() { ... }
    }

```

Another typical usage would be to divide a problem into N parts, describe each part with a Runnable that executes that portion and counts down on the latch, and queue all the Runnables to an Executor. When all sub-parts are complete, the coordinating thread will be able to pass through await. (When threads must repeatedly count down in this way, instead use a CyclicBarrier.)

另一个典型的用法是将一个问题分为 N 个部分，用一个 Runnable 来描述每个部分，执行该部分并在锁存器上进行倒计时，并将所有的 Runnable 排到一个 Executor。当所有的子部分都完成后，协调线程就可以通过等待。(当线程必须以这种方式反复倒计时时，可以用CyclicBarrier代替。)

```
    class Driver2 {     // ...
        void main() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(N);
            Executor e = ...

            for (int i = 0; i < N; ++i)     // create and start threads
                e.execute(new WorkerRunnable(doneSignal, i));

            doneSignal.await();             // wait for all to finish
        }
    }

    class WorkerRunnable implements Runnable {
        private final CountDownLatch doneSignal;
        private final int i;
        WorkerRunnable(CountDownLatch doneSignal, int i) {
            this.doneSignal = doneSignal;
            this.i = i;
        }

        public void run() {
            try {
                doWork(i);
                doneSignal.countDown();
            } catch (InterruptedException ex) {}    // return;
        }

        void doWork() { ... }
    }

```

Memory consistency effects: Until the count reaches zero, actions in a thread prior to calling countDown() happen-before actions following a successful return from a corresponding await() in another thread.

内存一致性影响。在计数达到零之前，一个线程在调用 countDown() 之前的动作会发生在另一个线程的相应await() 成功返回之后的动作之前。


### 7 CyclicBarrier
A synchronization aid that allows a set of threads to all wait for each other to reach a common barrier point. CyclicBarriers are useful in programs involving a fixed sized party of threads that must occasionally wait for each other. The barrier is called cyclic because it can be re-used after the waiting threads are released.

一种同步辅助工具，允许一组线程互相等待，以达到一个共同的障碍点。CyclicBarriers 在涉及固定规模的线程组的程序中非常有用，这些线程必须偶尔相互等待。这个屏障被称为循环的，因为它在等待的线程被释放后可以被重新使用。

A CyclicBarrier supports an optional Runnable command that is run once per barrier point, after the last thread in the party arrives, but before any threads are released. This barrier action is useful for updating shared-state before any of the parties continue.

CyclicBarrier 支持一个可选的 Runnable 命令，该命令在每个障碍点运行一次，在聚会的最后一个线程到达后，但在任何线程被释放前。这个屏障动作对于在任何一方继续之前更新共享状态很有用。

Sample usage: Here is an example of using a barrier in a parallel decomposition design:

#### 7.1 Sample Usage
下面是一个在并行分解设计中使用屏障的例子。

```
    class Solver {
        final int N;
        final float[][] data;
        final CyclicBarrier barrier;

        class Worker implements Runnable {
            int myRow;
            Worker(int row) {
                myRow = row;
            }

            public void run() {
                while (!done()) {
                    processRow(myRow);

                    try {
                        barrier.await();
                    } catch(InterruptedException e) {
                        return;
                    } catch(BrokenBarrierException e) {
                        return;
                    }
                }
            }
        }

        public Solver(float[][] matrix) {
            data = matrix;
            N = matrix.length;
            Runnable barrierAction = new Runnable() {
                public void run() { mergeRows(...); }
            }
            barrier = new CyclicBarrier(N, barrierAction);

            List<Thread> thread = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                Thread thread = new Thread(new Worker(i));
                threads.add(thread);
                thread.start();
            }

            for (Thread thread: threads) {
                thread.join();
            }
        }
    }

```

Here, each worker thread processes a row of the matrix then waits at the barrier until all rows have been processed. When all rows are processed the supplied Runnable barrier action is executed and merges the rows. If the merger determines that a solution has been found then done() will return true and each worker will terminate.

在这里，每个工作线程处理一行矩阵，然后在屏障处等待，直到所有行都处理完毕。处理完所有行后，将执行提供的可运行屏障操作并合并这些行。如果合并确定已找到解决方案，则 done（）将返回true，并且每个worker 将终止。

If the barrier action does not rely on the parties being suspended when it is executed, then any of the threads in the party could execute that action when it is released. To facilitate this, each invocation of await returns the arrival index of that thread at the barrier. You can then choose which thread should execute the barrier action, for example:

如果屏障操作在执行时不依赖于被暂停的当事方，则当该操作被释放时，当事方中的任何线程都可以执行该操作。为了便于实现这一点，每次调用 wait 都会返回该线程在屏障处的到达索引。然后，您可以选择哪个线程应执行屏障操作，例如：

```
    if (barrier.await() == 0) {
        // log the completion of this iteration
    }
```

The CyclicBarrier uses an all-or-none breakage model for failed synchronization attempts: If a thread leaves a barrier point prematurely because of interruption, failure, or timeout, all other threads waiting at that barrier point will also leave abnormally via BrokenBarrierException (or InterruptedException if they too were interrupted at about the same time).

CyclicBarrier 对失败的同步尝试使用“全部”或“无”中断模型：如果线程由于中断、故障或超时而过早离开一个障碍点，在该屏障点等待的所有其他线程也将通过 BrokenBarrierException（或InterruptedException，如果它们也在大约同一时间被中断）异常离开。

Memory consistency effects: Actions in a thread prior to calling await() happen-before actions that are part of the barrier action, which in turn happen-before actions following a successful return from the corresponding await() in other threads.

内存一致性影响：调用 await（）之前线程中的操作发生在作为 barrier 操作一部分的操作之前，而barrier 操作又发生在其他线程中相应的 await（）成功返回之后的操作之前。

#### 7.2 CyclicBarrier.Generation
Each use of the barrier is represented as a generation instance. The generation changes whenever the barrier is tripped, or is reset. There can be many generations associated with threads using the barrier - due to the non-deterministic way the lock may be allocated to waiting threads - but only one of these can be active at a time (the one to which count applies) and all the rest are either broken or tripped. There need not be an active generation if there has been a break but no subsequent reset.

屏障的每次使用都被表示为一个生成实例。每当屏障被触发或被重置时，代数就会改变。由于锁可能被分配给等待的线程的非决定性方式，使用屏障的线程可能有很多代，但每次只有一个是活动的（适用于计数的那个），其余的要么被打破，要么被跳过。如果有一个断裂但没有随后的重置，就不需要有一个活跃的生成。