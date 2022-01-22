# Nonblocking Concurrent Data Structures with Condition Synchronization
> William N. Scherer III and Michael L. Scott

## Abstract（摘要）
We apply the classic theory of linearizability to operations that must wait for some other thread to establish a precondition. We model such an operation as a request and a follow-up, each with its own linearization point. Linearization of the request marks the point at which a thread's wishes become visible to its peers; linearization of the follow-up marks the point at which the request is fulfilled and the operation takes effect. By placing both linearization points within the purview of object semanitcs, we can specify not only the effects of operations, but also the order in which pending requests should be fulfilled.

我们将经典的线性化理论应用于必须等待其他线程建立先决条件的操作。我们将此类操作建模为请求和后续操作，每个操作都有自己的线性化点。请求的线性化标记了线程的愿望对其对等线程可见的点；跟踪的线性化标记了满足请求和操作生效的点。通过将两个线性化点放在对象语义的范围内，我们不仅可以指定操作的效果，还可以指定应满足挂起请求的顺序。

We use the term dual data structure to describe a concurrent object implementation that may hold both data and reservations (registered requests). By reasoning separately about a request, its successful follow-up, and the period in-between, we obtain meaningful definitions of nonblocking dual data structures. As concrete examples, we present lock-free dualstacks and dualqueues, and experimentally compare their performance with that of lock-based and nonblocking alternatives.

我们使用术语“双数据结构”来描述一个并发对象实现，它可以同时保存数据和保留（已注册的请求）。通过分别对请求、请求的成功后续以及请求之间的时间段进行推理，我们获得了非阻塞双重数据结构的有意义的定义。作为具体的例子，我们给出了无锁双栈和双队列，并通过实验比较了它们与基于锁和非阻塞方案的性能。

