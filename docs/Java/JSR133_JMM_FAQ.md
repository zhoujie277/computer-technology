## JSR 133 (Java Memory Model) FAQ
Jeremy Manson and Brian Goetz, February 2004

[TOC]

### What is a memory model, anyway?
In multiprocessor systems, processors generally have one or more layers of memory cache, which improves performance both by speeding access to data (because the data is closer to the processor) and reducing traffic on the shared memory bus (because many memory operations can be satisfied by local caches.) Memory caches can improve performance tremendously, but they present a host of new challenges. What, for example, happens when two processors examine the same memory location at the same time? Under what conditions will they see the same value?

在多处理器系统中，处理器一般都有一层或多层内存缓存，通过加快数据访问速度（因为数据离处理器更近）和减少共享内存总线上的流量（因为许多内存操作可以由本地缓存满足）来提高性能。 内存缓存可以极大地提高性能，但也带来了一系列新挑战。例如，当两个处理器同时检查同一个内存位置时会发生什么？在什么条件下它们会看到相同的值？

At the processor level, a memory model defines necessary and sufficient conditions for knowing that writes to memory by other processors are visible to the current processor, and writes by the current processor are visible to other processors. Some processors exhibit a strong memory model, where all processors see exactly the same value for any given memory location at all times. Other processors exhibit a weaker memory model, where special instructions, called memory barriers, are required to flush or invalidate the lcoal processor cache in order to see writes mage by other processors or make writes by this processor visible to others. These memory barriers are usually performed when lock and unlock actions are taken; they are invisible to programmers in a high level language.

在处理器层面，内存模型定义了必要和充分的条件，即知道其他处理器对内存的写入对当前处理器是可见的，而当前处理器的写入对其他处理器是可见的。一些处理器表现出强势的内存模型，即所有处理器在任何时候都能看到任何给定内存位置的完全相同的值。其他处理器表现出一种较弱的内存模型，在这种模型中，需要有特殊的指令，称为内存屏障，来刷新或废止本地处理器的缓存，以便看到其他处理器的写入，或使本处理器的写入对其他人可见。这些内存障碍通常是在进行锁定和解锁操作时进行的；在高级语言中，程序员是看不到它们的。

It can sometimes be easier to write programs for strong memory models, because of the reduced need for memory barriers. However, even on some of the strongest memory models, memory barriers are often necessary; quite frequently their placement is counterintuitive. Recent trends in processor design have encouraged weaker memory models, because the relaxations they make for cache consistency allow for greater scalability across multiple processors and larger amounts of memory.

由于对内存屏障的需求减少，有时为强内存模型编写程序会更容易。然而，即使在一些最强的内存模型上，内存屏障也是必要的；它们的位置经常是反直觉的。最近的处理器设计趋势鼓励使用较弱的内存模型，因为它们对缓存一致性的放宽允许在多个处理器和更大的内存数量上有更大的可扩展性。

The issue of when a write becomes visible to another thread is compounded by the compiler's reordering of code. For example, the compiler might decide that it is more efficient to move a write operation later in the program; as long as this code motion does not change the program's semantics, it is free to do so.  If a compiler defers an operation, another thread will not see it until it is performed; this mirrors the effect of caching.

写操作何时对另一线程可见的问题因编译器对代码的重新排序而变得复杂。例如，编译器可能决定将一个写操作移到程序的后面，这样做更有效率；只要这种代码移动不改变程序的语义，它就可以自由地这么做。 如果编译器推迟了一个操作，另一个线程将不会看到它，直到它被执行；这反映了缓存的效果。

Moreover, writes to memory can be moved earlier in a program; in this case, other threads might see a write before it actually "occurs" in the program.  All of this flexibility is by design -- by giving the compiler, runtime, or hardware the flexibility to execute operations in the optimal order, within the bounds of the memory model, we can achieve higher performance.

此外，对内存的写入可以在程序中提前进行；在这种情况下，其他线程可能会在程序中实际 "发生 "之前看到一个写入。 所有这些灵活性都是设计出来的--通过赋予编译器、运行时或硬件灵活性，在内存模型的范围内以最佳顺序执行操作，我们可以实现更高的性能。

A sample example of this can be seen in the following code:

```
    class Reordering {
        int x = 0, y = 0;
        public void writer() {
            x = 1;
            y = 2;
        }

        public void reader() {
            int r1 = y;
            int r2 = x;
        }
    }
```

