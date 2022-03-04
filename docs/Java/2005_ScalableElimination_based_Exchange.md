# A Scalable Elimination-based Exchange Channel
> William N. Scherer IIIDept. of Computer Science
> Doug LeaComputer Science Dept.
> Michael L. ScottDept. of Computer Science

## ABSTRACT
We present a new nonblocking implementation of the exchange channel, a concurrent data structure in which 2N participants form N pairs and each participant exchanges data with its partner. Our new implementation combines techniques from our previous work in dual stacks and from the elimination-based stack of Hendler et al. to yield very high concurrency.

我们提出了一个新的交换通道的非阻塞实现，这是一个并发的数据结构，其中 2N 个参与者组成 N 对，每个参与者与它的伙伴交换数据。我们的新实现结合了我们以前在双栈中的工作和 Hendler 等人的基于消除的栈中的技术，产生了非常高的并发性。

We assess the performance of our exchange channel using experimental results from a 16-processor SunFire 6800. We compare our implementation to that of the Java SE 5.0 class java.util.concurrent.Exchanger using both a synthetic microbench-mark and a real-world application that finds an approximate solution to the traveling salesman problem using genetic recombination. Our algorithm outperforms the Java SE 5.0 Exchanger in the microbenchmark by a factor of two at two threads up to a factor of 50 at 10; similarly, it outperforms the Java SE 5.0 Exchanger by a factor of five in the traveling salesman problem at ten threads. Our exchanger has been adopted for inclusion in Java 6.

我们使用 16 个处理器的 SunFire 6800 的实验结果来评估我们的交换通道的性能。我们用一个合成的微观基准和一个现实世界的应用来比较我们的实现和 Java SE 5.0 的 java.util.concurrent.Exchanger 类的实现，该应用使用遗传重组来寻找旅行销售员问题的近似解决方案。在微测试中，我们的算法比 Java SE 5.0 Exchanger 的性能要好，在 2 个线程时，我们的算法比 Java SE 5.0 Exchanger 的性能要好2 倍，在 10 个线程时，我们的算法比 Java SE 5.0 Exchanger 的性能要好 50 倍。我们的交换器已被纳入 Java 6 中。

## 1. INTRODUCTION
The problem of exchange channels (sometimes known as rendezvous channels) arises in a variety of concurrent programs. In it, a thread ta with datum da that enters the channel pairs up with another thread tb (with datum db) and exchanges data such that ta returns with db and tb returns with da. More generally, 2N threads form N pairs 〈ta1, tb1〉, 〈ta2, tb2 〉, 〈ta3, tb3〉, ..., 〈taN , tbN 〉 and exchange data pairwise.

交换通道（有时被称为交会通道）的问题出现在各种并发程序中。在其中，一个拥有数据量 da 的线程 ta 进入通道后与另一个线程 tb（拥有数据量 db ）配对，并交换数据，使得 ta 以 db 返回，tb 以 da 返回。更一般地说，2N 个线程组成 N 个对 〈ta1, tb1〉, 〈ta2, tb2 〉, 〈ta3, tb3〉, ..., 〈taN , tbN 〉并成对地交换数据。

In the basic exchange problem, if no partner is available immediately, thread ta waits until one becomes available. In the abortable exchange problem, however, ta specifies a patience pa that represents a maximum length of time it is willing to wait for a partner to appear; if no partner appears within pa μseconds, ta returns emptyhanded. Caution must be applied in implementations to ensure that a thread tb that sees ta just as it “gives up” and returns failure must not return da: Exchange must be bilateral.

在基本的交换问题中，如果没有伙伴立即出现，线程 ta 会等待，直到有伙伴出现。然而，在可中止交换问题中，ta 指定了一个代表它愿意等待伙伴出现的最大时间长度的耐心 pa；如果在 pa μseconds 内没有伙伴出现，ta 将空手返回。在实现中必须谨慎行事，以确保线程 tb 在看到 ta 时正好 "放弃 "并返回失败，但不能返回 da。交换必须是双边的。

Exchange channels are frequently used in parallel simulations. For example, the Promela modeling language for the SPIN model checker [8] uses rendezvous channels to simulate interprocess communication channels. Another typical use is in operating systems and server software. In a system with one producer and one consumer, the producer might work to fill a buffer with data, then exchange it with the buffer-draining consumer. This simultaneously bounds memory allocation for buffers and throttles the producer to generate data no faster than the consumer can process it.