## 1. Introduction (简介)
Since its introduction nearly fifteen years ago, linearrizability has become the standard means of reasoning about the correctness of concurrent objects. Informally, linearizability "provides the illusion that each operation... takes effect instantaneously at some point between its invocation and its response" [3, abstract]. Linearizability is "non-blocking" in the sense that it never requires a call to a total method (one whose precondition is simply true) to wait for the execution of any other method. (Certain other correctness criteria, such as serializability [10], may require blocking, e.g. to enforce coherence across a multi-object system.) The fact that it is nonblocking makes linearizability particularly attractive for reasoning about nonblocking implementations of concourrent objects, which provide guarantees of various strength regarding the progress of method calls in practice. In await-free implementation, every contending thread is guaranteed to complete its method call within a bounded number of its own time steps [4]. In a lock-free implementation, some some contending thread is guaranteed to complete its method call within a bounded number of steps (from any thread's point of view) [4]. In an obstruction-free implmentation, a thread is guaranteed to complete its method call within a bounded number of steps in the absence of contention, i.e. if no other threads execute competing methods concurrently [2].

自近十五年前引入以来，线性化已成为论证并发对象正确性的标准方法。非正式地说，线性化“提供了一种错觉，即每个操作……在其调用和响应之间的某个点瞬间生效”[3，摘要]。线性化是“非阻塞”的，因为它从不需要调用 total 方法（前提条件为 true 的方法）来等待任何其他方法的执行。（某些其他正确性标准，如可序列化性[10]，可能需要阻塞，例如，在多对象系统中强制一致性。）。非阻塞性这一事实使得线性化对于并发对象的非阻塞实现的推理特别有吸引力，这为方法调用在实践中的进展提供了各种强度的保证。在无等待实现中，每个争用线程都保证在其自身时间步的有限数量内完成其方法调用[4]。在无锁实现中，某些争用线程保证在一定数量的步骤内完成其方法调用（从任何线程的角度来看）[4]。在无障碍实现中，保证线程在没有争用的情况下，即如果没有其他线程同时执行争用方法[2]，则线程可以在有限的步骤数内完成其方法调用。

These various progress conditions all assume that every method is total. As Herlihy puts it [4, p. 128]:

这些不同的进度条件都假设每种方法都是 total 的。正如赫利希所说

> We restrict our attention to objecs whose operations are total because it is unclear how to interpret the wait-free condition for partial operations. For example, the most natural way to define the effects of a partial deq in a concurrent system is to have it wait until the queue becomes nonempty, a specification tha clearly does not admit a wait-free implementation.
> 
> 我们将注意力限制在操作为全部的对象上，因为不清楚如何解释部分操作的无等待条件。例如，在并发系统中定义部分 deq 效果的最自然的方法是让它等待队列变为非空，这显然是一个不允许无等待实现的规范。

To avoid this problem the designers of nonblocking data structures typically "totalize" their methods by returning an error flag whenever the current state of the object does not admit the method's intended behavior.

为了避免这个问题，当对象的当前状态不允许方法的预期行为时，非阻塞数据结构的设计者通常通过返回错误标志来“汇总”他们的方法。

But partial methods are important! Many applications need a dequeue, pop, or deletemin operation that waits when its structure is empty; these and countless other examples of condition synchronization are fundamental to concurrent programming.

但是局部方法很重要！许多应用程序需要一个 dequeue、pop 或 deletemin 操作，当其结构为空时等待；这些和无数其他条件同步的例子是并发编程的基础。

Given a nonblocking data structure with "totalized" methods, the obvious spin-based strategy is to embed each call in a loop, and retry until it succeeds. This strategy has two important drawbacks. First, it introduces unnecessary contension for memory and communication bandwidth, which may significantly degrade performance, even with careful backoff. Second, it provides no fairness guarantees.

给定一个带有 “totalized” 方法的非阻塞数据结构，明显的方式是采用基于自旋的策略将每个调用嵌入一个循环中，然后重试直到成功。这种策略有两个重要的缺点。首先，它引入了对内存和通信带宽的不必要争用，这可能会显著降低性能，即使小心退避也是如此。其次，它没有提供公平保障。

Consider a total queue whose dequeue method waits until it can return successfully, and a sequence of calls by threads A, B, C, and D:

考虑一个总队列，它的队列方法等待直到它能够成功返回，以及线程 A、B、C 和 D 的调用序列：

```
    C enqueues a 1
    D enqueues a 2
    A calls dequeue
    A's call returns the 2
    B calls dequeue
    B's call returns the 1
```

This is clearly a "bad" execution history, because it returns results in the wrong (non-FIFO) order; it implies an incorrect implementation. The following is clearly a "good" history:

这显然是一个“糟糕”的执行历史，因为它返回的结果顺序错误（非 FIFO）；这意味着一个不正确的实现。以下显然是一段“良好”的历史：

```
    A calls dequeue
    B calls dequeue
    C enqueues a 1
    D enqueues a 2
    A's call returns the 1
    B's call returns the 2
```

But what about the following:

但以下情况如何：

```
    A calls dequeue
    B calls dequeue
    C enqueues a 1
    D enqueues a 2
    B's call returns the 1
    A's call returns the 2
```

If the first line is known to have occurred before the second (this may be the case, for example, if waiting threads can be identified by querying the scheduler, examining a thread control block, or reading an object-specific flag), then intuition suggests that while this history returns results in the right order, it returns them to the wrong threads. If we implement our queue by wrapping the nonblocking "totalized" dequeue in a loop, then this third, questionable history may certainly occur.

如果已知第一行出现在第二行之前（可能是这种情况，例如，如果可以通过查询调度程序、检查线程控制块或读取特定于对象的标志来识别等待的线程），则直觉表明，尽管此历史记录以正确的顺序返回结果，它将它们返回到错误的线程。如果我们通过将非阻塞的“总计”出列包装在一个循环中来实现队列，那么第三个可疑的历史记录肯定会发生。

In the following section we show how to apply the theory of linearizability in such a way that object semantics can sepcify the order in which pending requests will be fulfilled. We then purpose that data structures implement those semantics by explicityly respresenting the set of pending requests. Borrowing terminology from the BBN Butterfly Paraller Processor of the early 1980s [1], we define a dual data structure to be one that may hold reservations (registered requests) instead of, or in addition to, data. A nonblocking dual data structure is one in which (a) every operation either completes or registers a request in a nonblocking fashion, (b) fulfilled requests complete in a nonblocking fashion, and (c) threads that are waiting for their requests to be fulfilled do not interfere with the propgress of other threads.

在下一节中，我们将展示如何以这样一种方式应用线性化理论，即对象语义可以指定挂起请求的完成顺序。然后，我们建议数据结构通过显式表示挂起的请求集来实现这些语义。借用 20 世纪 80 年代早期 BBN Butterfly 并行处理器[1]中的术语，我们将双数据结构定义为一种可以保存保留（注册请求）而不是数据的结构。非阻塞双数据结构是这样一种结构：（A）每个操作都以非阻塞方式完成或注册请求，（b）以非阻塞方式完成已完成的请求，以及（c）等待其请求完成的线程不干扰其他线程的进度。

As a concrete examples, we introduce two lock-free dual data structures in Section 3: a dualstack and a dualqueue. The dualstack both returns results and fulfills requests in LIFO order; the dualqueue does both in FIFO order. Both structures are attractive candidates for "bag of tasks" programming on multiprocessor systems. The dualqueue also subsumes scalable queue-based spin locks and semaphores, and can be used in conjunction with a small-scale test-and-set lock to obtain a limited contention spin lock that embodies an explicit tradeoff between fairness and locality on distributed shared memory machines. Preliminary performance results for dualstacks and dualqueues appear in section 4. We summarize our findings and suggest directions for future work in Section 5.

作为具体的例子，我们在第 3 节中介绍了两种无锁的双重数据结构：dualstack 和 dualqueue。dualstack 以后进先出的顺序返回结果并满足请求；dualqueue 以 FIFO 顺序执行这两项操作。这两种结构都是多处理器系统上“任务包”编程的有吸引力的候选结构。dualqueue 还包含可伸缩的基于队列的自旋锁和信号量，并可与小规模测试和设置锁结合使用，以获得有限竞争自旋锁，该自旋锁体现了分布式共享内存机器上公平性和局部性之间的显式折衷。第 4 节介绍了双堆栈和双队列的初步性能结果。在第5节中，我们总结了我们的发现并给未来工作的方向提出了建议。

## 2. Definitions (定义)

### 2.1 Linearizable Objects 线性化对象
Following Herlihy and Wing [3], a history of an object is a (potentially infinite) sequence of method invocation events ⟨m(args) t⟩ and response (return) events ⟨r(val) t⟩, where m is the name of a method, r is a return condition (usually “ok”), and t identifies a thread. An invocation matches the next response in the sequence that has the same thread id. Together, an invocation and its matching response are called an operation. The invocation and response of operation o may also be denoted inv(o) and res(o), respectively. If event e1 precedes event e2 in history H, we write e1 < H(e2).

继 Herlihy 和 Wing[3]之后，对象的历史记录是方法调用事件的（可能无限）序列 ⟨m（args）t⟩ 和响应（返回）事件⟨r（val）t⟩, 其中 m 是方法的名称，r 是返回条件（通常为“ok”），t 表示线程。一个 invocation 匹配序列中具有相同线程 id 的下一个 response。一个调用及其匹配的响应一起称为一个 operation。operation o 的 invocation 和 response 也可以分别表示为 inv（o）和 res（o）。如果在历史 H 中事件 e1 先于事件 e2，则我们写入e1 < H(e2)。

A history is sequential if every response immediately follows its matching invocation. A non-sequential history is concurrent. A thread subhistory is the subsequence of a history consisting of all events for a given thread. Two histories are equivalent if all their thread subhistories are identical. We consider only well-formed concurrent histories, in which every thread subhistory is sequential, and begins with an invocation.

如果每个响应都紧跟其匹配调用，则历史记录是连续的。非连续历史是并发的。线程子历史记录是由给定线程的所有事件组成的历史记录的子序列。如果两个历史记录的所有线程子历史记录都相同，则这两个历史记录是等效的。我们只考虑形成良好的并发历史，其中每个线程子历史是连续的，并且从调用开始。

We assume that the semantics of an object (which we do not consider formally here) uniquely determine a set of legal sequential histories. In a queue, for example, items must be inserted and removed in FIFO order. That is, the nth successful dequeue in a legal history must return the value inserted by the nth enqueue. Moreover at any given point the number of prior enqueues must equal or exceed the number of successful dequeues. To permit dequeue calls to occur at any time (i.e., to make dequeue a total method—one whose precondition is simply true), one can allow unsuccessful dequeues [⟨deq( ) t⟩ ⟨no(⊥) t⟩] to appear in the history whenever the number of prior enqueues equals the number of prior successful dequeues.

我们假设对象的语义（我们在这里没有正式考虑）唯一地决定了一组合法的序贯历史。例如，在队列中，项目必须按 FIFO 顺序插入和删除。也就是说，合法历史记录中的第 n 个成功出列必须返回第 n 个队列插入的值。此外，在任何给定点，先前成功入队的数量必须等于或超过成功出队的数量。为了允许随时发生 dequeue 调用（即，使出列成为一个总方法，其前提条件仅为 true），只要先前的入队数等于先前成功的出队数，可以允许不成功的出列[⟨deq（）t⟩ ⟨no(⊥) T⟩] 出现在历史记录中。

A (possibly concurrent) history H induces a partial order ≺H on operations: oi ≺H oj if res (oi ) < H inv (oj ). H is linearizable if (a) it is equivalent to some legal sequential history S, and (b) ≺H ⊆ ≺S .

一个（可能并发的）历史 H 会导致一个偏序 ≺H 关于业务： oi ≺H oj if res (oi ) < H inv (oj )。H 是可线性化的，如果（a）它相当于某些合法顺序历史，和（b）≺H⊆ ≺s。

Departing slightly from Herlihy and Wing, we introduce the notion of an augmented history of an object. A (well-formed) augmented history H′ is obtained from a history H by inserting a linearization point ⟨m l(args , val ) t⟩ (also denoted lin (o )) somewhere between each response and its previous matching invocation: inv(o) < H′ lin(o) < H′ res(o).

稍微离开 Herlihy 和 Wing，我们引入了对象的增强历史的概念。通过插入一个线性化点，从历史 H 中获得（格式良好的）扩充历史 H′⟨m l（args，val）t⟩ （也表示为 lin（o））在每个响应和它以前的匹配调用之间的某处：inv（o）< H′lin（o）< H′res（o）。

If H is equivalent to some legal sequential history S and the linearization points of H′ appear in the same order as the corresponding operations in S, then H′ embodies a linearization of H: the order of the linearization points defines a total order on operations that is consistent with the partial order induced by H. Put another way: res (oi ) < H inv (oj ) ⇒ lin (oi ) < H ′ lin (oj ). Given this notion of augmented histo- ries, we can define legality without resort to equivalent sequential histories: we say that an augmented history is legal if its sequence of linearization points is permitted by the object semantics. Similarly, H is linearizable if it can be augmented to produce a legal augmented history H′.

如果 H 等价于某些合法的序列历史 S，且 H′的线性化点的出现顺序与 S 中相应的运算相同，然后H′体现了 H 的线性化：线性化点的顺序定义了与 H 诱导的偏序一致的操作的总顺序。换句话说：res（oi）< H inv（oj）⇒ lin（oi）< H′lin（oj）。考虑到增广历史的概念，我们可以定义合法性，而不必求助于等价的序列历史：如果对象语义允许其线性化点序列，我们说增广历史是合法的。类似地，如果可以对 H 进行增广以产生合法的增广历史 H′，则 H 是可线性化的。

### 2.2 Implementations 实现

We define an implementation of a concurrent object as a pair (ß,∂), where

我们将并发对象的实现定义为一对（ß，∂), 

