# Synchronization and the Java Memory Model

Consider the tiny class, defined without any synchronization:
```
    final class SetCheck {
        private int  a = 0;
        private long b = 0;

        void set() {
            a =  1;
            b = -1;
        }

        boolean check() {
            return ((b == 0) || (b == -1 && a == 1)); 
        }
    }
```

In a purely sequential language, the method check could never return false. This holds even though compilers, run-time systems, and hardware might process this code in a way that you might not intuitively expect. For example, any of the following might apply to the execution of method set:

在一个纯粹的顺序语言中，方法检查不可能返回错误。即使编译器、运行时系统和硬件可能会以一种你可能没有直观预期的方式来处理这段代码，这一点仍然成立。例如，以下任何一种情况都可能适用于方法集的执行。

+ The compiler may rearrange the order of the statements, so b may be assigned before a. If the method is inlined, the compiler may further rearrange the orders with respect to yet other statements. (编译器可能会重新安排语句的顺序，所以 b 可能被分配在a之前。如果方法是内联的，编译器可能会进一步重新安排与其他语句有关的顺序。)
+ The processor may rearrange the execution order of machine instructions corresponding to the statements, or even execute them at the same time. (处理器可以重新安排与语句相对应的机器指令的执行顺序，甚至可以同时执行这些指令。)
+ The memory system (as governed by cache control units) may rearrange the order in which writes are committed to memory cells corresponding to the variables. These writes may overlap with other computations and memory actions. (存储器系统（由高速缓存控制单元管理）可以重新安排写入对应变量的存储单元的顺序。这些写操作可能与其他计算和内存操作重叠。)
+ The compiler, processor, and/or memory system may interleave the machine-level effects of the two statements. For example on a 32-bit machine, the high-order word of b may be written first, followed by the write to a, followed by the write to the low-order word of b. (编译器、处理器和/或存储系统可以将这两条语句的机器级效果交错起来。例如，在 32 位机器上，可以先写 b 的高阶字，然后再写 a，再写 b 的低阶字。)
+ The compiler, processor, and/or memory system may cause the memory cells representing the variables not to be updated until sometime after (if ever) a subsequent check is called, but instead to maintain the corresponding values (for example in CPU registers) in such a way that the code still has the intended effect.(编译器、处理器和/或存储系统可以使代表变量的存储单元不被更新，直到随后的检查被调用后的某个时间（如果有的话），而是以这样一种方式保持相应的值（例如在 CPU 寄存器中），使代码仍然具有预期效果。)

In a sequential language, none of this can matter so long as program execution obeys as-if-serial semantics. Sequential programs cannot depend on the internal processing details of statements within simple code blocks, so they are free to be manipulated in all these ways. This provides essential flexibility for compilers and machines. Exploitation of such opportunities (via pipelined superscalar CPUs, multilevel caches, load/store balancing, interprocedural register allocation, and so on) is responsible for a significant amount of the massive improvements in execution speed seen in computing over the past decade. The as-if-serial property of these manipulations shields sequential programmers from needing to know if or how they take place. Programmers who never create their own threads are almost never impacted by these issues.

在顺序语言中，只要程序的执行遵从 if-serial 语义，这些都不重要。顺序程序不能依赖于简单代码块中的语句的内部处理细节，所以它们可以自由地以所有这些方式被操纵。这为编译器和机器提供了必要的灵活性。对这些机会的利用（通过流水线超标量 CPU、多级缓存、负载/存储平衡、程序间寄存器分配等等）是过去十年中计算领域执行速度大幅提升的重要原因。这些操作的 as-if-serial 属性使顺序程序员不需要知道它们是否发生或如何发生。那些从不创建自己的线程的程序员几乎不会受到这些问题的影响。

Things are different in concurrent programming. Here, it is entirely possible for check to be called in one thread while set is being executed in another, in which case the check might be "spying" on the optimized execution of set. And if any of the above manipulations occur, it is possible for check to return false. For example, as detailed below, check could read a value for the long b that is neither 0 nor -1, but instead a half-written in-between value. Also, out-of-order execution of the statements in set may cause check to read b as -1 but then read a as still 0.