交换通道经常被用于并行模拟中。例如，SPIN 模型检查器的 Promela 建模语言[8]使用交会通道来模拟进程间通信通道。另一个典型的用途是在操作系统和服务器软件中。在一个有一个生产者和一个消费者的系统中，生产者可能致力于用数据填满一个缓冲区，然后与消耗缓冲区的消费者交换。这同时限制了缓冲区的内存分配，并使生产者产生数据的速度不超过消费者的处理速度。

## 2. BACKGROUND

### 2.1 Nonblocking Synchronization
Linearizability [5] has become the standard technique for demonstrating that a concurrent implementation of an object is correct. Informally, it “provides the illusion that each operation. . . takes effect instantaneously at some point between its invocation and its response” [5, abstract]. Linearizability is nonblocking in that it never requires a call to a total method (one whose precondition is simply true) to wait for the execution of any other method. The fact that it is nonblocking makes linearizability particularly attractive for reasoning about nonblocking implementations of concurrent objects, which provide guarantees of various strength regarding the progress of method calls in practice. In a wait-free implementation, every contending thread is guaranteed to complete its method call within a bounded number of its own time steps [6]. In a lock-free implementation, some contending thread is guaranteed to complete its method call within a bounded number of steps (from any thread’s point of view) [6]. In an obstruction-free implementation, a thread is guaranteed to complete its method call within a bounded number of steps in the absence of contention, i.e. if no other threads execute competing methods concurrently [4].

可线性化[5]已经成为证明一个对象的并发实现是正确的标准技术。非正式地说，它 "提供了一种错觉，即每个操作. ...在其调用和响应之间的某个点上瞬间生效" [5, abstract]。Linearizability 是无阻塞的，因为它从不要求调用一个完全的方法（其前提条件是真的）来等待任何其他方法的执行。它是无阻塞的这一事实使得线性化对于推理并发对象的无阻塞实现特别有吸引力，它在实践中对方法调用的进度提供了不同强度的保证。在无等待的实现中，每个竞争的线程都被保证在其自身时间步数的一定范围内完成其方法调用[6]。在无锁实现中，一些竞争线程被保证在一定数量的步骤内（从任何线程的角度）完成其方法调用[6]。在无障碍实现中，在没有竞争的情况下，即没有其他线程同时执行竞争方法的情况下，一个线程被保证在一定的步数内完成其方法调用[4]。

### 2.2 Dual Data Structures
In traditional nonblocking implementations of concurrent objects, every method is total: It has no preconditions that must be satisfied before it can complete. Operations that might normally block before completing, such as dequeuing from an empty queue, are generally totalized to simply return a failure code in the case that their preconditions are not met. Then, calling the totalized method in a loop until it succeeds allows one to simulate the partial operation.

在传统的并发对象的非阻塞实现中，每个方法都是全能的：它在完成之前没有必须满足的前提条件。通常在完成之前可能会阻塞的操作，如从空队列中去排队，通常会被总体化，在不满足前提条件的情况下简单地返回一个失败代码。然后，在一个循环中调用累计方法，直到它成功，就可以模拟部分操作。

But this doesn’t necessarily respect our intuition for the semantics of an object! For example, consider the following sequence of events for threads A, B, C, and D:

但这并不一定尊重我们对对象语义的直觉 例如，考虑以下线程 A、B、C 和 D 的事件序列。

```
    A calls dequeue
    B calls dequeue
    C enqueues a 1
    D enqueues a 2
    B’s call returns the 1
    A’s call returns the 2
```

If thread A’s call to dequeue is known to have started before thread B’s call, then intuitively, we would think that A should get the first result out of the queue. Yet, with the call-in-a-loop idiom, ordering is simply a function of which thread happens to try a totalized dequeue operation first once data becomes available. Further, each invocation of the totalized method introduces performance-sapping contention for memory–interconnect band-width on the data structure. Finally, note that the mutual-swap semantics of an exchange channel do not readily admit a totalized implementation: If one simply fails when a partner is not available at the exact moment one enters the channel, the rate of successful rendezvous connections will be very low.

如果已知线程 A 对 dequeue 的调用是在线程 B 的调用之前开始的，那么直觉上，我们会认为 A 应该从队列中获得第一个结果。然而，在 call-in-a-loop 习性下，排序只是一个函数，即一旦数据可用，哪个线程碰巧先尝试累计出队操作。此外，每次调用累计方法都会对数据结构的内存互连带宽产生破坏性能的争夺。最后，请注意，交换通道的相互交换语义并不容易接受总体化实现。如果一个人在进入通道的确切时刻没有伙伴，就会简单地失败，那么成功的会合连接率将非常低。