Let's say that this code is executed in two threads concurrently, and the read of y sees the value 2. Because this write came after the write to x, the programmer might assume that the read of x must see the value 1. However, the writes may have been reordered. If this takes place, then the write to y could happen, the reads of both variables could follow, and then the write to x could take place. The result would be that r1 has the value 2, but r2 has the value 0.

假设这段代码是在两个线程中同时执行的，对 y 的读取看到了值 2。因为这个写入是在对 x 的写入之后，程序员可能认为对 x 的读取必须看到值 1。然而，写的内容可能被重新排序了。如果这发生了，那么对 y 的写可能会发生，两个变量的读可能会随之发生，然后对 x 的写可能会发生。结果是 r1 的值是 2，但 r2 的值是 0。

The Java Memory Model describes what behaviors are legal in multithreaded code, and how threads may interact through memory. It describes the relationship between variables in a program and the low-level details of storing and retrieving them to and from memory or registers in a real computer system. It does this in a way that can be implemented correctly using a wide variety of hardware and a wide variety of compiler optimizations.

Java 内存模型描述了多线程代码中哪些行为是合法的，以及线程如何通过内存进行交互。它描述了程序中变量之间的关系，以及在真实的计算机系统中存储和检索这些变量的低级细节，以及从内存或寄存器中的存储和检索。它以一种可以使用各种硬件和各种编译器优化的方式来正确实现这一点。

Java includes several language constructs, including volatile, final, and synchronized, which are intended to help the programmer describe a program's concurrency requirements to the compiler. The Java Memory Model defines the behavior of volatile and synchronized, and, more importantly, ensures that a correctly synchronized Java program runs correctly on all processor architectures.

Java 包括几个语言结构，包括 volatile、final 和 synchronized，其目的是帮助程序员向编译器描述程序的并发需求。Java 内存模型定义了 volatile 和 synchronized 的行为，更重要的是，它能确保一个正确的同步 Java 程序在所有处理器架构上正确运行。

### Do other languages, like C++, have a memory model?
大多数其他编程语言，如 C 和 C++，在设计上并不直接支持多线程。这些语言对发生在编译器和架构中的各种重排的保护在很大程度上取决于所使用的线程库（如 pthreads）、所使用的编译器以及代码运行的平台所提供的保证。 

### What is JSR 133 about?
Since 1997, several serious flaws have been discovered in the Java Memory Model as defined in Chapter 17 of the Java Language Specification. These flaws allowed for confusing behaviors (such as final fields being observed to change their value) and undermined the compiler's ability to perform common optimizations.

自1997年以来，在《Java语言规范》第17章中定义的 Java 内存模型中发现了几个严重的缺陷。这些缺陷允许出现令人困惑的行为（如 final 字段被观察到改变其值），并破坏了编译器执行普通优化的能力。

The Java Memory Model was an ambitious undertaking; it was the first time that a programming language specification attempted to incorporate a memory model which could provide consistent semantics for concurrency across a variety of architectures. Unfortunately, defining a memory model which is both consistent and intuitive proved far more difficult than expected. JSR 133 defines a new memory model for the Java language which fixes the flaws of the earlier memory model. In order to do this, the semantics of final and volatile needed to change.

Java 内存模型是一项雄心勃勃的工作；这是第一次有编程语言规范试图纳入一个内存模型，它可以为各种架构的并发性提供一致的语义。不幸的是，定义一个既一致又直观的内存模型被证明比预期的要困难得多。JSR 133为 Java 语言定义了一个新的内存模型，修正了早期内存模型的缺陷。为了做到这一点，final 和 volatile 的语义需要改变。

The full semantics are available at http://www.cs.umd.edu/users/pugh/java/memoryModel, but the formal semantics are not for the timid. It is surprising, and sobering, to discover how complicated seemingly simple concepts like synchronization really are. Fortunately, you need not understand the details of the formal semantics -- the goal of JSR 133 was to create a set of formal semantics that provides an intuitive framework for how volatile, synchronized, and final work.

完整的语义可在 http://www.cs.umd.edu/users/pugh/java/memoryModel，但正式的语义并不适合 timid。发现像同步这样看似简单的概念到底有多复杂，是令人惊讶的，也是令人清醒的。幸运的是，你不需要理解形式语义的细节-- JSR 133的目标是创建一套形式语义，为 volatile、synchronized 和 final 的工作提供一个直观的框架。