在并发编程中，情况是不同的。在这里，完全有可能在一个线程中调用 check，而 set 正在另一个线程中执行，在这种情况下，check 可能是在 "偷窥" set 的优化执行。而如果发生了上述的任何操作，check 就有可能返回 false。例如，正如下面所详述的，check 可以为长条 b 读取一个既不是 0 也不是 -1 的值，而是一个写了一半的中间值。另外，set 中语句的失序执行也可能导致 check 将 b 读成 -1，但随后将 a 读成 0。

In other words, not only may concurrent executions be interleaved, but they may also be reordered and otherwise manipulated in an optimized form that bears little resemblance to their source code. As compiler and run-time technology matures and multiprocessors become more prevalent, such phenomena become more common. They can lead to surprising results for programmers with backgrounds in sequential programming (in other words, just about all programmers) who have never been exposed to the underlying execution properties of allegedly sequential code. This can be the source of subtle concurrent programming errors.

换句话说，不仅并发的执行可以交错进行，而且还可以重新排序，并以优化的形式进行操作，与源代码几乎没有相似之处。随着编译器和运行时技术的成熟和多处理器的普及，这种现象变得越来越普遍。对于具有顺序编程背景的程序员（换句话说，几乎所有的程序员）来说，他们可能会导致令人惊讶的结果，因为他们从未接触过所谓的顺序代码的基本执行属性。这可能是微妙的并发式编程错误的来源。

In almost all cases, there is an obvious, simple way to avoid contemplation of all the complexities arising in concurrent programs due to optimized execution mechanics: Use synchronization. For example, if both methods in class SetCheck are declared as synchronized, then you can be sure that no internal processing details can affect the intended outcome of this code.

几乎在所有情况下，都有一个明显的、简单的方法来避免考虑并发程序中由于优化执行机制而产生的所有复杂问题。使用同步化。例如，如果 SetCheck 类中的两个方法都被声明为同步的，那么你可以确信没有任何内部处理细节可以影响这段代码的预期结果。

But sometimes you cannot or do not want to use synchronization. Or perhaps you must reason about someone else's code that does not use it. In these cases you must rely on the minimal guarantees about resulting semantics spelled out by the Java Memory Model. This model allows the kinds of manipulations listed above, but bounds their potential effects on execution semantics and additionally points to some techniques programmers can use to control some aspects of these semantics (most of which are discussed in §2.4).

但有时你不能或不想使用同步化。或者你必须对别人的代码进行推理，而这些代码并没有使用它。在这些情况下，你必须依靠 Java 内存模型所阐述的关于结果语义的最小保证。这个模型允许上面列出的各种操作，但限定了它们对执行语义的潜在影响，另外还指出了一些程序员可以用来控制这些语义的某些方面的技术（其中大部分将在第2.4 节讨论）。

The Java Memory Model is part of The JavaTM Language Specification, described primarily in JLS chapter 17. Here, we discuss only the basic motivation, properties, and programming consequences of the model. The treatment here reflects a few clarifications and updates that are missing from the first edition of JLS.

Java内存模型是《JavaTM语言规范》的一部分，主要在 JLS 第 17 章中描述。在这里，我们只讨论该模型的基本动机、属性和编程结果。这里的处理反映了JLS第一版中缺少的一些澄清和更新。

The assumptions underlying the model can be viewed as an idealization of a standard SMP machine of the sort described in §1.2.4:

该模型所依据的假设可以被看作是第 1.2.4 节中描述的那种标准 SMP 机器的理想化。

For purposes of the model, every thread can be thought of as running on a different CPU from any other thread. Even on multiprocessors, this is infrequent in practice, but the fact that this CPU-per-thread mapping is among the legal ways to implement threads accounts for some of the model's initially surprising properties. For example, because CPUs hold registers that cannot be directly accessed by other CPUs, the model must allow for cases in which one thread does not know about values being manipulated by another thread. However, the impact of the model is by no means restricted to multiprocessors. The actions of compilers and processors can lead to identical concerns even on single-CPU systems.