As an alternative, suppose we could register a request for a partner in the channel. Inserting this reservation could be done in a nonblocking manner, and checking to see whether someone has come along to fulfill our reservation could consist of checking a boolean flag in the data structure representing the request. Even if the overall exchange operation requires blocking, it can be divided into two logical halves: the pre-blocking reservation and the post-blocking fulfillment.

作为一种选择，假设我们可以在通道中注册一个合作伙伴的请求。插入这个保留可以以非阻塞的方式进行，而检查是否有人来履行我们的保留可以包括检查代表请求的数据结构中的布尔标志。即使整个交换操作需要阻塞，它也可以被分为两个逻辑部分：阻塞前的保留和阻塞后的履行。

In our earlier work [10], we define a dual data structure to be one that may hold reservations (registered requests) instead of, or in addition to, data. A nonblocking dual data structure is one in which (a) every operation either completes or registers a request in non-blocking fashion; (b) fulfilled requests complete in non–blocking fashion; and (c) threads that are waiting for their requests to be fulfilled do not interfere with the progress of other threads. In a lock-free dual data structure, then, every operation either completes or registers a request in a lock-free manner and fulfilled requests complete in a lock-free manner.

在我们早期的工作[10]中，我们将双数据结构定义为可以持有保留（注册请求）而不是数据，或者除了数据之外，还可以持有保留。一个无锁的双数据结构是这样的：（a）每个操作都以非阻塞的方式完成或注册一个请求；（b）已完成的请求以非阻塞的方式完成；以及（c）正在等待其请求完成的线程不会干扰其他线程的进度。那么，在一个无锁的双重数据结构中，每个操作都是以无锁的方式完成或注册一个请求，已满足的请求也是以无锁的方式完成。

For a more formal treatment of linearizability for dual data structures, and for practical examples of dual stacks and queues, we refer the reader to our earlier work [10].

对于双重数据结构的线性化的更正式处理，以及双重堆栈和队列的实际例子，我们请读者参考我们的早期工作[10]。

### 2.3 Elimination
Elimination is a technique introduced by Shavit and Touitou [11] that improves the concurrency of data structures. It exploits the observation, for example, that one Push and one Pop, when applied with no intermediate operations to a stack data structure, yield a state identical to that from before the operations. Intuitively, then, if one could pair up Push and Pop operations, there would be no need to reference the stack data structure; they could “cancel each other out”. Elimination thus reduces contention on the main data structure and allows parallel completion of operations that would otherwise require accessing a common central memory location.

消除是由 Shavit 和 Touitou[11] 引入的一项技术，它改善了数据结构的并发性。它利用了这样的观察，例如，当对一个堆栈数据结构应用一个 Push 和一个 Pop 操作时，没有中间操作，产生的状态与操作之前的状态相同。直观地说，如果我们能把 Push 和 Pop 操作配对起来，就不需要引用堆栈数据结构；它们可以"相互抵消"。因此，消除可以减少对主数据结构的争夺，并允许平行地完成那些需要访问一个共同的中央存储器位置的操作。

More formally, one may define linearization points for mutually-canceling elimination operations in a manner such that no other linearization points intervene between them; since the operations effect (collectively) no change to the base data structure state, the history of operations – and its correctness – is equivalent to one in which the two operations never happened.

更正式地说，我们可以为相互抵消的消除操作定义线性化点，使它们之间没有其他线性化点介入；由于这些操作对基础数据结构状态（共同）没有影响，操作的历史--以及它的正确性--等同于这两个操作从未发生。

Although the original eliminating stack of Shavit and Touitou [11] is not linearizable [5], follow-up work by Hendler et al. [3] details one that is. Elimination has also been used for shared counters [1] and even for FIFO queues [9].

尽管 Shavit 和 Touitou[11] 的原始消除堆栈是不可线性化的[5]，Hendler 等人[3]的后续工作详细说明了一个可线性化的堆栈。消除法也被用于共享计数器[1]，甚至用于 FIFO 队列[9]。

## 3. ALGORITHM DESCRIPTION
Our exchanger uses a novel combination of nonblocking dual data structures and elimination arrays to achieve high levels of concurrency. The implementation is originally based on a combination of our dual stack [10] and the eliminating stack of Hendler et al. [3], though peculiarities of the exchange channel problem limit the visibility of this ancestry. Source code for our exchanger may be found in the Appendix.