The goals of JSR 133 include:
+ Preserving existing safety guarantees, like type-safety, and strengthening others. For example, variable values may not be created "out of thin air": each value for a variable observed by some thread must be a value that can reasonably be placed there by some thread.
+ The semantics of correctly synchronized programs should be as simple and intuitive as possible.
+ The semantics of incompletely or incorrectly synchronized programs should be defined so that potential security hazards are minimized.
+ Programmers should be able to reason confidently about how multithreaded programs interact with memory.
+ It should be possible to design correct, high performance JVM implementations across a wide range of popular hardware architectures.
+ A new guarantee of initalization safety should be provided. If an object is properly constructed(which means that references to it do not escape during construction), then all threads which see a reference to that object will also see the values for its final fields that were set in the constructor, without the need for synchronization.
+ There should be minimal impact on existing code.

+ 保留现有的安全保障，如类型安全，并加强其他保障。例如，变量值不能 "凭空 "产生：某个线程观察到的每个变量值必须是某个线程可以合理放置的值。
+ 正确的同步程序的语义应该是尽可能简单和直观的。
+ 应该对不完全或不正确的同步程序的语义进行定义，以便将潜在的安全隐患降到最低。
+ 程序员应该能够自信地推理出多线程程序如何与内存互动。
+ 应该可以在广泛的流行硬件架构上设计出正确的、高性能的 JVM 实现。
+ 应该提供一个新的初始化安全保证。如果一个对象是正确构造的（这意味着对它的引用在构造过程中没有逃脱），那么所有看到对该对象的引用的线程也将看到在构造函数中设置的最终字段的值，而不需要同步。
+ 对现有法规的影响应该是最小的。

### What is meant by reordering?
There are a number of cases in which accesses to program variables (object instance fields, class static fields, and array elements) may appear to execute in a different order than was specified by the program. The compiler is free to take liberties with the ordering of instructions in the name of optimization. Processors may execute instructions out of order under certain circumstances. Data may be moved between registers, processor caches, and main memory in different order than specified by the program.

在很多情况下，对程序变量（对象实例字段、类静态字段和数组元素）的访问可能会出现与程序指定的不同的执行顺序。编译器可以以优化的名义自由地对指令的顺序进行调整。在某些情况下，处理器可以不按顺序执行指令。数据在寄存器、处理器缓存和主内存之间的移动顺序可能与程序规定的不同。

For example, if a thread writes to field a and then to field b, and the value of b does not depend on the value of a, then the compiler is free to reorder these operations, and the cache is free to flush b to main memory before a. There are a number of potential sources of reordering, such as the compiler, the JIT, and the cache.

例如，如果一个线程先写到字段 a，然后再写到字段 b，而 b 的值并不取决于 a 的值，那么编译器可以自由地重新排序这些操作，缓存也可以自由地在 a 之前将 b 刷入主内存。

The compiler, runtime, and hardware are supposed to conspire to create the illusion of as-if-serial semantics, which means that in a single-threaded program, the program should not be able to observe the effects of reorderings. However, reorderings can come into play in incorrectly synchronized multithreaded programs, where one thread is able to observe the effects of other threads, and may be able to detect that variable accesses become visible to other threads in a different order than executed or specified in the program.

编译器、运行时和硬件应该合力创造一个假序列语义的假象，这意味着在单线程程序中，程序不应该能够观察到重排序的影响。然而，在不正确的同步多线程程序中，重新排序可能会起作用，其中一个线程能够观察到其他线程的影响，并且可能能够检测到变量访问对其他线程的可见性，其顺序与程序中执行或指定的顺序不同。

Most of the time, one thread doesn't care what the other is doing. But when it does, that's what synchronization is for.

大多数时候，一个线程并不关心另一个线程在做什么。但当它关心时，这就是同步的作用。

### What was wrong with the old memory model?
There were several serious problems with the old memory model. It was difficult to understand, and therefore widely violated. For example, the old model did not, in many cases, allow the kinds of reorderings that took place in every JVM. This confusion about the implications of the old model was what compelled the formation of JSR-133.

旧的内存模型有几个严重的问题。它很难理解，因此被广泛违反。例如，在许多情况下，旧模型不允许在每个JVM 中发生的各种重新排序。这种对旧模型的影响的困惑，迫使 JSR-133 的形成。

One widely held belief, for example, was that if final fields were used, then synchronization between threads was unnecessary to guarantee another thread would see the value of the field. While this is a reasonable assumption and a sensible behavior, and indeed how we would want things to work, under the old memory model, it was simply not true. Nothing in the old memory model treated final fields differently from any other field -- meaning synchronization was the only way to ensure that all threads see the value of a final field that was written by the constructor. As a result, it was possible for a thread to see the default value of the field, and then at some later time see its constructed value. This means, for example, that immutable objects like String can appear to change their value -- a disturbing prospect indeed.