就该模型而言，每个线程都可以被认为是运行在与其他线程不同的 CPU 上。即使在多处理器上，这种情况在实践中也不常见，但这种 CPU -每线程的映射是实现线程的合法方式之一，这也是该模型最初令人惊讶的一些特性。例如，由于 CPU 持有的寄存器不能被其他 CPU 直接访问，该模型必须允许一个线程不知道另一个线程正在操纵的值的情况。然而，该模型的影响决不限于多处理器。即使在单 CPU 系统上，编译器和处理器的行为也会导致相同的问题。

The model does not specifically address whether the kinds of execution tactics discussed above are performed by compilers, CPUs, cache controllers, or any other mechanism. It does not even discuss them in terms of classes, objects, and methods familiar to programmers. Instead, the model defines an abstract relation between threads and main memory. Every thread is defined to have a working memory (an abstraction of caches and registers) in which to store values. The model guarantees a few properties surrounding the interactions of instruction sequences corresponding to methods and memory cells corresponding to fields. Most rules are phrased in terms of when values must be transferred between the main memory and per-thread working memory. The rules address three intertwined issues:

该模型没有具体讨论上面讨论的各种执行策略是否由编译器、CPU、缓存控制器或任何其他机制执行。它甚至没有用程序员熟悉的类、对象和方法来讨论它们。相反，该模型定义了线程和主内存之间的抽象关系。每个线程都被定义为有一个工作存储器（缓存和寄存器的抽象），可以在其中存储值。该模型保证了围绕对应于方法的指令序列和对应于字段的内存单元之间的相互作用的一些属性。大多数规则都是以值必须在主内存和每线程工作内存之间转移的时间来表述的。这些规则解决了三个相互交织的问题。

+ Atomicity
Which instructions must have indivisible effects. For purposes of the model, these rules need to be stated only for simple reads and writes of memory cells representing fields - instance and static variables, also including array elements, but not including local variables inside methods.

哪些指令必须有不可分割的效果。为了模型的目的，这些规则只需要对代表字段的内存单元的简单读写进行说明--实例变量和静态变量，也包括数组元素，但不包括方法中的局部变量。

+ Visibility
Under what conditions the effects of one thread are visible to another. The effects of interest here are writes to fields, as seen via reads of those fields.

在什么条件下，一个线程的效果对另一个线程是可见的。这里所关注的效果是对字段的写入，通过对这些字段的读取来观察。

+ Ordering
Under what conditions the effects of operations can appear out of order to any given thread. The main ordering issues surround reads and writes associated with sequences of assignment statements.

在什么条件下，操作的效果对任何给定的线程来说都会出现失序。主要的排序问题围绕着与赋值语句序列相关的读和写。

When synchronization is used consistently, each of these properties has a simple characterization: All changes made in one synchronized method or block are atomic and visible with respect to other synchronized methods and blocks employing the same lock, and processing of synchronized methods or blocks within any given thread is in program-specified order. Even though processing of statements within blocks may be out of order, this cannot matter to other threads employing synchronization.

当同步化被持续使用时，这些属性中的每一个都有一个简单的特征。在一个同步方法或块中所做的所有改变都是原子性的，并且对于采用相同锁的其他同步方法和块来说是可见的，并且在任何给定的线程中对同步方法或块的处理都是按照程序指定的顺序进行的。即使块内语句的处理可能不按顺序进行，但这对其他采用同步的线程来说并不重要。

When synchronization is not used or is used inconsistently, answers become more complex. The guarantees made by the memory model are weaker than most programmers intuitively expect, and are also weaker than those typically provided on any given JVM implementation. This imposes additional obligations on programmers attempting to ensure the object consistency relations that lie at the heart of exclusion practices: Objects must maintain invariants as seen by all threads that rely on them, not just by the thread performing any given state modification.