1. ß is a set of valid executions of some operational system, e.g. the possible interleavings of machine instructions among threads executing a specified body of C code on some commercial multiprocessor. Each execution takes the form of a series of steps (e.g., instructions), each of which is identified with a particular thread and occurs atomically. Implementations of nonblocking concurrent objects on real machines typically rely not only on atomic loads and stores, but on such universal atomic primitives [4] as compare and swap or load linked and store conditional, each of which completes in a single step.

ß 是某些操作系统的一组有效执行，例如，在某些商用多处理器上执行特定 C 代码体的线程之间可能出现的机器指令交错。每次执行都采取一系列步骤（例如，指令）的形式，每个步骤都由一个特定的线程标识，并以原子方式发生。实际机器上非阻塞并发对象的实现通常不仅依赖于原子加载和存储，还依赖于 compare.and.swap 或 load.linked and store_conditional 等通用原子原语[4]，每一个原语都在一个步骤中完成。

2. ∂ is an interpretation that maps each execution E ∈ ß to some augmented object history H′ = ∂(E) whose events (including the linearization points) are identified with steps of E in such a way that if e1 < H′ e2, then s(e1) ≤ E s(e2), where s(e) for any event e is the step identified with e.

∂ 是映射每个执行的解释 E ∈ ß对于某些增强对象历史 H′= ∂(E），其事件（包括线性化点）通过 E 的步骤识别，如果e1 < H′e2，则 s（e1）≤ E s（e2），其中任何事件 E 的 s（E）是用 E 标识的步骤。

We say an implementation is correct if ∀E ∈ ß, ∂(E) is a legal augmented history of the concurrent object.

我们说实现是正确的，如果 ∀E ∈ ß, ∂(E） 是并发对象的合法扩充历史记录。

In practice, of course, steps in an execution have an observable order only if they are executed by the same thread, or if there is a data dependence between them [7]. In particular, while we cannot in general observe that s(inv (oi )) < E s(inv (oj )), where oi and oj are performed by different threads, we can observe that s(res (oi )) < E s(inv (oj )), because oi ’s thread may write a value after its response that is read by oj ’s thread before its invocation. The power of linearizability lies in its insistence that the semantic order of operations on a concurrent object be consistent with such externally observable orderings.

当然，在实践中，只有当执行中的步骤由同一线程执行，或者它们之间存在数据依赖时，执行中的步骤才具有可观察的顺序[7]。特别是，虽然我们通常无法观察到 s（inv（oi））< e s（inv（oj）），其中 oi 和 oj 由不同的线程执行，但我们可以观察到s（res（oi））< e s（inv（oj）），因为oi的线程可能会在其响应之后写入一个值，该值在调用之前由 oj 的线程读取。线性化的力量在于它坚持并发对象上操作的语义顺序与这种外部可观察的顺序一致。

In addition to correctness, an implementation may have a variety of other properties of interest, including bounds on time (steps), space, or remote memory accesses; the suite of required atomic instructions; and various progress conditions. An implementation is wait-free if we can bound, for all executions E and invocations inv(o) in ∂(E), the number of steps between (the steps identified with) inv(o) and res(o). An implementation is lock-free if for all invocations inv (oi ) we can bound the number of steps between (the steps identified with) inv (oi ) and the (not necessarily matching) first subsequent response res(oj ). An implementation is obstruction-free if for all threads t and invocations inv(o) performed by t we can bound the number of consecutive steps performed by t (with no intervening steps by any other thread) between (the steps identified with) inv(o) and res(o). Note that the definitions of lock freedom and obstruction freedom permit executions in which an invocation has no matching response (i.e., in which threads may starve).

除了正确性之外，一个实现可能还具有各种其他感兴趣的属性，包括时间（步长）、空间或远程内存访问的界限；所需的一套原子指令；以及各种进展情况。如果我们可以绑定，那么实现是无等待的，因为在 ∂（E） ，表示（用）inv（o）和 res（o）标识的步骤之间的步骤数。如果对于所有调用 inv（oi），我们可以将inv（oi）和（不一定匹配）第一个后续响应res（oj）之间的步骤数绑定在一起，则实现是无锁的。如果对于所有线程t和由t执行的调用inv（o），我们可以在（由inv（o）和res（o标识的步骤）之间绑定由t执行的连续步骤数（没有任何其他线程的干预步骤），则实现是无障碍的。请注意，锁自由度和阻塞自由度的定义允许执行调用没有匹配响应（即线程可能会饿死）的执行。

### 2.3 Adaptation to Objects with Partial Methods

When an object has partial methods, we divide each such method into a request method and a follow-up method, each of which has its own invocations and responses. A total queue, for example, would provide dequeue request and dequeue followup methods. By analogy with Lamport’s bakery algorithm, the request method returns a ticket, which is then passed as an argument to the follow-up method. The follow-up, for its part, returns either the desired result or, if the method’s precondition has not yet been satisfied, an error indication.

当一个对象有部分方法时，我们将每个这样的方法分为请求方法和后续方法，每个方法都有自己的调用和响应。例如，一个总队列将提供出列请求和出列后续方法。与 Lamport 的 bakery算法类似，request 方法返回一张票证，然后作为参数传递给后续方法。后续操作则返回所需的结果，如果方法的前提条件尚未满足，则返回错误指示。

The history of a concurrent object now consists not only of invocation and response events ⟨m(args) t⟩ and ⟨ok(val) t⟩ for total methods m, but also request invocation and response events ⟨preq(args) t⟩ and ⟨ok(tik) t⟩, and follow-up invocation and re- sponse events ⟨pfol(tik) t⟩ and ⟨r(val) t⟩ for partial methods p. A request invocation and its matching response are called a request operation; a follow-up invocation and its matching response are called a follow-up operation. The request invocation and re- sponse of operation o may be denoted inv(or) and res(or); the follow-up invocation and response may be denoted inv(of ) and res(of ).

并发对象的历史现在不仅包括调用和响应事件 ⟨m（args）t⟩ 和⟨ok（val）t⟩ 对于 total方法 m，还包括请求调用和响应事件 ⟨preq（args）t⟩ 和⟨好的⟩, 以及后续调用和响应事件⟨pfol（tik）t⟩ 和⟨r（val）t⟩ 对于部分方法p，请求调用及其匹配响应称为请求操作；后续调用及其匹配响应称为后续操作。操作o的请求调用和响应可以表示为inv（or）和res（or）；后续调用和响应可以表示为inv（of）和res（of）。

A follow-up with ticket argument k matches the previous request that returned k. A follow-up operation is said to be successful if its response event is ⟨ok (val ) t⟩; it is said to be unsuccessful its its response event is ⟨no(⊥) t⟩. We consider only well-formed histories, in which every thread subhistory is sequential, and is a prefix of some string in the regular set (ru⋆s)⋆, where r is a request, u is an unsuccessful follow-up that matches the preceding r, and s is a successful follow-up that matches the preceding r.

带有票证参数 k 的后续操作与返回 k 的上一个请求相匹配。如果后续操作的响应事件为⟨ok（val）t⟩; 据说是不成功的，其反应事件是⟨没有(⊥) T⟩. 我们只考虑形成良好的历史，其中每个线程子历史是连续的，并且是正则集中的一些字符串的前缀（RU）。⋆（s）⋆, 其中 r 是请求，u 是匹配前面 r 的不成功后续，s 是匹配前面 r 的成功后续。

Because it consists of a sequence of operations (beginning with a request and ending with a successful response), a call to a partial method p has a sequence of linearization points, including an initial linearization point ⟨pi(args) t⟩ somewhere between the in- vocation and the response of the request, and a final linearization point ⟨pf(val) t⟩ somewhere between the invocation and the response of the successful matching follow- up. The initial and final linearization points for p may also be denoted in(p) and fin(p).

因为它由一系列操作组成（以请求开始，以成功响应结束），对部分方法 p 的调用有一系列线性化点，包括初始线性化点⟨pi（args）t⟩ 介于工作和请求响应之间，以及最终线性化点⟨pf（val）t⟩ 在调用和成功匹配后续的响应之间的某个地方。p 的初始和最终线性化点也可以在（p）和fin（p）中表示。

We say an augmented history is legal if its sequence of linearization points is among those determined by the semantics of the object. This definition allows us to capture partial methods in object semantics. In the previous section we suggested that the semantics of a queue might require that (1) the nth successful dequeue returns the value inserted by the nth enqueue, and (2) the number of prior enqueues at any given point equals or exceeds the number of prior successful dequeues. We can now instead require that (1′) the nth final linearization point for dequeue contains the value from the linearization point of the nth enqueue, (2′) the number of prior linearization points for enqueue equals or exceeds the number of prior final lin- earization points for dequeue, and (3′) at the linearization point of an unsuccessful dequeue followup, the number of prior linearization points for enqueue exactly equals the number of prior final linearization points for dequeue (i.e., linearization points for successful dequeue followups). These rules ensure not only that the queue returns results in FIFO order, but also that pending requests for partial methods (which are now permitted) are fulfilled in FIFO order.

如果一个扩充历史的线性化点序列是由对象的语义决定的，那么我们说它是合法的。这个定义允许我们捕获对象语义中的部分方法。在上一节中，我们建议队列的语义可能要求（1）第n个成功出列返回第n个成功出列所插入的值，以及（2）任何给定点的先前出列数等于或超过先前成功出列数。我们现在可以要求（1′）排队的第 n 个最终线性化点包含来自第 n 个排队的线性化点的值，（2′）排队的先前线性化点的数量等于或超过排队的先前最终线性化点的数量，和（3′）在不成功出列后续的线性化点处，排队的先前线性化点的数量正好等于出列的先前最终线性化点的数量（即，成功出列后续的线性化点）。这些规则不仅确保队列以 FIFO 顺序返回结果，而且确保部分方法（现在允许）的挂起请求以 FIFO 顺序完成。

As before, a history H is linearizable if it can be augmented to produce a legal augmented history H′, and an implementation (ß, ∂) is correct if ∀E ∈ ß, ∂(E) is a legal augmented history of the concurrent object.

如前所述，如果历史H可以被扩充以产生合法的扩充历史H′，则历史 H 是可线性化的，并且实现（ß，∂）是正确的，如果∀E∈ ß、∂(E）是并发对象的合法扩充历史。

Given the definition of well-formedness above, a thread t that wishes to execute a partial method p must first call p request and then call p followup in a loop until it succeeds. This is very different from calling a traditional “totalized” method until it succeeds: linearization of a distinguished request operation is the hook that allows object semantics to address the order in which pending requests will be fulfilled.

根据上述良构性的定义，希望执行部分方法p的线程 t 必须首先调用p request，然后在循环中调用 p followup，直到成功。这与在成功之前调用传统的“totalized”方法非常不同：可分辨请求操作的线性化是一个钩子，它允许对象语义处理挂起请求的完成顺序。

As a practical matter, implementations may wish to provide a p demand method that waits until it can return successfully, and/or a plain p method equivalent to p demand(p request). The obvious implementation of p demand contains a busy- wait loop, but other implementations are possible. In particular, an implementation may choose to use scheduler-based synchronization to put t to sleep on a semaphore that will be signaled when p’s precondition has been met, allowing the processor to be used for other purposes in the interim. We require that it be possible to provide request and follow-up methods, as defined herein, with no more than trivial modifications to any given implementation. The algorithms we present in Section 3 provide only a plain p interface, with internal busy-wait loops.

实际上，实现可能希望提供一个 p-demand 方法，等待它成功返回，和/或 一个与 p-demand（p-request）等价的普通 p 方法。p demand 的明显实现包含一个忙等待循环，但其他实现也是可能的。具体地说，实现可以选择使用基于调度器的同步将 t 置于信号量上的睡眠状态，该信号量将在满足p的前提条件时发出信号，从而允许处理器在过渡期间用于其他目的。我们要求能够提供本文定义的请求和后续方法，而对任何给定的实现只进行微不足道的修改。我们在第3节中介绍的算法只提供一个普通的p接口，带有内部忙等待循环。

#### Progress Conditions
When reasoning about progress, we must deal with the fact that a partial method may wait for an arbitrary amount of time (perform an arbitrary number of unsuccessful follow-ups) before its precondition is satisfied. Clearly we wish to require that requests and follow-ups are nonblocking. But this is not enough: we must also prevent unsuccessful follow-ups from interfering with progress in other threads. We do so by prohibiting such operations from accessing remote memory. On a cache-coherent machine, an access by thread t within operation o is said to be remote if it writes to memory that may (in some execution) be read or written by threads other than t more than a constant number of times between inv(or) and res(of ), or if it reads memory that may (in some execution) be written by threads other than t more than a constant number of times between inv(or) and res(of ). On a non-cache-coherent machine, an access by thread t is also remote if it refers to memory that t itself did not allocate.

在对进展进行推理时，我们必须处理这样一个事实，即部分方法在满足其前提条件之前可能会等待任意时间（执行任意数量的不成功后续操作）。显然，我们希望要求请求和后续行动是非阻塞的。但这还不够：我们还必须防止不成功的后续操作干扰其他线程的进度。我们通过禁止此类操作访问远程内存来实现这一点。在缓存一致性机器上，如果操作 o 中的线程 t 对内存的访问（在某些执行中）可能由 t 以外的线程读取或写入超过 inv（or）和 res（of）之间的恒定次数，则称操作 o 中的线程 t 的访问为远程访问，或者如果它在inv（or）和res（of）之间读取可能（在某些执行中）由 t 以外的线程写入的内存的次数超过常量。在非缓存一致性机器上，线程 t 的访问也是远程的，如果它引用的是 t 本身没有分配的内存。

### 2.4 Dual Data Structures
We define a dual data structure D to be a concurrent object implementation that may hold reservations (registered requests) instead of, or in addition to, data. Reservations correspond to requests that cannot be fulfilled until the object satisfies some necessary precondition. A reservation may be removed from D, and a call to its follow-up method may return, when some call by another thread makes the precondition true. D is a nonblocking dual data structure if
1. It is a correct implementation of a linearizable concurrent object, as defined above.
2. All operations, including requests and follow-ups, are nonblocking.
3. Unsuccessful follow-ups perform no remote memory accesses.

我们将双数据结构D定义为一个并发对象实现，它可以保存保留（已注册的请求），而不是数据，或者除了数据之外。保留对应于在对象满足某些必要的前提条件之前无法满足的请求。当另一个线程的某个调用使前提条件为真时，可以从D中删除保留，并返回对其后续方法的调用。D是非阻塞双数据结构，如果
1. 它是上述可线性化并发对象的正确实现。
2. 所有操作，包括请求和后续操作，都是非阻塞的。
3. 不成功的后续操作不执行远程内存访问。

Nonblocking dual data structures may be further classified as wait-free, lock-free, or obstruction-free, depending on their guarantees with respect to condition (2) above. In the following section we consider concrete lock-free implementations of a dualstack and a dualqueue.

非阻塞双数据结构可进一步分类为无等待、无锁或无阻塞，这取决于它们对上述条件（2）的保证。在下面的部分中，我们考虑了双锁栈和双队列的具体无锁实现。

## 3 Example Data Structures 示例数据结构
Space limitations preclude inclusion of pseudocode in the conference proceedings. Both example data structures can be found on-line at www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html. Both use a double-width compare and swap (CAS) instruction (as provided, for example, on the Sparc) to create “counted pointers” that avoid the ABA problem: each vulnerable pointer is paired with a serial number, which is incremented every time the pointer is updated to a non-NULL value. We assume that no thread can stall long enough to see a serial number repeat. On a machine with (single-word) load_linked/store_conditional (LL/SC) instructions, the serial numbers would not be needed.

由于篇幅限制，论文中不能包含伪代码。两个示例数据结构都可以在线找到。两者都使用双宽度 CAS 指令（如Sparc上提供的）来创建“计数指针”，以避免 ABA 问题：每个指针都与一个序列号配对，该序列号在指针每次更新为非空值时递增。我们假设没有线程可以暂停足够长的时间，以看到序列号重复。在具有（单字）load_linked/store_conditional（LL/SC）指令的机器上，不需要序列号。

### 3.1 The Dualstack 双端堆栈
The dualstack is based on the standard lock-free stack of Treiber [13]. So long as the number of calls to pop does not exceed the number of calls to push, the dualstack behaves the same as its non-dual cousin.

dualstack 基于 Treiber 的标准无锁堆栈。只要对 pop 的调用数不超过对 push 的调用数，dualstack 的行为与其非 dualstack 的行为相同。

When the stack is empty, or contains only reservations, the pop method pushes a reservation, and then spins on the data node field within it. A push method always pushes a data node. If the previous top node was a reservation, however, the two adjacent nodes “annihilate each other”: any thread that finds a data node and an underlying reservation at the top of the stack attempts to (a) write the address of the former into the data node field of the latter, and then (b) pop both nodes from the stack. At any given time, the stack contains either all reservations, all data, or one datum (at the top) followed by reservations.

当堆栈为空或仅包含保留时，pop 方法将推送保留，然后在其中的数据节点字段上旋转。push 方法总是推送数据节点。但是，如果前一个顶部节点是保留结点，则两个相邻结点“相互湮灭”：任何在堆栈顶部找到数据节点和基础保留的线程都会尝试（a）将前者的地址写入后者的数据节点字段，然后（b）从堆栈中弹出两个节点。在任何给定时间，堆栈包含所有保留、所有数据或一个数据（位于顶部），后跟保留。

Both the head pointer and the next pointers in stack nodes are tagged to indicate whether the next node in the list is a reservation or a datum and, if the latter, whether there is a reservation beneath it in the stack. We assume that nodes are word-aligned, so that these tags can fit in the low-order bits of a pointer. For presentation purposes the on-line pseudocode assumes that data values are integers, though this could obviously be changed to any type (including a pointer) that will fit, together with a serial number, in the target of a double-width CAS (or in a single word on a machine with LL/SC). To differentiate between the cases where the topmost data node is present to fulfill a request and where the stack contains all data, pushes for the former case set both the data and reservation tags; pushes for the latter set only the data tag.

堆栈节点中的头指针和下一个指针都被标记，以指示列表中的下一个节点是保留节点还是基准节点，如果是后者，则指示堆栈中它下面是否有保留。我们假设节点是字对齐的，因此这些标记可以放入指针的低位。出于表示目的，在线伪代码假定数据值为整数，但这显然可以更改为任何类型（包括指针），与序列号一起适合双倍宽度 CAS 的目标（或具有LL/SC的机器上的单个字）。为了区分出现最顶层数据节点以完成请求的情况和堆栈包含所有数据的情况，推送前一个情况集的数据和保留标记；为后者推送仅设置数据标记。

As mentioned in Section 2.3 our code provides a single pop method that subsumes the sequence of operations from a pop request through its successful follow-up. The initial linearization point in pop, like the linearization point in push, is the CAS that modifies the top-of-stack pointer. For pops when the stack is non-empty, this CAS is also the final linearization point. For pops that have to spin, the final linearization point is the CAS (in some other thread) that writes to the data node field of the requester’s reservation, terminating its spin.

如第 2.3 节所述，我们的代码提供了一个单一的 pop 方法，该方法通过成功的后续处理包含 pop 请求中的操作序列。pop 中的初始线性化点与 push 中的线性化点类似，是修改堆栈顶部指针的 CAS。对于堆栈非空的 pops，此 CAS 也是最终线性化点。对于必须旋转的 POP，最后的线性化点是 CAS（在其他线程中），它写入请求者保留的数据节点字段，从而终止其旋转。

The code for push is lock-free, as is the code from the beginning of pop to the initial linearization point, and from the final linearization point (the read that terminates the spin) to the end of pop. Moreover the spin in pop (which would comprise the body of an unsuccessful follow-up operation, if we provided it as a separate method), is entirely local: it reads only the requester’s own reservation node, which the requester allocated itself, and which no other thread will write except to terminate the spin. The dualstack therefore satisfies conditions 2 and 3 of Section 2.4.

push 的代码是无锁的，从 pop 开始到初始线性化点，以及从最终线性化点（终止旋转的读取）到 pop 结束的代码也是无锁的。此外，pop 中的 spin（如果我们将其作为一个单独的方法提供的话，它将包含一个不成功的后续操作的主体）是完全本地的：它只读取请求者自己的保留节点，请求者自己分配了该节点，并且除了终止 spin 之外，没有其他线程会写入该节点。因此，双堆栈满足第 2.4 节的条件 2 和 3。

Though we do not offer a proof, inspection of the code confirms that the dualstack satisfies the usual LIFO semantics for total methods: if the number of previous lin- earization points for push exceeds the number of previous initial linearization points for pop, then a new pop operation p will succeed immediately, and will return the value provided by the most recent previous push operation h such that the numbers of pushes and pops that linearized between h and p are equal. In a similar fashion, the dualstack satisfies pending requests in LIFO order: if the number of previous initial linearization points for pop exceeds the number of previous linearization points for push, then a push operation h will provide the value to be returned by the most recent previous pop operation p such that the numbers of pushes and pops that linearized between p and h are equal. This is condition 1 from Section 2.4.

尽管我们没有提供证据，但对代码的检查确认 dualstack 满足总体方法的通常后进先出语义：如果push 的先前线性化点的数量超过了 pop 的先前初始线性化点的数量，则新的 pop 操作 p 将立即成功，并将返回最近一次推送操作 h 提供的值，以使在 h 和 p 之间线性化的推送和 POP的 数量相等。以类似的方式，dualstack 以后进先出的顺序满足未决请求：如果 pop 先前初始线性化点的数量超过 push 先前线性化点的数量，然后，推送操作 h 将提供由最近的前一个 pop 操作 p 返回的值，使得在 p 和 h 之间线性化的推送和 pop 的数量相等。这是第 2.4 节中的条件 1。

The spin in pop is terminated by a CAS in some other thread (possibly the fulfilling thread, possibly a helper) that updates the data node field in the reservation. This CAS is the final linearization point of the spinning thread. It is not, however, the final linearization point of the fulfilling thread; that occurs earlier, when the fulfilling thread successfully updates the top-of-stack pointer to point to the fulfilling datum. Once the fulfilling push has linearized, no thread will be able to make progress until the spinning pop reaches its final linearization point. It is possible, however, for the spinning thread to perform an unbounded number of (local, spinning) steps in a legal execution before this happens: hence the need to separate the linearization points of the fulfilling and fulfilled operations.

pop 中的 spin 由某个其他线程（可能是执行线程，可能是助手）中的 CAS 终止，该线程更新保留中的数据节点字段。该 CAS 是纺纱线的最终线性化点。然而，它不是实现线程的最终线性化点；当执行线程成功地更新堆栈顶部指针以指向执行数据时，就会出现这种情况。一旦完成推送线性化，在旋转弹跳到达其最终线性化点之前，任何线程都无法前进。然而，对于旋转线程来说，在合法执行过程中，在这种情况发生之前，可以执行无限数量的（局部旋转）步骤：因此需要分离完成操作和完成操作的线性化点。

It is tempting to consider a simpler implementation in which the fulfilling thread pops a reservation from the stack and then writes the fulfilling datum directly into the reservation. This implementation, however, is incorrect: it leaves the requester vulnera- ble to a failure or stall in the fulfilling thread subsequent to the pop of the reservation but prior to the write of the datum. Because the reservation would no longer be in the stack, an arbitrary number of additional pop operations (performed by other threads, and returning subsequently pushed data) could linearize before the requester’s successful follow-up.

很容易想到一种更简单的实现，在其中实现线程从栈中弹出一个预留，然后将实现的数据直接写入保留。然而，这种实现是不正确的：它使请求者容易在保留弹出之后但在写入数据之前的执行线程中发生故障或暂停。因为保留将不再在堆栈中，所以在请求者成功跟进之前，任意数量的额外 pop 操作（由其他线程执行，并随后返回推送数据）可能会线性化。

One possible application of a dualstack is to implement a “bag of tasks” in a localityconscious parallel execution system. If newly created tasks share data with recently completed tasks, it may make sense for a thread to execute a newly created task, rather than one created long ago, when it next needs work to do. Similarly, if there is insuf- ficient work for all threads, it may make sense for newly created tasks to be executed by threads that became idle recently, rather than threads that have been idle for a long time. In addition to enhancing locality, this could allow power-aware processors to enter a low-power mode when running spinning threads, potentially saving significant energy. The LIFO ordering of a dualstack will implement these policies.

dualstack的一个可能应用是在局部感知并行执行系统中实现“任务包”。如果新创建的任务与最近完成的任务共享数据，那么线程在下次需要工作时执行新创建的任务（而不是很久以前创建的任务）是有意义的。类似地，如果所有线程都没有足够的工作，那么由最近空闲的线程执行新创建的任务，而不是由长时间空闲的线程执行，可能是有意义的。除了增强局部性之外，这还可以使电源感知处理器在运行旋转线程时进入低功耗模式，从而潜在地节省大量能源。双堆栈的后进先出排序将实现这些策略。

### 3.2 The DualQueue
The dualqueue is based on the M&S lock-free queue [9]. So long as the number of calls to dequeue does not exceed the number of calls to push, it behaves the same as its non-dual cousin. It is initialized with a single “dummy” node; the first real datum (or reservation) is always in the second node, if any. At any given time the second and subsequent nodes will either all be reservations or all be data.

dualqueue 基于 M&S 无锁队列。只要要退出队列的调用数不超过要推送的调用数，它的行为就与它的非双重表亲相同。它用一个“虚拟”节点初始化；第一个真实基准（或保留）始终位于第二个节点（如果有）。在任何给定时间，第二个和后续节点要么全部是保留节点，要么全部是数据节点。

When the queue is empty, or contains only reservations, the dequeue method enqueues a reservation, and then spins on the request pointer field of the former tail node. The enqueue method, for its part, fulfills the request at the head of the queue, if any, rather than enqueue a datum. To do so, the fulfilling thread uses a CAS to update the reservation’s request field with a pointer to a node (outside the queue) containing the provided data. This simultaneously fulfills the request and breaks the requester’s spin. Any thread that finds a fulfilled request at the head of the queue removes and frees it. (NB: acting on the head of the queue requires that we obtain a consistent snapshot of the head, tail, and next pointers. Extending the technique of the original M&S queue, we use a two-stage check to ensure sufficient consistency to prevent untoward race conditions.)

当队列为空或仅包含保留时，dequeue 方法将保留排入队列，然后在前一个尾部节点的请求指针字段上旋转。就排队方法而言，它在队列的最前面（如果有）完成请求，而不是将数据排队。为此，执行线程使用CAS更新保留的请求字段，并使用指向包含所提供数据的节点（队列外部）的指针。这同时满足了请求并打破了请求者的旋转。任何在队列头部发现已完成请求的线程都会删除并释放该请求。（注意：对队列的头部进行操作需要获得头部、尾部和下一个指针的一致快照。扩展原始M&S队列的技术，我们使用两阶段检查来确保足够的一致性，以防止出现异常竞争情况。）

As in the dualstack, queue nodes are tagged as requests by setting a low-order bit in pointers that point to them. We again assume, without loss of generality, that data values are integers, and we provide a single dequeue method that subsumes the sequence of operations from a dequeue request through its successful follow-up.

在 dualstack 中，通过在指向队列节点的指针中设置低位，队列节点被标记为请求。我们再次假设，在不丧失一般性的情况下，数据值是整数，并且我们提供了一个单独的出列方法，该方法包含从出列请求到成功后续的操作序列。

The code for enqueue is lock-free, as is the code from the beginning of dequeue to the initial linearization point, and from the final linearization point (the read that terminates the spin) to the end of dequeue. The spin in dequeue (which would comprise the body of an unsuccessful follow-up) accesses a node that no other thread will write except to terminate the spin. The dualqueue therefore satisfies conditions 2 and 3 of Section 2.4 on a cache-coherent machine. (On a non-cache-coherent machine we would need to modify the code to provide an extra level of indirection; the spin in dequeue reads a node that the requester did not allocate.)

入队的代码是无锁的，从出队开始到初始线性化点，以及从最终线性化点（终止自旋的读取）到出队结束的代码也是无锁的。退出队列中的 spin（将包含一个不成功的后续操作的主体）访问一个节点，除了终止 spin 之外，其他线程都不会写入该节点。因此，dualqueue 在缓存相关机上满足第 2.4 节 的条件 2 和 3。（在非缓存一致性机器上，我们需要修改代码以提供额外级别的间接寻址；spin-in-dequeue 读取请求者未分配的节点。）

Though we do not offer a proof, inspection of the code confirms that the dualqueue satisfies the usual FIFO semantics for total methods: if the number of previous linearization points for enqueue exceeds the number of previous initial linearization points for dequeue, then a new, nth dequeue operation will return the value provided by the nth enqueue. In a similar fashion, the dualqueue satisfies pending requests in FIFO order: if the number of previous initial linearization points for dequeue exceeds the number of previous linearization points for enqueue, then a new, nth enqueue operation will provide a value to the nth dequeue. This is condition 1 from Section 2.4.

虽然我们没有提供证据，但对代码的检查确认 dualqueue 满足 total 方法的通常 FIFO 语义：如果入队的先前线性化点的数量超过了出队的先前初始线性化点的数量，则新的，第 n 个排队操作将返回第 n 个排队提供的值。以类似的方式，dualqueue 以 FIFO 顺序满足挂起的请求：如果先前用于排队的初始线性化点的数量超过先前用于排队的线性化点的数量，则新的第 n 个排队操作将为第 n 个排队提供一个值。这是第2.4节中的条件 1。

The spin in dequeue is terminated by a CAS in another thread’s enqueue method; this CAS is the linearization point of the enqueue and the final linearization point of the dequeue. Note again that a simpler algorithm, in which the enqueue method could remove a request from the queue and then fulfill it, would not be correct: the CAS operation used for removal would constitute the final linearization point of the enqueue, but the corresponding dequeue could continue to spin for an arbitrary amount of time if the thread performing the enqueue were to stall.

退出队列中的自旋由另一个线程的排队方法中的 CAS 终止；该 CAS 是排队的线性化点和出列的最终线性化点。请再次注意，一个更简单的算法（其中排队方法可以从队列中删除请求，然后完成请求）是不正确的：用于删除的 CAS 操作将构成排队的最终线性化点，但是，如果执行排队的线程暂停，相应的排队可能会继续旋转任意时间。

#### Dualqueue Applications
Dualqueues are versatile. They can obviously be used as a traditional “bag of tasks” or a producer–consumer buffer. They have several other uses as well:

双队列是多功能的。它们显然可以用作传统的“任务包”或生产者-消费者缓冲区。它们还有其他几种用途：

Mutual exclusion. A dualqueue that is initialized to hold a single datum is a previously unknown variety of queue-based mutual exclusion lock. Unlike the widely used MCS lock [8], a dualqueue lock has no spin in the release code: where the MCS lock updates the tail pointer of the queue and then the next pointer of the predecessor’s node, a dualqueue lock updates the next pointer first, and then swings the tail to cover it.

独占互斥。初始化为保存单个数据的 dualqueue 是一种以前未知的基于队列的互斥锁。与广泛使用的MCS 锁不同[8]，dualqueue 锁在发布代码中没有旋转：MCS 锁更新队列的尾部指针，然后更新前置节点的下一个指针，dualqueue 锁首先更新下一个指针，然后摆动尾部覆盖它。

Semaphores. A dualqueue that is initialized with k data nodes constitutes a contention-free spin-based semaphore. It can be used, for example, to allocate k interchangeable resources among a set of competing threads.

信号灯。用 k 个数据节点初始化的 dualqueue 构成了一个无争用的基于自旋的信号量。例如，它可以用于在一组竞争线程之间分配k个可互换资源。

Limited contention lock. As noted by Radovic and Hagersten [11], among others, the strict fairness of queue-based locks may not be desirable on a non-uniform memory access (distributed shared memory) multiprocessor. At the same time, a test-and-set lock, which tends to grant requests to physically nearby threads, can be unacceptably unfair (not to mention slow) when contention among threads is high: threads that are physically distant may starve. An attractive compromise is to allow waiting threads to bypass each other in line to a limited extent. A dualqueue paired with a test-and-set lock provides a straightforward implementation of such a “limited contention” lock. We initialize the dualqueue with k tokens, each of which grants permission to contend for the test-and-set lock. The value of k determines the balance between fairness and locality. The acquire operation first dequeues a token from the dualqueue and then contends for the test-and-set lock. The release operation enqueues a token in the dualqueue and releases the test-and-set lock. Starvation is still possible, though less likely than with an ordinary test-and-set lock. We can eliminate it entirely, if desired, by reducing k to one on a periodic basis.

有限竞争锁。正如 Radovic 和 Hagersten 所指出的，在非统一内存访问（分布式共享内存）多处理器上，基于队列的锁的严格公平性可能并不可取。同时，当线程之间的争用较高时，倾向于将请求授予物理上相邻的线程的测试和设置锁可能是不可接受的不公平（更不用说慢了）：物理上相距较远的线程可能会饿死。一个有吸引力的折衷方案是允许等待的线程在一定程度上绕过队列中的其他线程。与测试和设置锁配对的 dualqueue 提供了这种“有限竞争”锁的直接实现。我们使用 k 个令牌初始化 dualqueue，每个令牌都授予争用测试和设置锁的权限。k 值决定了公平性和局部性之间的平衡。acquire 操作首先从 dualqueue 中取出令牌，然后争夺测试和设置锁。释放操作将令牌排入dualqueue，并释放测试和设置锁。饥饿仍然是可能的，尽管不像普通的测试和设置锁那么容易。如果需要的话，我们可以通过周期性地将 k 减少到 1 来完全消除它。

## 4 Experimental Results 
（待补充）


## Conclusions
Linearizability is central to the study of concurrent data structures. It has historically been limited by its restriction to methods that are total. We have shown how to encompass partial methods by introducing a pair of linearization points, one for the registration of a request and the other for its later fulfillment. By reasoning separately about a request, its successful follow-up, and the period in-between, we obtain meaningful definitions of wait-free, lock-free, and obstruction-free implementations of concurrent objects with condition synchronization.

线性化是并发数据结构研究的核心。它在历史上一直受到限制，仅限于完全的方法。我们已经展示了如何通过引入一对线性化点来包含部分方法，一个用于注册请求，另一个用于稍后的实现。通过分别对请求、请求的成功后续以及请求之间的时间段进行推理，我们获得了具有条件同步的并发对象的无等待、无锁和无障碍实现的有意义的定义。

We have presented concrete lock-free implementations of a dualstack and a dualqueue. Performance results on a commercial multiprocessor suggest that dualism can yield significant performance gains over naive retry on failure. The dualqueue, in particular, appears to be an eminently useful algorithm, outperforming the M&S queue in our experiments by almost a factor of two for large thread counts.

我们已经介绍了 dualstack 和 dualqueue 的具体无锁实现。在商用多处理器上的性能结果表明，与单纯的失败重试相比，二元论可以产生显著的性能增益。特别是 dualqueue，它似乎是一种非常有用的算法，在我们的实验中，对于大线程数，它的性能几乎比 M&S 队列高出两倍。

Nonblocking dual data structures could undoubtedly be developed for double-ended queues, priority queues, sets, dictionaries, and other abstractions. Each of these may in turn have variants that embody different policies as to which of several pending requests to fulfill when a matching operation makes a precondition true. One could imagine, for example, a stack that grants pending requests in FIFO order, or (conceivably) a queue that grants them in LIFO order. More plausibly, one could imagine an arbitrary system of thread priorities, in which a matching operation fulfills the highest priority pending request.

毫无疑问，可以为双端队列、优先级队列、集合、字典和其他抽象开发非阻塞双数据结构。其中每一个都可能有变体，这些变体体现了不同的策略，即当匹配操作使前提条件为真时，要满足几个挂起的请求中的哪一个。例如，我们可以想象一个堆栈以 FIFO 顺序授予挂起的请求，或者（可以想象）一个队列以LIFO 顺序授予它们。更合理的是，我们可以想象一个线程优先级的任意系统，其中匹配操作满足最高优先级的挂起请求。

Further useful structures may be obtained by altering behavior between a request and its subsequent successful follow-up. As noted in Section 2.3, one could deschedule waiting threads, thereby effectively incorporating scheduler-based condition synchronization into nonblocking data structures. For real-time or database systems, one might combine dualism with timeout, allowing a spinning thread to remove its request from the structure if it waits “too long”.

通过改变请求及其后续成功跟进之间的行为，可以获得更多有用的结构。如第2.3节所述，可以重新调度等待线程，从而有效地将基于调度器的条件同步合并到非阻塞数据结构中。对于实时或数据库系统，可以将双重性与超时结合起来，允许旋转线程在等待“太长”时从结构中删除其请求。