例如，人们普遍认为，如果使用 final 字段，那么线程之间的同步就没有必要保证另一个线程会看到该字段的值。虽然这是一个合理的假设和明智的行为，而且确实是我们希望的工作方式，但在旧的内存模型下，这根本不是真的。旧的内存模型中没有任何东西将 final 字段与其他字段区别对待--这意味着同步是确保所有线程看到由构造函数写入的最终字段的值的唯一方法。因此，一个线程有可能看到该字段的默认值，然后在稍后的时间看到其构造值。这意味着，例如，像 String 这样的不可改变的对象可以出现改变它们的值--这的确是一个令人不安的前景。

The old memory model allowed for volatile writes to be reordered with nonvolatile reads and writes, which was not consistent with most developers intuitions about volatile and therefore caused confusion.

旧的内存模型允许 volatile 写入与非 volatile 读写重新排序，这与大多数开发者对 volatile 的直觉不一致，因此造成了混乱。

Finally, as we shall see, programmers' intuitions about what can occur when their programs are incorrectly synchronized are often mistaken. One of the goals of JSR-133 is to call attention to this fact.

最后，正如我们将看到的，程序员对其程序不正确同步时可能发生的情况的直觉往往是错误的。JSR-133 的目标之一就是要唤起人们对这个事实的关注。

### What do you mean by incorrectly synchronized?
Incorrectly synchronized code can mean different things to different people. When we talk about incorrectly synchronized code in the context of the Java Memory Model, we mean any code where
1. there is a write of a variable by one thread.
2. there is a read of the same variable by another thread and
3. the write and read are not ordered by synchronization

对于不同的人来说，不正确的同步化代码可能意味着不同的事情。当我们在 Java 内存模型的背景下谈论不正确的同步代码时，我们指的是有下列情况的代码
1. 有一个线程对一个变量进行了写入。
2. 另一个线程对同一变量进行了读取，并且
3. 写和读的顺序不是同步的

When these rules are violated, we say we have a data race on that variable. A program with a data race is an incorrectly synchronized program.

当这些规则被违反时，我们说我们在该变量上有一个数据争用。一个有数据争用的程序是一个不正确的同步程序。

### What does synchronization do?
Synchronization has several aspects. The most well-understood is mutual exclusion -- only one thread can hold a monitor at once, so synchronizing on a monitor means that once one thread enters a synchronized block protected by a monitor, no other thread can enter a block protected by that monitor until the first thread exits the synchronized block.

同步有几个方面。最广为人知的是相互排斥 -- 一次只能有一个线程持有一个监视器，所以在监视器上同步意味着一旦一个线程进入一个由监视器保护的同步块，其他线程就不能进入由该监视器保护的块，直到第一个线程退出同步块。

But there is more to synchronization than mutual exclusion. Synchronization ensures that memory writes by a thread before or during a synchronized block are made visible in a predictable manner to other threads which synchronize on the same monitor. After we exit a synchronized block, we release the monitor, which has the effect of flushing the cache to main memory, so that writes made by this thread can be visible to other threads. Before we can enter a synchronized block, we acquire the monitor, which has the effect of invalidating the local processor cache so that variables will be reloaded from main memory. We will then be able to see all of the writes made visible by the previous release.

但是，同步还有比相互排斥更重要的意义。同步保证了在同步块之前或期间，一个线程的内存写入会以一种可预测的方式被其他在同一监视器上同步的线程所看到。在我们退出一个同步块后，我们会释放监视器，这样做的效果是将缓存冲到主内存中，这样这个线程所做的写入就会被其他线程看到。在我们进入一个同步块之前，我们要获取监视器，这样做的效果是使本地处理器的缓存失效，这样变量就会从主内存中重新加载。然后，我们将能够看到所有由前次发布的可见写内容。

Discussing this in terms of caches, it may sound as if these issues only affect multiprocessor machines. However, the reordering effects can be easily seen on a single processor. It is not possible, for example, for the compiler to move your code before an acquire or after a release. When we say that acquires and releases act on caches, we are using shorthand for a number of possible effects.

从缓存的角度来讨论这个问题，听起来似乎这些问题只影响到多处理器的机器。然而，在单处理器上可以很容易看到重排的效果。例如，编译器不可能将你的代码移到 acquire 之前或 release 之后。当我们说获取和释放作用于缓存时，我们使用的是一些可能的效果的简写。