当不使用或不一致地使用同步时，答案变得更加复杂。内存模型提供的保证比大多数程序员直觉上期望的要弱，也比任何特定 JVM 实现上通常提供的要弱。这给试图确保对象一致性关系的程序员带来了额外的义务，而对象一致性关系是排除法的核心。对象必须保持所有依赖它们的线程所看到的不变性，而不仅仅是执行任何特定状态修改的线程。

The most important rules and properties specified by the model are discussed below.

下面将讨论该模型规定的最重要的规则和属性。

**Atomicity**
Accesses and updates to the memory cells corresponding to fields of any type except long or double are guaranteed to be atomic. This includes fields serving as references to other objects. Additionally, atomicity extends to volatile long and double. (Even though non-volatile longs and doubles are not guaranteed atomic, they are of course allowed to be.)

对除 long 或 double 以外的任何类型的字段对应的内存单元的访问和更新都保证是原子性的。这包括作为其他对象的引用的字段。此外，原子性还延伸到 volatile 的long 和 double 变量。(尽管非 volatile 的 long 和 double 不被保证是原子的，但它们当然被允许是原子的）。

Atomicity guarantees ensure that when a non-long/double field is used in an expression, you will obtain either its initial value or some value that was written by some thread, but not some jumble of bits resulting from two or more threads both trying to write values at the same time. However, as seen below, atomicity alone does not guarantee that you will get the value most recently written by any thread. For this reason, atomicity guarantees per se normally have little impact on concurrent program design.

原子性保证了在表达式中使用非 long/double 字段时，你将获得它的初始值或由某个线程写入的某个值，而不是由两个或多个线程同时试图写入值而产生的一些杂乱的位。然而，正如下面所看到的，仅仅是原子性并不能保证你会得到任何线程最近写入的值。由于这个原因，原子性保证本身通常对并发程序设计没有什么影响。

**Visibility**
Changes to fields made by one thread are guaranteed to be visible to other threads only under the following conditions:

只有在以下条件下，一个线程对字段所做的改变才能保证对其他线程可见。

+ A writing thread releases a synchronization lock and a reading thread subsequently acquires that same synchronization lock.

一个写线程释放了一个同步锁，一个读线程随后获得了同一个同步锁。

In essence, releasing a lock forces a flush of all writes from working memory employed by the thread, and acquiring a lock forces a (re)load of the values of accessible fields. While lock actions provide exclusion only for the operations performed within a synchronized method or block, these memory effects are defined to cover all fields used by the thread performing the action.
  
从本质上讲，释放一个锁会迫使线程从工作内存中刷新所有的写操作，而获取一个锁会迫使（重新）加载可访问字段的值。虽然锁动作只为在同步方法或块中执行的操作提供排除，但这些内存效应被定义为涵盖执行动作的线程所使用的所有字段。

Note the double meaning of synchronized: it deals with locks that permit higher-level synchronization protocols, while at the same time dealing with the memory system (sometimes via low-level memory barrier machine instructions) to keep value representations in synch across threads. This reflects one way in which concurrent programming bears more similarity to distributed programming than to sequential programming. The latter sense of synchronized may be viewed as a mechanism by which a method running in one thread indicates that it is willing to send and/or receive changes to variables to and from methods running in other threads. From this point of view, using locks and passing messages might be seen merely as syntactic variants of each other.

请注意同步的双重含义：它处理允许更高级别的同步协议的锁，同时处理内存系统（有时通过低级别的内存屏障机器指令）以保持跨线程的值表示的同步。这反映了并发编程与分布式编程比与顺序编程更相似的一种方式。后者意义上的同步可以被看作是一种机制，通过这种机制，在一个线程中运行的方法表明它愿意向其他线程中运行的方法发送和/或接收对变量的改变。从这个角度来看，使用锁和传递消息可能仅仅被看作是彼此的语法变体。

+ If a field is declared as volatile, any value written to it is flushed and made visible by the writer thread before the writer thread performs any further memory operation (i.e., for the purposes at hand it is flushed immediately). Reader threads must reload the values of volatile fields upon each access.

如果一个字段被声明为 volatile，那么在写入者线程执行任何进一步的内存操作之前，写入它的任何值都会被写入者线程刷新并变得可见（也就是说，就目前的目的而言，它被立即刷新）。读者线程必须在每次访问时重新加载volatile 字段的值。

+ The first time a thread accesses a field of an object, it sees either the initial value of the field or a value since written by some other thread.

当一个线程第一次访问一个对象的字段时，它看到的要么是该字段的初始值，要么是由其他线程写入的值。

Among other consequences, it is bad practice to make available the reference to an incompletely constructed object (see §2.1.2). It can also be risky to start new threads inside a constructor, especially in a class that may be subclassed. Thread.start has the same memory effects as a lock release by the thread calling start, followed by a lock acquire by the started thread. If a Runnable superclass invokes new Thread(this).start() before subclass constructors execute, then the object might not be fully initialized when the run method executes. Similarly, if you create and start a new thread T and then create an object X used by thread T, you cannot be sure that the fields of X will be visible to T unless you employ synchronization surrounding all references to object X. Or, when applicable, you can create X before starting T.

在其他后果中，让一个未完全构建的对象的引用可用是不好的做法（见§2.1.2）。在构造函数中启动新线程也是有风险的，特别是在一个可能被子类化的类中。Thread.start 的内存效果与调用 start 的线程释放锁，然后由被启动的线程获取锁的效果相同。如果一个 Runnable 超类在子类构造函数执行之前调用 new Thread(this).start()，那么当运行方法执行时，该对象可能没有被完全初始化。同样，如果你创建并启动一个新的线程 T，然后创建一个被线程 T 使用的对象 X，你不能确保 X 的字段对 T 来说是可见的，除非你采用围绕对象 X 的所有引用的同步。

+ As a thread terminates, all written variables are flushed to main memory. For example, if one thread synchronizes on the termination of another thread using Thread.join, then it is guaranteed to see the effects made by that thread (see §4.3.2).

当一个线程终止时，所有写入的变量都会被刷新到主内存。例如，如果一个线程使用 Thread.join 对另一个线程的终止进行同步，那么它就能保证看到该线程做出的效果（见§4.3.2）。

Note that visibility problems never arise when passing references to objects across methods in the same thread.

请注意，在同一线程中跨方法传递对对象的引用时，绝不会出现可见性问题。

The memory model guarantees that, given the eventual occurrence of the above operations, a particular update to a particular field made by one thread will eventually be visible to another. But eventually can be an arbitrarily long time. Long stretches of code in threads that use no synchronization can be hopelessly out of synch with other threads with respect to values of fields. In particular, it is always wrong to write loops waiting for values written by other threads unless the fields are volatile or accessed via synchronization (see §3.2.6).

内存模型保证，考虑到上述操作的最终发生，一个线程对特定字段的特定更新最终会对另一个线程可见。但最终可能是一个任意长的时间。在不使用同步的线程中，长长的一段代码可能会与其他线程的字段值完全不同步，令人绝望。特别是，写循环等待其他线程写的值总是错误的，除非这些字段是 volatile 的或通过同步访问（见§3.2.6）。

The model also allows inconsistent visibility in the absence of synchronization. For example, it is possible to obtain a fresh value for one field of an object, but a stale value for another. Similarly, it is possible to read a fresh, updated value of a reference variable, but a stale value of one of the fields of the object now being referenced.

该模型还允许在没有同步的情况下有不一致的可见性。例如，有可能获得一个对象的一个字段的新值，但另一个字段的值是过时的。同样地，有可能读取一个引用变量的最新值，但现在被引用的对象的一个字段的过时值。

However, the rules do not require visibility failures across threads, they merely allow these failures to occur. This is one aspect of the fact that not using synchronization in multithreaded code doesn't guarantee safety violations, it just allows them. On most current JVM implementations and platforms, even those employing multiple processors, detectable visibility failures rarely occur. The use of common caches across threads sharing a CPU, the lack of aggressive compiler-based optimizations, and the presence of strong cache consistency hardware often cause values to act as if they propagate immediately among threads. This makes testing for freedom from visibility-based errors impractical, since such errors might occur extremely rarely, or only on platforms you do not have access to, or only on those that have not even been built yet. These same comments apply to multithreaded safety failures more generally. Concurrent programs that do not use synchronization fail for many reasons, including memory consistency problems.

然而，这些规则并不要求跨线程的可见性故障，它们只是允许这些故障的发生。这是一个方面，在多线程代码中不使用同步并不能保证安全违规，它只是允许它们发生。在当前大多数JVM实现和平台上，即使是那些采用多处理器的JVM，也很少发生可检测的可见性故障。在共享CPU的线程中使用共同的缓存，缺乏积极的基于编译器的优化，以及强大的缓存一致性硬件的存在，往往会导致数值在线程间立即传播。这使得对基于可见性的错误的测试变得不切实际，因为这种错误可能极少发生，或者只发生在你无法访问的平台上，或者只发生在那些甚至还没有被建立的平台上。这些评论同样适用于多线程的安全故障，更普遍。不使用同步的并发程序由于许多原因而失败，包括内存一致性问题。

**Ordering**
Ordering rules fall under two cases, within-thread and between-thread:
排序规则分为两种情况，线程内和线程间。

+ From the point of view of the thread performing the actions in a method, instructions proceed in the normal as-if-serial manner that applies in sequential programming languages.

从执行方法中动作的线程的角度来看，指令以顺序编程语言中正常的 if-serial 方式进行。

+ From the point of view of other threads that might be "spying" on this thread by concurrently running unsynchronized methods, almost anything can happen. The only useful constraint is that the relative orderings of synchronized methods and blocks, as well as operations on volatile fields, are always preserved.

从其他可能通过并发运行非同步方法 "偷窥 "这个线程的角度来看，几乎什么都可能发生。唯一有用的约束是，同步方法和块的相对顺序，以及对 volatile 字段的操作，总是被保留下来。

Again, these are only the minimal guaranteed properties. In any given program or platform, you may find stricter orderings. But you cannot rely on them, and you may find it difficult to test for code that would fail on JVM implementations that have different properties but still conform to the rules.

同样，这些只是最小的保证属性。在任何特定的程序或平台中，你可能会发现更严格的排序。但你不能依赖它们，而且你可能会发现很难测试那些在 JVM 实现上会失败的代码，这些实现有不同的属性，但仍然符合规则。

Note that the within-thread point of view is implicitly adopted in all other discussions of semantics in JLS. For example, arithmetic expression evaluation is performed in left-to-right order (JLS section 15.6) as viewed by the thread performing the operations, but not necessarily as viewed by other threads.

请注意，在 JLS 中关于语义的所有其他讨论中都隐含了线程内的观点。例如，在执行操作的线程看来，算术表达式的评估是以从左到右的顺序进行的（JLS 第15.6节），但其他线程不一定是这样看的。

The within-thread as-if-serial property is helpful only when only one thread at a time is manipulating variables, due to synchronization, structural exclusion, or pure chance. When multiple threads are all running unsynchronized code that reads and writes common fields, then arbitrary interleavings, atomicity failures, race conditions, and visibility failures may result in execution patterns that make the notion of as-if-serial just about meaningless with respect to any given thread.

只有当每次只有一个线程在操作变量时，线程内的 if-serial 属性才是有帮助的，这是由于同步、结构性排斥或纯粹的机会。当多个线程都在运行不同步的代码来读写共同的字段时，那么任意的交错、原子性失败、竞赛条件和可见性失败都可能导致执行模式的出现，使得 if-serial 的概念对于任何给定的线程来说都毫无意义。

Even though JLS addresses some particular legal and illegal reorderings that can occur, interactions with these other issues reduce practical guarantees to saying that the results may reflect just about any possible interleaving of just about any possible reordering. So there is no point in trying to reason about the ordering properties of such code.

即使JLS解决了一些可能发生的特定的合法和非法的重新排序，与这些其他问题的相互作用将实际的保证减少到说结果可能反映了几乎所有可能的重新排序的交织。因此，试图推理这种代码的排序属性是没有意义的。

**Volatile**
In terms of atomicity, visibility, and ordering, declaring a field as volatile is nearly identical in effect to using a little fully synchronized class protecting only that field via get/set methods, as in:

在原子性、可见性和有序性方面，将一个字段声明为 volatile，与使用一个完全同步的小类通过get/set方法只保护该字段的效果几乎相同，如：。

```
    final class VFloat {
        private float value;

        final synchronized void set(float f) { value = f; }
        final synchronized float get()      { return value; }
    }
```

Declaring a field as volatile differs only in that no locking is involved. In particular, composite read/write operations such as the "++'' operation on volatile variables are not performed atomically.

将一个字段声明为 volatile，其不同之处仅在于不涉及锁。特别是，复合读/写操作，如对易失性变量的 "++"操作，不是以原子方式进行的。

Also, ordering and visibility effects surround only the single access or update to the volatile field itself. Declaring a reference field as volatile does not ensure visibility of non-volatile fields that are accessed via this reference. Similarly, declaring an array field as volatile does not ensure visibility of its elements. Volatility cannot be manually propagated for arrays because array elements themselves cannot be declared as volatile.

而且，有序性和可见性效果只围绕对 volatile 字段本身的单一访问或更新。将一个引用字段声明为 volatile 并不确保通过该引用访问的非 volatile 字段的可见性。类似地，将一个数组字段声明为 volatile 并不能确保其元素的可见性。volatile 不能被手动传播给数组，因为数组元素本身不能被声明为 volatile。

Because no locking is involved, declaring fields as volatile is likely to be cheaper than using synchronization, or at least no more expensive. However, if volatile fields are accessed frequently inside methods, their use is likely to lead to slower performance than would locking the entire methods.

由于不涉及锁定，将字段声明为 volatile 可能比使用同步更便宜，或者至少不会更昂贵。然而，如果在方法中频繁访问 volatile 字段，使用它们可能会导致比锁定整个方法更慢的性能。

Declaring fields as volatile can be useful when you do not need locking for any other reason, yet values must be accurately accessible across multiple threads. This may occur when:

当你因为其他原因不需要锁定，但数值必须在多个线程中准确地被访问时，将字段声明为 volatile 是有用的。这可能发生在以下情况。

+ The field need not obey any invariants with respect to others. (该字段不需要遵守任何与其他字段有关的不变量。)
+ Writes to the field do not depend on its current value. (对该字段的写入不取决于其当前值。)
+ No thread ever writes an illegal value with respect to intended semantics. (就预期的语义而言，没有线程会写出一个非法的值。)
+ The actions of readers do not depend on values of other non-volatile fields. (读者的行动不取决于其他非 volatile 字段的值。)

Using volatile fields can make sense when it is somehow known that only one thread can change a field, but many other threads are allowed to read it at any time. For example, a Thermometer class might declare its temperature field as volatile. As discussed in §3.4.2, a volatile can be useful as a completion flag. Additional examples are illustrated in §4.4, where the use of lightweight executable frameworks automates some aspects of synchronization, but volatile declarations are needed to ensure that result field values are visible across tasks.

当人们知道只有一个线程可以改变一个字段，但其他许多线程可以在任何时候读取它时，使用 volatile 字段是有意义的。例如，一个温度计类可以将其温度字段声明为 volatile。正如在§3.4.2中所讨论的，volatile 可以作为一个完成标志来使用。其他的例子在§4.4中说明，使用轻量级的可执行框架可以使同步的某些方面自动化，但需要 volatile 声明来确保结果字段的值在不同的任务中是可见的。