我们的交换器使用非阻塞双数据结构和消除数组的新颖组合来实现高水平的并发。该实现最初是基于我们的双栈[10]和 Hendler 等人的消除栈[3]的组合，尽管交换通道问题的特殊性限制了这种祖先的可见性。我们的交换器的源代码可以在附录中找到。

To simplify understanding, we present our exchanger algorithm in two parts. Section 3.1 first illustrates a simple exchanger that satisfies the requirements for being a lock-free dual data structure as defined earlier in Section 2.2. We then describe in Section 3.2 the manner in which we incorporate elimination to produce a scalable lock-free exchanger.

为了简化理解，我们将我们的交换器算法分为两部分。第 3.1 节首先说明了一个简单的交换器，它满足了前面第 2.2 节中定义的无锁双数据结构的要求。然后，我们在第 3.2 节中描述了我们采用消除的方式来产生一个可扩展的无锁交换器。

### 3.1 A Simple Nonblocking Exchanger
The main data structure we use for the simplified exchanger is a modified dual stack [10]. Additionally, we use an inner node class that consists of a reference to an Object offered for exchange and an AtomicReference representing the hole for an object. We associate one node with each thread attempting an exchange. Exchange is accomplished by successfully executing a compareAndSet, updating the hole from its initial null value to the partner’s node. In the event that a thread has limited patience for how long to wait before abandoning an exchange, signaling that it is no longer interested consists of executing a compareAndSet on its own hole, updating the value from null to a FAIL sentinel. If this compareAndSet succeeds, no other thread can successfully match the node; conversely, the compareAndSet can only fail if some other thread has already matched it.

我们用于简化交换器的主要数据结构是一个改进的双栈[10]。此外，我们使用了一个内部节点类，它由对提供给交换的对象的引用和代表对象的洞的 AtomicReference 组成。我们将一个节点与每个试图进行交换的线程联系起来。交换是通过成功执行 compareAndSet 来完成的，将洞从最初的空值更新到合作伙伴的节点。如果一个线程在放弃交换前等待的时间有限，那么表示它不再感兴趣的信号包括在它自己的洞上执行一个compareAndSet，将洞的值从 null 更新到一个 FAIL 哨位。如果这个 compareAndSet 成功了，就没有其他线程可以成功匹配这个节点；反之，只有当其他线程已经匹配了这个节点，compareAndSet 才会失败。

From this description, one can see how to construct a simple non-blocking exchanger. Referencing the implementation in Listing 1: Upon arrival, if the top-of-stack is null (line 07), we compareAndSet our thread’s node into it (08) and wait until either its patience expires (10-12) or another thread matches its node to us (09, 17). Alternatively, if the top-of-stack is non-null (19), we attempt to compareAndSet our node into the existing node’s hole (20); if successful, we then compareAndSet the top-of-stack back to null (22). Otherwise, we help remove the matched node from the top of the stack; hence, the compareAndSet is unconditional.

从这个描述中，我们可以看到如何构建一个简单的非阻塞式交换器。参考清单 1 中的实现：到达后，如果栈顶是空的（第 07 行），我们就把我们线程的节点比对到栈顶（08），然后等待，直到它的耐心过期（10-12）或者其他线程把它的节点匹配给我们（09，17）。或者，如果堆栈顶部是非空的（19），我们试图比较和设置我们的节点到现有节点的洞中（20）；如果成功，我们再比较和设置堆栈顶部为空（22）。否则，我们帮助从栈顶移除匹配的节点；因此，compareAndSet 是无条件的。

In this simple exchanger, the initial linearization point for an inprogress swap is when the compareAndSet on line 18 succeeds; this inserts a reservation into the channel for the next data item to arrive. The linearization point for a fulfilling operation is when the compareAndSet on line 20 succeeds; this breaks the waiting thread’s spin (lines 9-16). (Alternatively, a successful compareAndSet on line 11 is the linearization point for an aborted exchange.) As it is clear that the waiter’s spin accesses no remote memory locations and that both inserting and fulfilling reservations are lock-free (a compareAndSet in this case can only fail if another has succeeded), the simple exchanger constitutes a lock-free implementation of the exchanger dual data structure as defined in Section 2.2.