The new memory model semantics create a partial ordering on memory operations (read field, write field, lock, unlock) and other thread operations (start and join), where some actions are said to happen before other operations. When one action happens before another, the first is guaranteed to be ordered before and visible to the second. The rules of this ordering are as follows:

新的内存模型语义在内存操作（读字段、写字段、锁、解锁）和其他线程操作（启动和连接）上创建了一个部分排序，其中一些操作被认为发生在其他操作之前。当一个动作发生在另一个动作之前时，第一个动作被保证在第二个动作之前排序，并且对第二个动作可见。这种排序的规则如下。

+ Each action in a thread happens before every action in that thread that comes later in the program's order.
+ An unlock on a monitor happens before every subsequent lock on that same monitor.
+ A write to a volatile field happens before every subsequent read of that same volatile.
+ A call to start() on a thread happens before any actions in the started thread.
+ All actions in a thread happen before any other thread successfully returns from a join() on that thread.

+ 一个线程中的每个动作都发生在该线程中的每个动作之前，这些动作在程序的顺序中较晚。
+ 监视器上的解锁发生在同一监视器上的每一个后续锁定之前。
+ 对一个 volatile 字段的写入发生在对同一 volatile 的每一次后续读取之前。
+ 对一个线程的start()的调用发生在被启动的线程的任何行动之前。
+ 一个线程中的所有动作都发生在任何其他线程从该线程的 join() 中成功返回之前。

This means that any memory operations which were visible to a thread before exiting a synchronized block are visible to any thread after it enters a synchronized block protected by the same monitor, since all the memory operations happen before the release, and the release happens before the acquire.

这意味着，任何线程在退出同步块之前可见的内存操作，在其进入由同一监视器保护的同步块之后都是可见的，因为所有的内存操作都发生在释放之前，而释放发生在获取之前。

Another implication is that the following pattern, which some people use to force a memory barrier, doesn't work:

另一个含义是，有些人用以下模式强制内存屏障是不起作用的。
```
    synchronized(new Object()) {}
```

This is actually a no-op, and your compiler can remove it entirely, because the compiler knows that no other thread will synchronize on the same monitor. You have to set up a happens-before relationship for one thread to see the results of another.

这实际上是一个无用功，你的编译器可以完全删除它，因为编译器知道，没有其他线程会在同一个监视器上进行同步。你必须为一个线程看到另一个线程的结果设置一个 happens-before 关系。

Important Note: Note that it is important for both threads to synchronize on the same monitor in order to set up the happens-before relationship properly. It is not the case that everything visible to thread A when it synchronizes on object X becomes visible to thread B after it synchronizes on object Y. The release and acquire have to "match" (i.e., be performed on the same monitor) to have the right semantics. Otherwise, the code has a data race.

重要提示：请注意，两个线程必须在同一个监视器上进行同步，以便正确设置 happens-before 关系。当线程 A 在对象 X 上进行同步时，所有可见的东西在线程 B 在对象 Y 上进行同步后都是可见的，这是不可能的。否则，代码中就会出现数据争用。

### How can final fields appear to change their values?
One of the best examples of how final fields' values can be seen to change involves one particular implementation of the String class.

关于 final 字段的值如何变化，最好的例子之一涉及到 String 类的一个特殊实现。

A String can be implemented as an object with three fields -- a character array, an offset into that array, and a length. The rationale for implementing String this way, instead of having only the character array, is that it lets multiple String and StringBuffer objects share the same character array and avoid additional object allocation and copying. So, for example, the method String.substring() can be implemented by creating a new string which shares the same character array with the original String and merely differs in the length and offset fields. For a String, these fields are all final fields.

一个 String 可以被实现为一个有三个字段的对象--一个字符数组、一个数组的偏移量和一个长度。这样实现String，而不是只有字符数组的理由是，它可以让多个 String 和 StringBuffer 对象共享同一个字符数组，避免额外的对象分配和复制。因此，例如，String.substring() 方法可以通过创建一个新的字符串来实现，该字符串与原始的 String 共享相同的字符数组，只是在长度和偏移量字段上有所不同。对于一个字符串，这些字段都是 final 字段。

```
    String s1 = "/usr/tmp";
    String s2 = s1.substring(4);
```

The string s2 will have an offset of 4 and a length of 4. But, under the old model, it was possible for another thread to see the offset as having the default value of 0, and then later see the correct value of 4, it will appear as if the string "/usr" changes to "/tmp".

字符串 s2 的偏移量为 4，长度为 4。但是，在旧模式下，另一个线程有可能看到偏移量的默认值为 0，后来又看到正确的值为 4，就会出现字符串"/usr" 变为 "/tmp" 的情况。

The original Java Memory Model allowed this behavior; several JVMs have exhibited this behavior. The new Java Memory Model makes this illegal.

最初的Java内存模型允许这种行为；一些 JVM 已经表现出这种行为。新的 Java 内存模型将这种行为定为非法。

### How do final fields work under the new JMM?
The values for an object's final fields are set in its constructor. Assuming the object is constructed "correctly", once an object is constructed, the values assigned to the final fields in the constructor will be visible to all other threads without synchronization. In addition, the visible values for any other object or array referenced by those final fields will be at least as up-to-date as the final fields.

一个对象的 final 字段的值是在其构造函数中设置的。假设对象的构造是 "正确 "的，一旦对象被构造出来，在构造函数中分配给 final 字段的值将对所有其他线程可见，而不需要同步。此外，由这些 final 字段引用的任何其他对象或数组的可见值将至少与最终字段一样是最新的。

What does it mean for an object to be properly constructed? It simply means that no reference to the object being constructed is allowed to "escape" during construction. (See Safe Construction Techniques for examples.)  In other words, do not place a reference to the object being constructed anywhere where another thread might be able to see it; do not assign it to a static field, do not register it as a listener with any other object, and so on. These tasks should be done after the constructor completes, not in the constructor.