在这个简单的交换器中，正在进行的交换的初始线性化点是第 18 行的 compareAndSet 成功时；这为下一个数据项的到来在通道中插入了一个保留。完成操作的线性化点是第 20 行的 compareAndSet 成功时；这打破了等待线程的旋转（第 9-16 行）。(或者说，第 11 行成功的 compareAndSet 是中止交换的线性化点）。很明显，等待者的旋转没有访问任何远程内存位置，而且插入和履行保留都是无锁的（在这种情况下，只有当另一个成功时，compareAndSet 才会失败），简单的交换器构成了第 2.2 节中定义的交换器双数据结构的无锁实现。

### 3.2 Adding Elimination
Although the simple exchanger from the previous section is non-blocking, it will not scale very well: The top-of-stack pointer in particular is a hotspot for contention. This scalability problem can be resolved by adding an elimination step to the simple exchanger from Listing 1.

尽管上一节中的简单交换器是无阻塞的，但它的规模并不大。特别是栈顶指针是一个争夺的热点。这个可扩展性问题可以通过给清单 1 中的简单交换器添加一个消除步骤来解决。

```
    Object exchange(Object x, boolean timed, long patience) throws TimeoutException {
        boolean success = false;
        long start = System.nanotime();
        Node mine = new Node(x);
        for (;;) {
            Node top = stack.getTop();
            if (top == null) {
                if (stack.casTop(null, mine)) {
                    while (null == mine.hole) {
                        if (timeOut(start, timed, patience)) {
                            if (mine.casHole(null, FAIL))
                                throw new TimeoutException();
                            break;
                        }
                        // else spin
                    }
                    return mine.hole.item;
                }
            } else {
                success = top.casHole(null, mine);
                stack.casTop(top, null);
                if (success)
                    return top.item;
            }
        }
    }

    // Listing 1: A simple lock-free exchanger
```

In order to support elimination, we replace the single top-of-stack pointer with an arena (array) of (P + 1)/2 Java SE 5.0 AtomicReferences, where P is the number of processors in the runtime environment. Logically, the reference in position 0 is the top-of-stack; the other references are simply locations at which elimination can occur.

为了支持消除，我们用一个 (P + 1) / 2 Java SE 5.0 AtomicReferences 的场(数组)来代替单一的堆顶指针，其中 P 是运行时环境中的处理器数量。从逻辑上讲，位置 0 的引用是栈顶；其他的引用只是可以消除的位置。

Following the lead of Hendler et al., we incorporate elimination with backoff when encountering contention at top-of-stack. As in their work, by only attempting elimination under conditions of high contention, we incur no additional overhead for elimination. 

在 Hendler 等人的领导下，我们在遇到栈顶部的争用时，将消除与后退相结合。与他们的工作一样，通过只在高争用条件下尝试消除，我们没有为消除产生额外的开销。

Logically, in each iteration of a main loop, we attempt an exchange in the 0th arena position exactly as in the simple exchanger. If we successfully insert or fulfill a reservation, we proceed exactly as before. The difference, however, comes when a compareAnd-Set fails. Now, instead of simply retrying immediately at the top- of-stack, we back off to attempt an exchange at another (randomized) arena location. In contrast to exchanges at arena[0], we limit the length of time we wait with a reservation in the remainder of the arena to a value significantly smaller than our overall patience. After canceling the reservation, we return to arena[0] for another iteration of the loop.

从逻辑上讲，在主循环的每个迭代中，我们在第 0 个竞技场的位置尝试交换，与简单交换器完全一样。如果我们成功地插入或完成一个保留，我们就会像以前一样继续进行。然而，不同的是，当一个比较和设置失败时。现在，我们不是简单地在堆栈顶部立即重试，而是退到另一个（随机的）竞技场位置上尝试交换。与竞技场[0]的交换不同，我们将在竞技场剩余位置的预约等待时间限制在一个明显小于我们整体耐心的值上。在取消预订后，我们返回竞技场[0]，进行另一次循环迭代。

In iteration i of the main loop, the arena location at which we attempt a secondary exchange is selected randomly from the range 1..b, where b is the lesser of i and the arena size. Hence, the first secondary exchange is always at arena[1], but with each iteration of the main loop, we increase the range of potential backoff locations until we are randomly selecting a backoff location from the entire arena. Similarly, the length of time we wait on a reservation at a backoff location is randomly selected from the range 0..2(b+k) − 1, where k is a base for the exponential backoff.

在主循环的迭代 i 中，我们尝试二次交换的竞技场位置是从 1...b 的范围内随机选择的，其中 b 是 i 和竞技场大小的较小值。因此，第一次二次交换总是在竞技场[1]，但随着主循环的每次迭代，我们增加潜在的退场位置的范围，直到我们从整个竞技场中随机选择一个退场位置。同样地，我们在一个退场地点等待预订的时间长度是从 0...2（b+k）-1 的范围内随机选择的，其中 k 是指数退场的基数。

From a correctness perspective, the same linearization points as in the simple exchanger are again the linearization points for the eliminating exchanger; however, they can occur at any of the arena slots, not just at a single top-of-stack. Although the eliminating stack can be shown to support LIFO ordering semantics, we have no particular ordering semantics to respect in the case of an exchange channel: Any thread in the channel is free to match to any other thread, regardless of when it entered the channel.

从正确性的角度来看，与简单交换器中相同的线性化点又是消除交换器的线性化点；然而，它们可以发生在任何一个竞技场的槽中，而不仅仅是在单一的堆栈顶部。尽管消除堆栈可以被证明支持后进先出的排序语义，但在交换通道的情况下，我们没有特别的排序语义需要尊重。通道中的任何线程都可以自由地与任何其他线程匹配，无论它何时进入通道。

The use of timed backoff accentuates the probabilistic nature of limited-patience exchange. Two threads that attempt an exchange with patience zero will only discover each other if they both happen to probe the top of the stack at almost exactly the same time. However, with increasing patience levels, the probability decreases exponentially that they will fail to match after temporally proximate arrivals. Other parameters that influence this rapid fall include the number of processors and threads, hardware instruction timings, and the accuracy and responsiveness of timed waits. In modern environments, the chance of backoff arena use causing two threads to miss each other is far less than the probability of thread scheduling or garbage collection delaying a blocked thread’s wakeup for long enough to miss a potential match.

定时退避的使用突出了有限耐心交换的概率性质。两个试图以零耐心进行交换的线程只有在它们碰巧在几乎完全相同的时间探测堆栈的顶部时才会发现对方。然而，随着耐心水平的提高，他们在时间上相近的到达后无法匹配的概率呈指数级下降。影响这种快速下降的其他参数包括处理器和线程的数量、硬件指令时序以及定时等待的准确性和响应性。在现代环境中，退场场馆的使用导致两个线程相互错过的概率远远小于线程调度或垃圾收集将被阻塞的线程唤醒的时间推迟到足以错过潜在匹配的概率。

### 3.3 Pragmatics
Our implementation of this algorithm (shown in the Appendix) reflects a few additional pragmatic considerations to maintain good performance:

我们对这一算法的实现（见附录）反映了一些额外的务实考虑，以保持良好的性能。

First, we use an array of AtomicReferences rather than a single AtomicReferenceArray. Using a distinct reference object per slot helps avoid some false sharing and cache contention, and places responsibility for their placement on the Java runtime system rather than on this class.

首先，我们使用一个 AtomicReferences 数组，而不是一个单一的AtomicReferenceArray。每个槽使用一个不同的引用对象有助于避免一些错误的共享和缓存争夺，并将放置它们的责任放在 Java 运行时系统而不是这个类上。

Second, the time constants used for exponential backoff can have a significant effect on overall throughput. We empirically chose a base value to be just faster than the minimum observed round-trip overhead, across a set of platforms, for timed parkNanos() calls on already-signaled threads. By so doing, we have selected the smallest value that does not greatly underestimate the actual wait time. Over time, future versions of this class might be expected to use smaller base constants.

其次，用于指数退避的时间常数对总体吞吐量有很大影响。我们根据经验选择了一个基础值，该值刚好快于在一组平台上观察到的最小往返开销，用于在已经发出信号的线程上定时调用 parkNanos()。通过这样做，我们选择了一个不会大大低估实际等待时间的最小值。随着时间的推移，这个类的未来版本可能会使用更小的基础常数。

## 4 EXPERIMENTAL RESULTS
(暂缓)

## 5 Conclusion
In this paper, we have demonstrated a novel lock-free exchange channel that achieves very high scalability through the use of elimination. To our knowledge, this is the first combination of dual data structures and elimination arrays.

在本文中，我们展示了一种新型的无锁交换通道，通过使用消除法实现了非常高的可扩展性。据我们所知，这是双数据结构和消除阵列的首次结合。

In a head-to-head microbenchmark comparison, our algorithm outperforms the Java SE 5.0 Exchanger by a factor of two at two threads and a factor of 50 at 10 threads. We have further shown that this performance differential spells the difference between performance degradation and linear parallel speedup in a real-world genetic algorithm-based traveling salesman application. Our new exchange channel has been adopted for inclusion in Java 6.

在头对头的微观基准比较中，我们的算法在两线程时比 Java SE 5.0 Exchanger高出 2 倍，在 10 线程时高出 50 倍。我们进一步表明，在一个基于遗传算法的旅行销售员应用中，这种性能差异意味着性能下降和线性并行加速的区别。我们的新交换通道已被纳入Java 6中。

For future work, we suspect that other combinations of elimination with dual data structures might see similar benefits; in particular, our dual queue seems to be an ideal candidate for such experimentation. Additionally, we look forward to implementing and experimenting with our red-blue exchanger.

对于未来的工作，我们怀疑其他的消除与双数据结构的组合可能会看到类似的好处；特别是，我们的双队列似乎是这种实验的理想候选人。此外，我们期待着实施和实验我们的红蓝交换器。

## APPENDIX
```
public class Exchanger<V> {
    private static final int SIZE = (Runtime.getRuntime().availableProcesssors() + 1) / 2;
    private static final long BACKOFF_BASE = 128L;
    static final Object FAIL = new Object();
    private final AtomicReference[] arena;
    public Exchanger() {
        arena = new AtomicReference[SIZE + 1];
        for (int i = 0; i < arena.length; i++)
            arena[i] = new AtomicReference();
    }

    public V exchange(V x) throws InterruptedException {
        try {
            return (V)doExchange(x, false, 0);
        } catch (TimeoutException cannotHappen) {
            throw new Error(cannotHappen);
        }
    }

    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        return (V) doExchange(x, true, unit.toNanos(timeout));
    }

    private Object doExchange(Object item, boolean timed, long nanos) throws InterruptedException, TimeoutException {
        Node me = new Node(item);
        long lastTime = (timed)? System.nanoTime() : 0;
        int idx = 0;
        int backoff = 0;

        for (;;) {
            AtomicReference<Node> slot = (AtomicReference<Node>)arena[idx];

            // If this slot is occupied, an item is waiting...
            Node you = slot.get();
            if (you != null) {
                Object v = you.fillHole(item);
                slot.compareAndSet(you, null);
                if (v != FAIL)          // ... unless it's cancelled
                    return v;
            }

            // Try to occupy this slot
            if (slot.compareAndSet(null, me)) {
                Object v = ((idx == 0)? me.waitForHole(timed, nanos) : me.waitForHole(true, randomDelay(backoff)));
                slot.compareAndSet(me, null);
                if (v != FAIL)
                    return v;
                if (Thread.interrupted())
                    throw new InterruptedException();
                if (timed) {
                    long now = System.nanoTime();
                    nanos -= now - lastTime;
                    lastTime = now;
                    if (nanos <= 0)
                        throw new TimeoutException();
                }
                me = new Node(item);
                if (backoff < SIZE - 1)
                    ++backof;
                idx = 0;        // Restart at top
            } else // Retry with a random non-top slot <= backoff
                idx = 1 + random.nextInt(backoff + 1);
        }
    }

    private long randomDelay(int backoff) {
        return ((BACKOFF_BASE << backoff) - 1) & random.nextInt();
    }

    static final class Node extends AtomicReference<Object> {
        final Object item;
        final Thread waiter;
        Node(Object item) {
            this.item = item;
            waiter = Thread.currentThread();
        }

        Object fillHole(Object val) {
            if (compareAndSet(null, val)) {
                LockSupport.unpark(waiter);
                return item;
            }
            return FAIL;
        }

        Object waitForHole(boolean timed, long nanos) {
            long lastTime = (timed) ? System.nanoTime() : 0;
            Object h;
            while ((h = get()) == null) {
                // If interrupted or timed out, try to cancel by CASing FAIL as hole value.
                if (Thread.currentThread().isInterrupted() || (timed && nanos <= 0)) {
                    compareAndSet(null, FAIL);
                } else if (!timed) {
                    LockSupport.park();
                } else {
                    LockSupport.parkNanos(nanos);
                    long now = System.nanoTime();
                    nanos -= now - lastTime;
                    lastTime = now;
                }
            }
            return h;
        }
    }
}
```