一个对象被正确构造是什么意思？简单地说，就是在构建过程中，不允许对正在构建的对象的引用 "逃跑"。(换句话说，不要把正在构建的对象的引用放在其他线程可能看到的地方；不要把它分配给静态字段，不要把它注册为任何其他对象的监听器，等等。这些任务应该在构造函数完成后完成，而不是在构造函数中完成。

```
    class FinalFieldExample {
        final int x;
        int y;
        static FinalFieldExample f;
        public FinalFieldExample() {
            x = 3;
            y = 4;
        }

        static void writer() {
            f = new FinalFieldExample();
        }

        static void reader() {
            if (f != null) {
                int i = f.x;
                int j = f.y;
            }
        }
    }
```

The class above is an example of how final fields should be used. A thread executing reader is guaranteed to see the value 3 for f.x, because it is final. It is not guaranteed to see the value 4 for y, because it is not final. If FinalFieldExample's constructor looked like this:

上面的类是一个应该如何使用 final 字段的例子。一个执行 reader 的线程保证能看到 f.x 的值 3，因为它是final。它不能保证看到 y 的值 4，因为它不是 final。如果 FinalFieldExample 的构造函数看起来像这样。

```
    public FinalFieldExample() { // bad !
        x = 3;
        y = 4;
        // bad construction - allowing this to escape
        global.obj = this;
    }
```

then threads that read the reference to this from global.obj are not guaranteed to see 3 for x.

那么从 global.obj 读取这个引用的线程就不能保证看到 x 的 3。

The ability to see the correctly constructed value for the field is nice, but if the field itself is a reference, then you also want your code to see the up to date values for the object (or array) to which it points. If your field is a final field, this is also guaranteed. So, you can have a final pointer to an array and not have to worry about other threads seeing the correct values for the array reference, but incorrect values for the contents of the array. Again, by "correct" here, we mean "up to date as of the end of the object's constructor", not "the latest value available".

能够看到字段的正确构造值是很好的，但是如果字段本身是一个引用，那么你也希望你的代码能够看到它所指向的对象（或数组）的最新值。如果你的字段是一个 final 字段，这也是保证。因此，你可以有一个指向数组的最终指针，而不必担心其他线程看到数组引用的正确值，但数组内容的不正确值。同样，这里的 "正确 "是指 "截至对象构造函数结束时的最新值"，而不是 "可用的最新值"。

Now, having said all of this, if, after a thread constructs an immutable object (that is, an object that only contains final fields), you want to ensure that it is seen correctly by all of the other thread, you still typically need to use synchronization. There is no other way to ensure, for example, that the reference to the immutable object will be seen by the second thread. The guarantees the program gets from final fields should be carefully tempered with a deep and careful understanding of how concurrency is managed in your code.

现在，说了这么多，如果在一个线程构造了一个不可变的对象（也就是一个只包含最终字段的对象）之后，你想确保它被其他所有线程正确看到，你通常还是需要使用同步。没有其他方法可以确保，例如，对不可变对象的引用会被第二个线程看到。程序从最终字段中得到的保证，应该与对你的代码中如何管理并发性的深刻而仔细的理解相协调。

There is no defined behavior if you want to use JNI to change final fields.

如果你想使用JNI来改变 final 字段，没有定义行为。

### What does volatile do ?
Volatile fields are special fields which are used for communicating state between threads. Each read of a volatile will see the last write to that volatile by any thread; in effect, they are designated by the programmer as fields for which it is never acceptable to see a "stale" value as a result of caching or reordering. The compiler and runtime are prohibited from allocating them in registers. They must also ensure that after they are written, they are flushed out of the cache to main memory, so they can immediately become visible to other threads. Similarly, before a volatile field is read, the cache must be invalidated so that the value in main memory, not the local processor cache, is the one seen. There are also additional restrictions on reordering accesses to volatile variables.

volatile 字段是特殊的字段，用于在线程之间交流状态。对 volatile 的每一次读取都会看到任何线程对该 volatile 的最后一次写入；实际上，它们被程序员指定为字段，由于缓存或重新排序的结果，看到一个"陈旧"的值是不可接受的。编译器和运行时被禁止在寄存器中分配它们。他们还必须确保在写入这些字段后，将其从缓存中刷新到主内存中，这样它们就能立即被其他线程看到。同样，在读取一个 volatile 字段之前，缓存必须被无效化，这样才能看到主内存中的值，而不是本地处理器的缓存。对 volatile 变量的重新排序访问也有额外的限制。

Under the old memory model, accesses to volatile variables could not be reordered with each other, but they could be reordered with nonvolatile variable accesses. This undermined the usefulness of volatile fields as a means of signaling conditions from one thread to another.

在旧的内存模型下，对 volatile 变量的访问不能相互重新排序，但它们可以与非 volatile 变量的访问重新排序。这削弱了 volatile 字段作为从一个线程到另一个线程的信号条件的有用性。

Under the new memory model, it is still true that volatile variables cannot be reordered with each other. The difference is that it is now no longer so easy to reorder normal field accesses around them. Writing to a volatile field has the same memory effect as a monitor release, and reading from a volatile field has the same memory effect as a monitor acquire. In effect, because the new memory model places stricter constraints on reordering of volatile field accesses with other field accesses, volatile or not, anything that was visible to thread A when it writes to volatile field f becomes visible to thread B when it reads f.

在新的内存模型下，volatile 变量不能相互重排，这仍然是事实。不同的是，现在不再那么容易对它们周围的正常字段访问进行重新排序了。**写入 volatile 字段的内存效果与监视释放相同，而从 volatile 字段中读出的内存效果与监视获取相同**。实际上，由于新的内存模型对 volatile 字段访问与其他字段访问的重新排序进行了更严格的限制，无论是否 volatile ，当线程 A 写到 volatile 字段 f 时，任何对线程 A 可见的东西在线程 B 读取 f 时都是可见的。

Here is a simple example of how volatile fields can be used:
```
    class VolatileExample {
        int x = 0;
        volatile boolean v = false;
        public void writer() {
            x = 42;
            v = true;
        }

        public void reader() {
            if (v == true) {
                // uses x - guaranteed to see 42.
            }
        }
    }
```

Assume that one thread is calling writer, and another is calling reader. The write to v in writer releases the write to x to memory, and the read of v acquires that value from memory. Thus, if the reader sees the value true for v, it is also guaranteed to see the write to 42 that happened before it. This would not have been true under the old memory model.  If v were not volatile, then the compiler could reorder the writes in writer, and reader's read of x might see 0.

假设一个线程在调用 writer，而另一个线程在调用 reader。在 writer 中对 v 的写释放了对 x 的写到内存中，而对 v 的读从内存中获得了该值。因此，如果读者看到 v 的值为 true，它也能保证看到在它之前发生的对 42 的写。这在旧的内存模型下是不对的。如果 v 不是 volatile 的，那么编译器可以重新排列写入者的顺序，而读者对 x 的读取可能会看到 0。

Effectively, the semantics of volatile have been strengthened substantially, almost to the level of synchronization. Each read or write of a volatile field acts like "half" a synchronization, for purposes of visibility.

有效地，volatile 的语义被大大加强了，几乎达到了同步的水平。就可见性而言，对一个 volatile 字段的每一次读或写就像 "一半 "的同步。

Important Note: Note that it is important for both threads to access the same volatile variable in order to properly set up the happens-before relationship. It is not the case that everything visible to thread A when it writes volatile field f becomes visible to thread B after it reads volatile field g. The release and acquire have to "match" (i.e., be performed on the same volatile field) to have the right semantics.

重要提示：请注意，两个线程必须访问相同的 volatile 变量，以便正确设置 happens-before 关系。当线程 A 写入volatile 字段 f 时，线程 B 在读取 volatile 字段 g 后，所有可见的东西都会变成可见的，这是不可能的。

### Does the new memeory model fix the "double-checked locking“ problems?
The (infamous) double-checked locking idiom (also called the multithreaded singleton pattern) is a trick designed to support lazy initialization while avoiding the overhead of synchronization. In very early JVMs, synchronization was slow, and developers were eager to remove it -- perhaps too eager. The double-checked locking idiom looks like this:

恶名昭彰的双检查锁成语（也叫多线程单例模式）是一种技巧，旨在支持懒惰的初始化，同时避免同步化的开销。在早期的 JVM 中，同步化速度很慢，开发者急于移除它--也许太急了。双重检查锁的习性看起来像这样。

```
    // double-checked-locking -don't do this !
    private static Something instance = null;

    public Something getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = new Something();
                }
            }
        }
        return instance;
    }
```

This looks awfully clever -- the synchronization is avoided on the common code path. There's only one problem with it -- it doesn't work. Why not? The most obvious reason is that the writes which initialize instance and the write to the instance field can be reordered by the compiler or the cache, which would have the effect of returning what appears to be a partially constructed Something. The result would be that we read an uninitialized object. There are lots of other reasons why this is wrong, and why algorithmic corrections to it are wrong. There is no way to fix it using the old Java memory model. More in-depth information can be found at Double-checked locking: Clever, but broken and The "Double Checked Locking is broken" declaration。

这看起来非常聪明 -- 在公共代码路径上避免了同步化。它只有一个问题 -- 它不工作。为什么不行？最明显的原因是，初始化实例的写入和对实例字段的写入可以被编译器或缓存重新排序，这将产生返回一个看起来是部分构建的东西的效果。其结果是，我们读到了一个未初始化的对象。还有很多其他的原因说明这是错的，以及为什么对它的算法修正是错的。使用旧的 Java 内存模型是没有办法解决这个问题的。更深入的信息可以在相关资料中找到。

Many people assumed that the use of the volatile keyword would eliminate the problems that arise when trying to use the double-checked-locking pattern. In JVMs prior to 1.5, volatile would not ensure that it worked (your mileage may vary). Under the new memory model, making the instance field volatile will "fix" the problems with double-checked locking, because then there will be a happens-before relationship between the initialization of the Something by the constructing thread and the return of its value by the thread that reads it.

许多人认为，使用 volatile 关键字可以消除在尝试使用双重检查锁定模式时出现的问题。在 1.5 之前的JVM 中，volatile 并不能确保它的工作（你的里程可能有所不同）。在新的内存模型下，使实例字段成为volatile 将 "解决 "双重检查锁定的问题，因为在构造线程对某物的初始化和读取它的线程对其值的返回之间将存在一个发生在之前的关系。

Instead, use the Initialization On Demand Holder idiom, which is thread-safe and a lot easier to understand:

取而代之的是使用 Initialization On Demand Holder 成语，它是线程安全的，而且更容易理解。

```
    private static class LazySomethiingHolder {
        public static Something something = new Something();
    }

    public static Something getInstance() {
        return LazySomethingHolder.something;
    }
```

This code is guaranteed to be correct because of the initialization guarantees for static fields; if a field is set in a static initializer, it is guaranteed to be made visible, correctly, to any thread that accesses that class.

这段代码被保证是正确的，因为静态字段的初始化保证；如果一个字段在静态初始化器中被设置，它被保证对访问该类的任何线程都是可见的，正确的。

### Why should I care ?
Why should you care? Concurrency bugs are very difficult to debug. They often don't appear in testing, waiting instead until your program is run under heavy load, and are hard to reproduce and trap. You are much better off spending the extra effort ahead of time to ensure that your program is properly synchronized; while this is not easy, it's a lot easier than trying to debug a badly synchronized application.

你为什么要关心？并发错误是很难调试的。它们通常不会在测试中出现，而是等到你的程序在重负载下运行时才出现，而且很难重现和捕获。你最好提前花费额外的精力来确保你的程序是正确同步的；虽然这并不容易，但比起试图调试一个不同步的应用程序要容易得多。