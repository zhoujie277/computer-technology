# Chapter 17. Threads and Locks

[TOC]

While most of the discussion in the preceding chapters is concerned only with the behavior of code as executed a single statement or expression at a time, that is, by a single thread, the Java Virtual Machine can support many threads of execution at once. These threads independently execute code that operates on values and objects residing in a shared main memory. Threads may be supported by having many hardware processors, by time-slicing a single hardware processor, or by time-slicing many hardware processors.

前面几章的讨论大多只涉及到一次执行单个语句或表达式的代码的行为，即由一个线程执行，而 Java 虚拟机可以同时支持许多线程的执行。这些线程独立地执行代码，对驻留在共享主内存中的值和对象进行操作。线程可以通过拥有许多硬件处理器，通过对单个硬件处理器进行时间分割，或通过对许多硬件处理器进行时间分割来支持。

Threads are represented by the Thread class. The only way for a user to create a thread is to create an object of this class; each thread is associated with such an object. A thread will start when the start() method is invoked on the corresponding Thread object.

线程是由 Thread 类来表示的。用户创建线程的唯一方法是创建这个类的一个对象；每个线程都与这样一个对象相关。当 start() 方法在相应的 Thread 对象上被调用时，一个线程就会启动。

The behavior of threads, particularly when not correctly synchronized, can be confusing and counterintuitive. This chapter describes the semantics of multithreaded programs; it includes rules for which values may be seen by a read of shared memory that is updated by multiple threads. As the specification is similar to the memory models for different hardware architectures, these semantics are known as the Java programming language memory model. When no confusion can arise, we will simply refer to these rules as "the memory model".

线程的行为，特别是在没有正确同步的情况下，可能会令人困惑和反直觉。本章描述了多线程程序的语义；它包括由多个线程更新的共享内存的读取可以看到哪些值的规则。由于该规范与不同硬件架构的内存模型相似，这些语义被称为 Java 编程语言的内存模型。在不产生混淆的情况下，我们将简单地把这些规则称为 "内存模型"。

These semantics do not prescribe how a multithreaded program should be executed. Rather, they describe the behaviors that multithreaded programs are allowed to exhibit. Any execution strategy that generates only allowed behaviors is an acceptable execution strategy.

这些语义并没有规定一个多线程程序应该如何执行。相反，它们描述了多线程程序被允许表现出来的行为。任何只产生允许行为的执行策略都是可接受的执行策略。

## 17.1. Synchronization
The Java programming language provides multiple mechanisms for communicating between threads. The most basic of these methods is synchronization, which is implemented using monitors. Each object in Java is associated with a monitor, which a thread can lock or unlock. Only one thread at a time may hold a lock on a monitor. Any other threads attempting to lock that monitor are blocked until they can obtain a lock on that monitor. A thread t may lock a particular monitor multiple times; each unlock reverses the effect of one lock operation.

Java 编程语言为线程之间的通信提供了多种机制。这些方法中最基本的是同步，它是用监视器实现的。Java中的每个对象都与一个监视器相关联，线程可以锁定或解锁该监视器。一次只有一个线程可以持有一个监视器的锁。任何其他试图锁定该监视器的线程都会被阻断，直到它们能够获得该监视器的锁。一个线程 t 可以多次锁定一个特定的监视器；每次解锁都会逆转一次锁定操作的效果。

The synchronized statement (§14.19) computes a reference to an object; it then attempts to perform a lock action on that object's monitor and does not proceed further until the lock action has successfully completed. After the lock action has been performed, the body of the synchronized statement is executed. If execution of the body is ever completed, either normally or abruptly, an unlock action is automatically performed on that same monitor.

同步语句（§14.19）计算对一个对象的引用；然后尝试对该对象的监视器执行锁定操作，在锁定操作成功完成之前，不会进一步进行。在锁动作执行完毕后，同步语句的主体被执行。如果主体的执行完成了，不管是正常的还是突然的，都会在同一个监视器上自动执行一个解锁动作。

A synchronized method (§8.4.3.6) automatically performs a lock action when it is invoked; its body is not executed until the lock action has successfully completed. If the method is an instance method, it locks the monitor associated with the instance for which it was invoked (that is, the object that will be known as this during execution of the body of the method). If the method is static, it locks the monitor associated with the Class object that represents the class in which the method is defined. If execution of the method's body is ever completed, either normally or abruptly, an unlock action is automatically performed on that same monitor.

同步方法（§8.4.3.6）在被调用时自动执行锁定动作；在锁定动作成功完成之前，其主体不被执行。如果该方法是一个实例方法，它将锁定与它被调用的实例相关的监视器（也就是说，在执行该方法的主体时，该对象将被称为这个）。如果该方法是静态的，它将锁定与代表该方法所定义的类的类对象相关的监视器。如果方法主体的执行完成了，无论是正常的还是突然的，都会在同一个监视器上自动执行解锁操作。

The Java programming language neither prevents nor requires detection of deadlock conditions. Programs where threads hold (directly or indirectly) locks on multiple objects should use conventional techniques for deadlock avoidance, creating higher-level locking primitives that do not deadlock, if necessary.

Java 编程语言既不防止也不要求检测死锁条件。线程在多个对象上持有（直接或间接）锁的程序应使用常规技术来避免死锁，必要时创建不会死锁的更高级别的锁原语。

Other mechanisms, such as reads and writes of volatile variables and the use of classes in the java.util.concurrent package, provide alternative ways of synchronization.

其他机制，如 volatile 变量的读写和使用 java.util.concurrent 包中的类，提供了同步的其他方式。

## 17.2. Wait Sets and Notification
Every object, in addition to having an associated monitor, has an associated wait set. A wait set is a set of threads.

每个对象，除了有一个相关的监视器外，还有一个相关的等待集。一个等待集是一个线程集。

When an object is first created, its wait set is empty. Elementary actions that add threads to and remove threads from wait sets are atomic. Wait sets are manipulated solely through the methods Object.wait, Object.notify, and Object.notifyAll.

当一个对象被首次创建时，其等待集是空的。向等待集添加线程和从等待集移除线程的基本动作是原子性的。等待集只能通过 Object.wait、Object.notify 和 Object.notifyAll 等方法来操作。

Wait set manipulations can also be affected by the interruption status of a thread, and by the Thread class's methods dealing with interruption. Additionally, the Thread class's methods for sleeping and joining other threads have properties derived from those of wait and notification actions.

等待集的操作也会受到线程的中断状态以及 Thread 类中处理中断的方法的影响。此外，Thread 类用于睡眠和加入其他线程的方法也有从等待和通知操作中衍生出来的属性。

### 17.2.1. Wait
Wait actions occur upon invocation of wait(), or the timed forms wait(long millisecs) and wait(long millisecs, int nanosecs).

等待动作发生在调用 wait()，或定时形式的 wait(long millisecs) 和 wait(long millisecs, int nanosecs)。

A call of wait(long millisecs) with a parameter of zero, or a call of wait(long millisecs, int nanosecs) with two zero parameters, is equivalent to an invocation of wait().

调用参数为 0 的 wait(long millisecs)，或者调用两个参数为 0 的 wait(long millisecs, int nanosecs)，相当于调用 wait()。

A thread returns normally from a wait if it returns without throwing an InterruptedException.

如果一个线程在返回时没有抛出一个 InterruptedException，那么它就会从等待中正常返回。

Let thread t be the thread executing the wait method on object m, and let n be the number of lock actions by t on m that have not been matched by unlock actions. One of the following actions occurs:

让线程 t 是在对象 m 上执行等待方法的线程，让 n 是 t 在 m 上进行的没有被解锁动作匹配的锁定动作的数量。下面的行动之一发生了。

+ If n is zero (i.e., thread t does not already possess the lock for target m), then an IllegalMonitorStateException is thrown. (如果 n 为零（即线程 t 还没有拥有目标 m 的锁），那么就会抛出一个 IllegalMonitorStateException。)
+ If this is a timed wait and the nanosecs argument is not in the range of 0-999999 or the millisecs argument is negative, then an IllegalArgumentException is thrown. (如果这是一个定时等待，并且纳秒参数不在 0-999999 的范围内，或者毫秒参数是负数，那么会抛出一个 IllegalArgumentException。)
+ If thread t is interrupted, then an InterruptedException is thrown and t's interruption status is set to false. (如果线程 t 被中断，那么会抛出一个InterruptedException，t 的中断状态被设置为 false。)
+ Otherwise, the following sequence occurs: (否则，会出现以下序列。)
  1. Thread t is added to the wait set of object m, and performs n unlock actions on m. (线程 t 被添加到对象 m 的等待集，并对 m 执行 n 个解锁动作。)
  2. Thread t does not execute any further instructions until it has been removed from m's wait set. The thread may be removed from the wait set due to any one of the following actions, and will resume sometime afterward: (线程 t 不执行任何进一步的指令，直到它被从 m 的等待集中移除。线程可能由于以下任何一个动作而被从等待集中移除，并将在之后的某个时间恢复。)
     + A notify action being performed on m in which t is selected for removal from the wait set. (正在对m进行的通知动作，其中 t 被选为从等待集中删除)
     + A notifyAll action being performed on m. (一个正在对 m 进行的 notifyAll 动作)
     + An interrupt action being performed on t. (正在对 t 进行的中断操作)
     + If this is a timed wait, an internal action removing t from m's wait set that occurs after at least millisecs milliseconds plus nanosecs nanoseconds elapse since the beginning of this wait action. (如果这是一个定时的等待，一个从 m 的等待集合中移除 t 的内部动作，该动作发生在从这个等待动作开始后的至少毫秒加纳秒的时间)
     + An internal action by the implementation. Implementations are permitted, although not encouraged, to perform "spurious wake-ups", that is, to remove threads from wait sets and thus enable resumption without explicit instructions to do so. (实现的内部动作。允许（但不鼓励）实现执行 "假性唤醒"，即从等待集中移除线程，从而在没有明确指令的情况下实现恢复。)

        Notice that this provision necessitates the Java coding practice of using wait only within loops that terminate only when some logical condition that the thread is waiting for holds. (请注意，这一规定使 Java 的编码实践成为必要，即只在循环中使用等待，只有当线程所等待的某些逻辑条件成立时才终止。)
    
        Each thread must determine an order over the events that could cause it to be removed from a wait set. That order does not have to be consistent with other orderings, but the thread must behave as though those events occurred in that order. (每个线程必须确定一个可能导致其从等待集中被移除的事件的顺序。这个顺序不一定要与其他顺序一致，但线程必须表现得像这些事件是按照这个顺序发生的。)

        For example, if a thread t is in the wait set for m, and then both an interrupt of t and a notification of m occur, there must be an order over these events. If the interrupt is deemed to have occurred first, then t will eventually return from wait by throwing InterruptedException, and some other thread in the wait set for m (if any exist at the time of the notification) must receive the notification. If the notification is deemed to have occurred first, then t will eventually return normally from wait with an interrupt still pending. (例如，如果一个线程 t 在 m 的等待集中，然后 t 的中断和 m 的通知都发生了，这些事件必须有一个顺序。如果中断被认为是先发生的，那么 t 最终将通过抛出 InterruptedException 从等待中返回，而 m 的等待集中的其他线程（如果在通知发生时存在任何线程）必须接收通知。如果通知被认为是先发生的，那么 t 最终将从等待中正常返回，而中断仍在等待中。)

  3. Thread t performs n lock actions on m. (线程 t 对 m 执行 n 个锁动作。)
  4. If thread t was removed from m's wait set in step 2 due to an interrupt, then t's interruption status is set to false and the wait method throws InterruptedException. (如果线程 t 在步骤 2 中由于中断而被从 m 的等待集中移除，那么 t 的中断状态被设置为 false，并且等待方法抛出 InterruptedException。)

### 17.2.2. Notification
Notification actions occur upon invocation of methods notify and notifyAll.

通知动作在调用方法 notify 和 notifyAll 时发生。

Let thread t be the thread executing either of these methods on object m, and let n be the number of lock actions by t on m that have not been matched by unlock actions. One of the following actions occurs:

让线程 t 是在对象 m 上执行这些方法的线程，让 n 是 t 在 m 上进行的没有被解锁动作匹配的锁定动作的数量。下面的行动之一发生了。

+ If n is zero, then an IllegalMonitorStateException is thrown.

    This is the case where thread t does not already possess the lock for target m. (这种情况下，线程 t 并不拥有目标 m 的锁。)

+ If n is greater than zero and this is a notify action, then if m's wait set is not empty, a thread u that is a member of m's current wait set is selected and removed from the wait set. (如果 n 大于 0，并且这是一个通知动作，那么如果 m 的等待集不是空的，作为 m 的当前等待集成员的线程 u 被选中并从等待集中移除。)
    
    There is no guarantee about which thread in the wait set is selected. This removal from the wait set enables u's resumption in a wait action. Notice, however, that u's lock actions upon resumption cannot succeed until some time after t fully unlocks the monitor for m. (对于等待集中的哪个线程被选中，没有任何保证。这种从等待集中移除的做法使 u 能够在等待行动中恢复。然而，请注意，u 在恢复时的锁动作不能成功，直到 t 为 m 完全解锁监视器后的某个时间。)

+ If n is greater than zero and this is a notifyAll action, then all threads are removed from m's wait set, and thus resume. (如果 n 大于 0，并且这是一个 notifyAll 动作，那么所有线程都会从 m 的等待集中移除，从而恢复。)
    
    Notice, however, that only one of them at a time will lock the monitor required during the resumption of wait. (然而，请注意，在恢复等待期间，每次只有其中一个会锁定所需的显示器。)

### 17.2.3. Interruptions
Interruption actions occur upon invocation of Thread.interrupt, as well as methods defined to invoke it in turn, such as ThreadGroup.interrupt.

中断动作发生在调用 Thread.interrupt，以及定义为依次调用它的方法，如 ThreadGroup.interrupt。

Let t be the thread invoking u.interrupt, for some thread u, where t and u may be the same. This action causes u's interruption status to be set to true.

让 t 是调用 u.interrupt 的线程，对于某个线程 u，t 和 u 可能是相同的。这个动作导致 u 的中断状态被设置为真。

Additionally, if there exists some object m whose wait set contains u, then u is removed from m's wait set. This enables u to resume in a wait action, in which case this wait will, after re-locking m's monitor, throw InterruptedException.

此外，如果存在某个对象 m，其等待集包含 u，那么 u 就会从 m 的等待集中移除。这使得 u 能够在一个等待动作中恢复，在这种情况下，这个等待将在重新锁定 m 的监视器后，抛出 InterruptedException。

Invocations of Thread.isInterrupted can determine a thread's interruption status. The static method Thread.interrupted may be invoked by a thread to observe and clear its own interruption status.

对 Thread.isInterrupted 的调用可以确定一个线程的中断状态。静态方法 Thread.interrupted 可以被一个线程调用，以观察和清除它自己的中断状态。

### 17.2.4. Interactions of Waits, Notification, and Interruption
The above specifications allow us to determine several properties having to do with the interaction of waits, notification, and interruption.

上述规范使我们能够确定与等待、通知和中断的互动有关的几个属性。

If a thread is both notified and interrupted while waiting, it may either:

如果一个线程在等待过程中既被通知又被打断，它可能是这样。

+ return normally from wait, while still having a pending interrupt (in other words, a call to Thread.interrupted would return true) (从等待中正常返回，同时仍有一个待处理的中断（换句话说，对 Thread.interrupted 的调用将返回 true）。)
+ return from wait by throwing an InterruptedException (抛出一个InterruptedException，从等待中返回)

The thread may not reset its interrupt status and return normally from the call to wait.

线程可能不会重置其中断状态并从调用等待中正常返回。

Similarly, notifications cannot be lost due to interrupts. Assume that a set s of threads is in the wait set of an object m, and another thread performs a notify on m. Then either:

同样地，通知也不能因为中断而丢失。假设一组线程 s 在一个对象 m 的等待集中，而另一个线程对 m 执行了一个通知，那么要么。

+ at least one thread in s must return normally from wait, or (s 中至少有一个线程必须从等待中正常返回，或)
+ all of the threads in s must exit wait by throwing InterruptedException (s 中的所有线程必须通过抛出 InterruptedException 退出等待。)

Note that if a thread is both interrupted and woken via notify, and that thread returns from wait by throwing an InterruptedException, then some other thread in the wait set must be notified.

请注意，如果一个线程既被中断又通过 notify 被唤醒，并且该线程通过抛出一个 InterruptedException 从等待中返回，那么等待集合中的其他一些线程必须被通知。

## 17.3. Sleep and Yield
Thread.sleep causes the currently executing thread to sleep (temporarily cease execution) for the specified duration, subject to the precision and accuracy of system timers and schedulers. The thread does not lose ownership of any monitors, and resumption of execution will depend on scheduling and the availability of processors on which to execute the thread.

Thread.sleep 使当前执行的线程在指定的持续时间内睡眠（暂时停止执行），这取决于系统定时器和调度器的精度和准确性。该线程不会失去对任何监视器的所有权，恢复执行将取决于调度和执行该线程的处理器的可用性。

It is important to note that neither Thread.sleep nor Thread.yield have any synchronization semantics. In particular, the compiler does not have to flush writes cached in registers out to shared memory before a call to Thread.sleep or Thread.yield, nor does the compiler have to reload values cached in registers after a call to Thread.sleep or Thread.yield.

值得注意的是，Thread.sleep 和 Thread.yield都没有任何同步语义。特别是，在调用 Thread.sleep 或 Thread.yield 之前，编译器不必将缓存在寄存器中的写入 flush 到共享内存中，在调用 Thread.sleep 或 Thread.yield 之后，编译器也不必重新加载缓存在寄存器中的值。

```
For example, in the following (broken) code fragment, assume that this.done is a non-volatile boolean field:

while (!this.done)
    Thread.sleep(1000);

The compiler is free to read the field this.done just once, and reuse the cached value in each execution of the loop. 
编译器可以自由地读取字段 this.done 一次，并在循环的每次执行中重复使用缓存的值。

This would mean that the loop would never terminate, even if another thread changed the value of this.done.
这将意味着，即使另一个线程改变了 this.done 的值，循环也不会终止。

```

## 17.4. Memory Model
A memory model describes, given a program and an execution trace of that program, whether the execution trace is a legal execution of the program. The Java programming language memory model works by examining each read in an execution trace and checking that the write observed by that read is valid according to certain rules.

一个内存模型描述，给定一个程序和该程序的执行轨迹，该执行轨迹是否是该程序的合法执行。Java 编程语言的内存模型通过检查执行轨迹中的每一个读，并检查该读所观察到的写是否根据某些规则有效。

The memory model describes possible behaviors of a program. An implementation is free to produce any code it likes, as long as all resulting executions of a program produce a result that can be predicted by the memory model.

内存模型描述了一个程序的可能行为。只要程序的所有执行结果都能产生内存模型所能预测的结果，实现者就可以自由地产生任何它喜欢的代码。

This provides a great deal of freedom for the implementor to perform a myriad of code transformations, including the reordering of actions and removal of unnecessary synchronization.

这为实现者提供了很大的自由度，可以进行大量的代码转换，包括重新安排动作的顺序和去除不必要的同步。

### Example 17.4-1. Incorrectly Synchronized Programs May Exhibit Surprising Behavior

The semantics of the Java programming language allow compilers and microprocessors to perform optimizations that can interact with incorrectly synchronized code in ways that can produce behaviors that seem paradoxical. Here are some examples of how incorrectly synchronized programs may exhibit surprising behaviors.

Java 编程语言的语义允许编译器和微处理器进行优化，这些优化可以与不正确的同步代码进行互动，从而产生看似矛盾的行为。下面是一些例子，说明不正确的同步化程序如何表现出令人奇怪的行为。

Consider, for example, the example program traces shown in Table 17.1. This program uses local variables r1 and r2 and shared variables A and B. Initially, A == B == 0.

例如，考虑一下表 17.1 中所示的示例程序痕迹。这个程序使用局部变量 r1 和 r2 以及共享变量 A 和 B。

Table 17.1. Surprising results caused by statement reordering - original code

表 17.1. 语句重排引起的令人惊讶的结果--原代码

Thread1 | Thread2
------  | -----
1: r2 = A; | 3: r1 = B;
2: B = 1;  | 4: A = 2;

It may appear that the result r2 == 2 and r1 == 1 is impossible. Intuitively, either instruction 1 or instruction 3 should come first in an execution. If instruction 1 comes first, it should not be able to see the write at instruction 4. If instruction 3 comes first, it should not be able to see the write at instruction 2.

看起来 r2 == 2 和 r1 == 1 的结果是不可能的。直观地说，在执行过程中，指令 1 或指令 3 应该先执行。如果指令 1 在先，它就不应该看到指令 4 的写入。如果指令 3 在先，它就不能看到指令 2 的写入。

If some execution exhibited this behavior, then we would know that instruction 4 came before instruction 1, which came before instruction 2, which came before instruction 3, which came before instruction 4. This is, on the face of it, absurd.

如果一些执行程序表现出这种行为，那么我们就会知道，指令 4 在指令 1 之前，而指令 1 在指令 2 之前，指令 2 在指令 3 之前，指令 3 在指令 4 之前。从表面上看，这是很荒谬的。

However, compilers are allowed to reorder the instructions in either thread, when this does not affect the execution of that thread in isolation. If instruction 1 is reordered with instruction 2, as shown in the trace in Table 17.2, then it is easy to see how the result r2 == 2 and r1 == 1 might occur.

然而，编译器允许对任何一个线程的指令进行重新排序，只要这不影响该线程的独立执行。如果指令 1 与指令 2 重新排序，如表 17.2 中的跟踪所示，那么很容易看出结果 r2 == 2 和 r1 == 1 可能发生。

Table 17.2. Surprising results caused by statement reordering - valid compiler transformation

表17.2. 语句重排引起的令人惊讶的结果 - 有效的编译器转换

Thread1 | Thread2
------  | -----
B = 1;	| r1 = B;
r2 = A;	| A = 2;

To some programmers, this behavior may seem "broken". However, it should be noted that this code is improperly synchronized:

对一些程序员来说，这种行为可能看起来是 "坏了"。然而，应该注意的是，这段代码是不适当的同步。

+ there is a write in one thread,
+ a read of the same variable by another thread,
+ and the write and read are not ordered by synchronization.

This situation is an example of a data race (§17.4.5). When code contains a data race, counterintuitive results are often possible.

这种情况是数据竞争的一个例子（§17.4.5）。当代码中含有数据竞争时，往往可能出现反直觉的结果。

Several mechanisms can produce the reordering in Table 17.2. A Just-In-Time compiler in a Java Virtual Machine implementation may rearrange code, or the processor. In addition, the memory hierarchy of the architecture on which a Java Virtual Machine implementation is run may make it appear as if code is being reordered. In this chapter, we shall refer to anything that can reorder code as a compiler.

有几种机制可以产生表 17.2 中的重新排序。Java 虚拟机实现中的及时编译器可能会重新排列代码，或者处理器。此外，运行 Java 虚拟机实现的架构的内存层次结构可能会使代码看起来被重新排序。在本章中，我们将把任何可以重新排列代码的东西称为编译器。

Another example of surprising results can be seen in Table 17.3. Initially, p == q and p.x == 0. This program is also incorrectly synchronized; it writes to shared memory without enforcing any ordering between those writes.

另一个令人惊讶的结果的例子可以在表 17.3 中看到。最初，p == q 和 p.x == 0。这个程序也是不正确的同步；它向共享内存写东西，而不在这些写东西之间执行任何排序。

Table 17.3. Surprising results caused by forward substitution

表17.3. 由正向替代引起的令人奇怪的结果

Thread1 | Thread2
------  | -----
r1 = p;	| r6 = p;
r2 = r1.x; | r6.x = 3;
r3 = q;    |
r4 = r3.x; | 
r5 = r1.x; | 

One common compiler optimization involves having the value read for r2 reused for r5: they are both reads of r1.x with no intervening write. This situation is shown in Table 17.4.

一个常见的编译器优化涉及到让 r2 的读值重用于 r5：它们都是对 r1.x 的读，没有中间的写。这种情况如表 17.4 所示。

Table 17.4. Surprising results caused by forward substitution
Thread1 | Thread2
------  | -----
r1 = p;	| r6 = p;
r2 = r1.x; | r6.x = 3;
r3 = q;    |
r4 = r3.x; | 
r5 = r2;   |

Now consider the case where the assignment to r6.x in Thread 2 happens between the first read of r1.x and the read of r3.x in Thread 1. If the compiler decides to reuse the value of r2 for the r5, then r2 and r5 will have the value 0, and r4 will have the value 3. From the perspective of the programmer, the value stored at p.x has changed from 0 to 3 and then changed back.

现在考虑这样的情况：线程 2 中对 r6.x 的赋值发生在线程 1 中第一次读 r1.x 和读 r3.x 之间。如果编译器决定为 r5 重用 r2 的值，那么 r2 和 r5 的值是 0，而 r4 的值是 3。从程序员的角度来看，存储在 p.x 的值已经从 0 变成了 3，然后又变回来。

The memory model determines what values can be read at every point in the program. The actions of each thread in isolation must behave as governed by the semantics of that thread, with the exception that the values seen by each read are determined by the memory model. When we refer to this, we say that the program obeys intra-thread semantics. Intra-thread semantics are the semantics for single-threaded programs, and allow the complete prediction of the behavior of a thread based on the values seen by read actions within the thread. To determine if the actions of thread t in an execution are legal, we simply evaluate the implementation of thread t as it would be performed in a single-threaded context, as defined in the rest of this specification.

内存模型决定了在程序的每一个点上可以读取哪些值。每个线程的行为都必须是由该线程的语义所支配的，但例外的是，每个读到的值都由内存模型决定。当我们提到这一点时，我们说程序服从线程内的语义。线程内语义是单线程程序的语义，它允许根据线程内的读操作所看到的值来完全预测线程的行为。为了确定线程 t 在执行中的行为是否合法，我们只需评估线程 t 的执行情况，因为它将在单线程的上下文中执行，如本规范的其余部分所定义。

Each time the evaluation of thread t generates an inter-thread action, it must match the inter-thread action a of t that comes next in program order. If a is a read, then further evaluation of t uses the value seen by a as determined by the memory model.

每次线程 t 的评估产生一个线程间动作时，它必须与程序顺序中接下来的 t 的线程间动作 a 匹配。如果 a 是读，那么对 t 的进一步评估就会使用由内存模型决定的 a 所看到的值。

This section provides the specification of the Java programming language memory model except for issues dealing with final fields, which are described in §17.5.

本节提供了 Java 编程语言内存模型的规范，但涉及 final 字段的问题除外，这些问题将在第 17.5 节描述。

The memory model specified herein is not fundamentally based in the object-oriented nature of the Java programming language. For conciseness and simplicity in our examples, we often exhibit code fragments without class or method definitions, or explicit dereferencing. Most examples consist of two or more threads containing statements with access to local variables, shared global variables, or instance fields of an object. We typically use variables names such as r1 or r2 to indicate variables local to a method or thread. Such variables are not accessible by other threads.

此处规定的内存模型并不是从根本上基于 Java 编程语言的面向对象性质。为了使我们的例子简洁明了，我们经常展示没有类或方法定义的代码片段，也没有明确的去引用。大多数例子由两个或更多的线程组成，包含访问局部变量、共享全局变量或对象的实例字段的语句。我们通常使用 r1 或 r2 这样的变量名来表示某个方法或线程的本地变量。这样的变量不能被其他线程访问。

### 17.4.1. Shared Variables
Memory that can be shared between threads is called shared memory or heap memory.

线程之间可以共享的内存被称为共享内存或堆内存。

All instance fields, static fields, and array elements are stored in heap memory. In this chapter, we use the term variable to refer to both fields and array elements.

所有的实例字段、静态字段和数组元素都存储在堆内存中。在本章中，我们使用术语变量来指代字段和数组元素。

Local variables (§14.4), formal method parameters (§8.4.1), and exception handler parameters (§14.20) are never shared between threads and are unaffected by the memory model.

本地变量（§14.4）、正式方法参数（§8.4.1）和异常处理程序参数（§14.20）从未在线程之间共享，不受内存模型的影响。

Two accesses to (reads of or writes to) the same variable are said to be conflicting if at least one of the accesses is a write.

如果对同一变量的两个访问（读或写）中至少有一个是写，则称为冲突。

### 17.4.2. Actions
An inter-thread action is an action performed by one thread that can be detected or directly influenced by another thread. There are several kinds of inter-thread action that a program may perform:

线程间动作是指由一个线程执行的、可以被另一个线程检测到或直接影响的动作。一个程序可能执行的线程间动作有几种。

+ Read (normal, or non-volatile). Reading a variable.

+ Write (normal, or non-volatile). Writing a variable.

+ Synchronization actions, which are:
  + Volatile read. A volatile read of a variable.
  + Volatile write. A volatile write of a variable.
  + Lock. Locking a monitor
  + Unlock. Unlocking a monitor.
  + The (synthetic) first and last action of a thread.
  + Actions that start a thread or detect that a thread has terminated (§17.4.4).

+ External Actions. An external action is an action that may be observable outside of an execution, and has a result based on an environment external to the execution. (外部行动是指在执行之外可以观察到的行动，它的结果是基于执行之外的环境。)

+ Thread divergence actions (§17.4.9). A thread divergence action is only performed by a thread that is in an infinite loop in which no memory, synchronization, or external actions are performed. If a thread performs a thread divergence action, it will be followed by an infinite number of thread divergence actions. (线程分歧行动（§17.4.9）。线程发散动作仅由处于无限循环中的线程执行，在该循环中不执行内存、同步或外部动作。如果一个线程执行了一个线程发散动作，那么它后面会有无限多的线程发散动作。)

    Thread divergence actions are introduced to model how a thread may cause all other threads to stall and fail to make progress. (引入了线程分歧行动，以模拟一个线程如何可能导致所有其他线程停滞，无法取得进展。)

This specification is only concerned with inter-thread actions. We do not need to concern ourselves with intra-thread actions (e.g., adding two local variables and storing the result in a third local variable). As previously mentioned, all threads need to obey the correct intra-thread semantics for Java programs. We will usually refere to inter-thread actions more succinctly as simply actions.

本规范只关注线程间的操作。我们不需要关注线程内的操作（例如，添加两个局部变量并将结果存储在第三个局部变量中）。如前所述，所有线程都需要遵守 Java 程序的正确线程内语义。我们通常会把线程间的动作更简洁地称为简单的动作。

An action a is described by a tuple < t, k, v, u >, comprising:

一个行动 a 由一个元组 < t, k, v, u> 来描述，包括。

+ t - the thread performing the action
+ k - the kind of action
+ v - the variable or monitor involved in the action.

    For lock actions, v is the monitor being locked; for unlock actions, v is the monitor being unlocked.

    If the action is a (volatile or non-volatile) read, v is the variable being read.

    If the action is a (volatile or non-volatile) write, v is the variable being written.

+ u - an arbitrary unique identifier for the action (行动的任意唯一标识符)

An external action tuple contains an additional component, which contains the results of the external action as perceived by the thread performing the action. This may be information as to the success or failure of the action, and any values read by the action.

一个外部动作元组包含一个额外的组件，它包含执行该动作的线程所感知的外部动作的结果。这可能是关于行动的成功或失败的信息，以及行动所读取的任何值。

Parameters to the external action (e.g., which bytes are written to which socket) are not part of the external action tuple. These parameters are set up by other actions within the thread and can be determined by examining the intra-thread semantics. They are not explicitly discussed in the memory model.

外部动作的参数（例如，哪些字节被写到哪个套接字）不是外部动作元组的一部分。这些参数是由线程内的其他动作设置的，可以通过检查线程内的语义来确定。它们在内存模型中没有被明确讨论。

In non-terminating executions, not all external actions are observable. Non-terminating executions and observable actions are discussed in §17.4.9.

在非终止执行中，并非所有的外部行为都是可观察的。第 17.4.9 节讨论了非终止执行和可观察行动。

### 17.4.3. Programs and Program Order
Among all the inter-thread actions performed by each thread t, the program order of t is a total order that reflects the order in which these actions would be performed according to the intra-thread semantics of t.

在每个线程 t 执行的所有线程间动作中，t 的程序顺序是一个总的顺序，反映了根据 t 的线程内语义执行这些动作的顺序。

A set of actions is sequentially consistent if all actions occur in a total order (the execution order) that is consistent with program order, and furthermore, each read r of a variable v sees the value written by the write w to v such that:

如果所有的行动都以与程序顺序一致的总顺序（执行顺序）出现，那么一组行动就是顺序一致的，此外，对一个变量 v 的每一次读 r 都能看到由写 w 写到 v 的值，这样。

+ w comes before r in the execution order, and

+ w 在执行顺序中排在 r 之前，并且

+ there is no other write w' such that w comes before w' and w' comes before r in the execution order.

+ 没有其他的写 w'，使得 w 在 w' 之前，而 w' 在执行顺序中在 r 之前。

Sequential consistency is a very strong guarantee that is made about visibility and ordering in an execution of a program. Within a sequentially consistent execution, there is a total order over all individual actions (such as reads and writes) which is consistent with the order of the program, and each individual action is atomic and is immediately visible to every thread.

顺序一致性是对程序执行中的可见性和顺序的一种非常有力的保证。在顺序一致的执行中，所有单个动作（如读和写）都有一个总的顺序，与程序的顺序一致，每个单个动作都是原子性的，对每个线程都立即可见。

If a program has no data races, then all executions of the program will appear to be sequentially consistent.

如果一个程序没有数据竞争，那么该程序的所有执行将看起来是顺序一致的。

Sequential consistency and/or freedom from data races still allows errors arising from groups of operations that need to be perceived atomically and are not.

顺序一致性和/或免于数据竞争的自由仍然允许需要以原子方式感知的操作组所产生的错误，而这些操作并不是。

If we were to use sequential consistency as our memory model, many of the compiler and processor optimizations that we have discussed would be illegal. For example, in the trace in Table 17.3, as soon as the write of 3 to p.x occurred, subsequent reads of that location would be required to see that value.

如果我们使用顺序一致性作为我们的内存模型，那么我们所讨论的许多编译器和处理器的优化都将是非法的。例如，在表 17.3 中的跟踪中，一旦 3 的写入发生在 p.x 中，随后对该位置的读取就需要看到这个值。

### 17.4.4. Synchronization Order
Every execution has a synchronization order. A synchronization order is a total order over all of the synchronization actions of an execution. For each thread t, the synchronization order of the synchronization actions (§17.4.2) in t is consistent with the program order (§17.4.3) of t.

每个执行都有一个同步顺序。同步顺序是一个执行中所有同步动作的总顺序。对于每个线程 t，t 中的同步动作（§17.4.2）的同步顺序与 t 的程序顺序（§17.4.3）一致。

Synchronization actions induce the synchronized-with relation on actions, defined as follows:

同步动作引起了动作之间的 synchronized-with 关系，定义如下。

+ An unlock action on monitor m synchronizes-with all subsequent lock actions on m (where "subsequent" is defined according to the synchronization order). (监视器 m 上的解锁动作与 m 上所有后续的锁动作同步（其中 "后续"是根据同步顺序定义的）。)

+ A write to a volatile variable v (§8.3.1.4) synchronizes-with all subsequent reads of v by any thread (where "subsequent" is defined according to the synchronization order). (对 volatile 变量 v 的写（§8.3.1.4）与任何线程对 v 的所有后续读同步（其中 "后续"是根据同步顺序定义的）。)

+ An action that starts a thread synchronizes-with the first action in the thread it starts. (一个启动线程的动作会与它所启动的线程中的第一个动作同步。)

+ The write of the default value (zero, false, or null) to each variable synchronizes-with the first action in every thread. (写入每个变量的默认值（零、假或空）与每个线程的第一个动作同步。)

    Although it may seem a little strange to write a default value to a variable before the object containing the variable is allocated, conceptually every object is created at the start of the program with its default initialized values. (虽然在包含变量的对象被分配之前给变量写一个默认值看起来有点奇怪，但从概念上讲，每个对象都是在程序开始时以其默认初始化值创建的。)

+ The final action in a thread T1 synchronizes-with any action in another thread T2 that detects that T1 has terminated. (一个线程 T1 的最后行动与另一个线程 T2 的任何行动同步，该行动检测到 T1 已经终止。)

    T2 may accomplish this by calling T1.isAlive() or T1.join(). (T2 可以通过调用 T1.isAlive() 或 T1.join() 来实现这一目标。)

+ If thread T1 interrupts thread T2, the interrupt by T1 synchronizes-with any point where any other thread (including T2) determines that T2 has been interrupted (by having an InterruptedException thrown or by invoking Thread.interrupted or Thread.isInterrupted). (如果线程 T1 中断线程 T2，T1 的中断与任何其他线程（包括 T2）确定 T2 已被中断（通过抛出 InterruptedException 或调用 Thread.interrupted 或Thread.isInterrupted）的点同步。)

The source of a synchronizes-with edge is called a release, and the destination is called an acquire.

一个 synchronizes-with 边的源头被称为 release，而目的地被称为 acquire。

### 17.4.5. Happens-before Order
Two actions can be ordered by a happens-before relationship. If one action happens-before another, then the first is visible to and ordered before the second.

两个动作可以通过 happens-before 的关系进行排序。如果一个动作发生在另一个动作之前，那么第一个动作对第二个动作是可见的，并在第二个动作之前排序。

If we have two actions x and y, we write hb(x, y) to indicate that x happens-before y.

如果我们有两个行动 x 和 y，我们写 hb(x, y) 来表示 x 发生在 y 之前。

+ If x and y are actions of the same thread and x comes before y in program order, then hb(x, y). （如果 x 和 y 是同一个线程的动作，并且 x 在程序顺序中排在 y 之前，那么 hb(x, y)。
+ There is a happens-before edge from the end of a constructor of an object to the start of a finalizer (§12.6) for that object. (从一个对象的构造函数的末尾到该对象的终结器（§12.6）的开始，有一条 happens-before 边。)
+ If an action x synchronizes-with a following action y, then we also have hb(x, y). (如果一个行动 x 与后续行动 y 同步，那么我们也有 hb(x, y)。)
+ If hb(x, y) and hb(y, z), then hb(x, z).

The wait methods of class Object (§17.2.1) have lock and unlock actions associated with them; their happens-before relationships are defined by these associated actions.

对象类的等待方法（§17.2.1）有与之相关的锁定和解锁动作；它们的 happens-before 关系由这些相关的动作来定义。

It should be noted that the presence of a happens-before relationship between two actions does not necessarily imply that they have to take place in that order in an implementation. If the reordering produces results consistent with a legal execution, it is not illegal.

应该注意的是，两个动作之间存在 happens-before 的关系并不一定意味着它们在执行中必须以这个顺序发生。如果重新排序产生的结果与合法的执行相一致，它就不是非法的。

For example, the write of a default value to every field of an object constructed by a thread need not happen before the beginning of that thread, as long as no read ever observes that fact.

例如，向一个线程构建的对象的每一个字段写入默认值，不需要在该线程开始之前发生，只要没有读取观察到这个事实。

More specifically, if two actions share a happens-before relationship, they do not necessarily have to appear to have happened in that order to any code with which they do not share a happens-before relationship. Writes in one thread that are in a data race with reads in another thread may, for example, appear to occur out of order to those reads.

更具体地说，如果两个动作共享一个 happens-before 的关系，它们不一定要在任何不共享 happens-before 关系的代码中以该顺序发生。例如，一个线程中的写操作如果与另一个线程中的读操作存在数据竞争，那么对于这些读操作来说，可能会出现不按顺序发生的情况。

The happens-before relation defines when data races take place.

happens-before 关系定义了数据竞争发生的时间。

A set of synchronization edges, S, is sufficient if it is the minimal set such that the transitive closure of S with the program order determines all of the happens-before edges in the execution. This set is unique.

如果一个同步边的集合 S 是最小的集合，使得 S 与程序顺序的反式闭合决定了执行中所有的 happens-before 的边，那么它就是充分的。这个集合是唯一的。

It follows from the above definitions that: (从上面的定义可以看出：)
+ An unlock on a monitor happens-before every subsequent lock on that monitor.
+ A write to a volatile field (§8.3.1.4) happens-before every subsequent read of that field.
+ A call to start() on a thread happens-before any actions in the started thread.
+ All actions in a thread happen-before any other thread successfully returns from a join() on that thread.
+ The default initialization of any object happens-before any other actions (other than default-writes) of a program. (任何对象的默认初始化都发生在程序的任何其他动作（除默认写入外）之前)

When a program contains two conflicting accesses (§17.4.1) that are not ordered by a happens-before relationship, it is said to contain a data race.

当一个程序包含两个相互冲突的访问（§17.4.1），而这两个访问并不是由 happens-before 的关系来排序的，就被称为包含数据竞争。

The semantics of operations other than inter-thread actions, such as reads of array lengths (§10.7), executions of checked casts (§5.5, §15.16), and invocations of virtual methods (§15.12), are not directly affected by data races.

除线程间操作外，其他操作的语义，如数组长度的读取（§10.7）、检查过的转换的执行（§5.5，§15.16）和虚拟方法的调用（§15.12），不会直接受到数据竞争的影响。

Therefore, a data race cannot cause incorrect behavior such as returning the wrong length for an array.

因此，数据竞争不能导致不正确的行为，例如为一个数组返回错误的长度。

A program is correctly synchronized if and only if all sequentially consistent executions are free of data races.

当且仅当所有顺序一致的执行都不存在数据竞争时，程序才是正确的同步。

If a program is correctly synchronized, then all executions of the program will appear to be sequentially consistent (§17.4.3).

如果一个程序被正确地同步了，那么程序的所有执行都会显得顺序一致（§17.4.3）。

This is an extremely strong guarantee for programmers. Programmers do not need to reason about reorderings to determine that their code contains data races. Therefore they do not need to reason about reorderings when determining whether their code is correctly synchronized. Once the determination that the code is correctly synchronized is made, the programmer does not need to worry that reorderings will affect his or her code.

这对程序员来说是一个极强的保证。程序员不需要对重排进行推理来确定他们的代码包含数据竞争。因此，在确定他们的代码是否正确同步时，他们不需要推理重新排序。一旦确定了代码是正确同步的，程序员就不需要担心重排会影响他或她的代码。

A program must be correctly synchronized to avoid the kinds of counterintuitive behaviors that can be observed when code is reordered. The use of correct synchronization does not ensure that the overall behavior of a program is correct. However, its use does allow a programmer to reason about the possible behaviors of a program in a simple way; the behavior of a correctly synchronized program is much less dependent on possible reorderings. Without correct synchronization, very strange, confusing and counterintuitive behaviors are possible.

一个程序必须正确地同步化，以避免在代码重新排序时可能出现的各种反直觉行为。使用正确的同步化并不能确保程序的整体行为是正确的。然而，它的使用确实允许程序员以一种简单的方式来推理程序的可能行为；一个正确同步的程序的行为对可能的重新排序的依赖性要小很多。如果没有正确的同步化，就有可能出现非常奇怪、混乱和反直觉的行为。

We say that a read r of a variable v is allowed to observe a write w to v if, in the happens-before partial order of the execution trace:

我们说，如果在执行跟踪的 happen-before 部分顺序中，一个变量 v 的读 r 被允许观察到对 v 的写 w。

+ r is not ordered before w (i.e., it is not the case that hb(r, w)), and (r 不在 w 之前排序（即不存在 hb(r, w) 的情况），并且)

+ there is no intervening write w' to v (i.e. no write w' to v such that hb(w, w') and hb(w', r)). (没有中间的写 w' 到 v（即没有写 w' 到 v 使 hb(w，w') 和 hb(w'，r)))

Informally, a read r is allowed to see the result of a write w if there is no happens-before ordering to prevent that read.

非正式地，如果没有 happens-before 的排序来阻止读，那么允许读 r 看到写 w 的结果。

A set of actions A is happens-before consistent if for all reads r in A, where W(r) is the write action seen by r, it is not the case that either hb(r, W(r)) or that there exists a write w in A such that w.v = r.v and hb(W(r), w) and hb(w, r).

如果对于 A 中的所有读动作 r，其中 W(r) 是 r 所看到的写动作，不是 hb(r, W(r)) 或者 A 中存在一个写 w，使得 w.v = r.v，并且 hb(W(r), w) 和 hb(w, r) 的情况下，那么一组动作 A 是 happens-before 一致的。

In a happens-before consistent set of actions, each read sees a write that it is allowed to see by the happens-before ordering.

在一个 happens-before 一致的行动集合中，每个读看到的写都是它被 happens-before 排序所允许看到的。

#### Example 17.4.5-1. Happens-before Consistency
For the trace in Table 17.5, initially A == B == 0. The trace can observe r2 == 0 and r1 == 0 and still be happens-before consistent, since there are execution orders that allow each read to see the appropriate write.

对于表 17.5 中的跟踪，最初 A == B == 0。跟踪可以观察到 r2 == 0 和 r1 == 0，并且仍然是 happens-before 一致的，因为存在执行顺序，允许每个读看到适当的写。

Table 17.5. Behavior allowed by happens-before consistency, but not sequential consistency.

Thread 1 | Thread 2
------   | -------
B = 1;   | A = 2;
r2 = A;	 | r1 = B;

Since there is no synchronization, each read can see either the write of the initial value or the write by the other thread. An execution order that displays this behavior is:

由于没有同步，每次读取都可以看到初始值的写入或其他线程的写入。显示这种行为的执行顺序是。

```
1: B = 1;
3: A = 2;
2: r2 = A;  // sees initial write of 0
4: r1 = B;  // sees initial write of 0
```

Another execution order that is happens-before consistent is:

另一个 happens-before 一致的执行顺序是。

```
1: r2 = A;  // sees write of A = 2
3: r1 = B;  // sees write of B = 1
2: B = 1;
4: A = 2;
```

In this execution, the reads see writes that occur later in the execution order. This may seem counterintuitive, but is allowed by happens-before consistency. Allowing reads to see later writes can sometimes produce unacceptable behaviors.

在这种执行中，读看到的是执行顺序中较晚发生的写。这似乎有悖常理，但这是由 happens-before 一致性所允许的。允许读看到后来的写有时会产生不可接受的行为。

### 17.4.6. Executions
An execution E is described by a tuple < P, A, po, so, W, V, sw, hb >, comprising:(一个执行 E 由一个元组 <P, A, po, so, W, V, sw, hb> 描述，包括)

+ P - a program
+ A - a set of actions
+ po - program order, which for each thread t, is a total order over all actions performed by t in A
+ so - synchronization order, which is a total order over all synchronization actions in A
+ W - a write-seen function, which for each read r in A, gives W(r), the write action seen by r in E.
+ V - a value-written function, which for each write w in A, gives V(w), the value written by w in E.
+ sw - synchronizes-with, a partial order over synchronization actions
+ hb - happens-before, a partial order over actions

Note that the synchronizes-with and happens-before elements are uniquely determined by the other components of an execution and the rules for well-formed executions (§17.4.7).

请注意，synchronizes-with 和 happen-before 元素是由执行的其他组件和良好形式的执行规则（§17.4.7）唯一确定的。

An execution is happens-before consistent if its set of actions is happens-before consistent (§17.4.5).

如果一个执行的行动集是 happens-before 一致的，那么它就是 happens-before 一致的（§17.4.5）。

### 17.4.7. Well-Formed Executions
We only consider well-formed executions. An execution E = < P, A, po, so, W, V, sw, hb > is well formed if the following conditions are true:

我们只考虑形式良好的执行。如果以下条件为真，那么一个执行 E = < P, A, po, so, W, V, sw, hb >是执行良好的。

1. Each read sees a write to the same variable in the execution. (在执行过程中，每次读都会看到对同一变量的写)

    All reads and writes of volatile variables are volatile actions. For all reads r in A, we have W(r) in A and W(r).v = r.v. The variable r.v is volatile if and only if r is a volatile read, and the variable w.v is volatile if and only if w is a volatile write. (所有对 volatile 变量的读和写都是 volatile 动作。对于 A 中的所有读 r，我们在 A 中有 W(r)，并且 W(r).v = r.v。当且仅当 r 是一个 volatile 读时，变量 r.v 是 volatile的，并且当且仅当 w 是一个 volatile 写时，变量 w.v 是 volatile 的。)

2. The happens-before order is a partial order. (happens-before 的顺序是一个部分顺序)

    The happens-before order is given by the transitive closure of synchronizes-with edges and program order. It must be a valid partial order: reflexive, transitive and antisymmetric. (happens-before 顺序是由synchronizes-with 边和程序顺序的反义闭合给出的。它必须是一个有效的部分顺序：反身的、转折的和不对称的。)

3. The execution obeys intra-thread consistency. (执行服从线程内的一致性。)

    For each thread t, the actions performed by t in A are the same as would be generated by that thread in program-order in isolation, with each write w writing the value V(w), given that each read r sees the value V(W(r)). Values seen by each read are determined by the memory model. The program order given must reflect the program order in which the actions would be performed according to the intra-thread semantics of P. (对于每个线程 t 来说，t 在 A 中执行的动作与该线程在孤立情况下按程序顺序产生的动作相同，每个写 w 写出的值为 V(w)，鉴于每个读 r 看到的值为 V(W(r))。每个读所看到的值是由内存模型决定的。给出的程序顺序必须反映出根据 P 的线程内语义执行这些动作的程序顺序。)

4. The execution is happens-before consistent (§17.4.6). (执行是 happens-before 一致的（§17.4.6）)

5. The execution obeys synchronization-order consistency. (执行过程中遵守同步顺序的一致性)
    
    For all volatile reads r in A, it is not the case that either so(r, W(r)) or that there exists a write w in A such that w.v = r.v and so(W(r), w) and so(w, r). (对于 A 中所有不稳定的读 r，并不是 so(r, W(r)) 或者 A 中存在一个写 w，使得 w.v = r.v，并且 so(W(r), w) 和 so(w, r)。)

### 17.4.8. Executions and Causality Requirements
We use f|d to denote the function given by restricting the domain of f to d. For all x in d, f|d(x) = f(x), and for all x not in d, f|d(x) is undefined.

我们用 f|d 来表示将 f 的域限制为 d 所给出的函数。对于 d 中的所有 x，f|d(x) = f(x)，而对于不在 d 中的所有 x，f|d(x) 是未定义的。

We use p|d to represent the restriction of the partial order p to the elements in d. For all x,y in d, p(x,y) if and only if p|d(x,y). If either x or y are not in d, then it is not the case that p|d(x,y).

我们用 p|d 来表示偏序 p 对 d 中元素的限制。对于 d 中的所有 x, y，p(x,y) 当且仅当 p|d(x,y)。如果 x 或 y 不在 d 中，那么就不是 p|d(x,y) 的情况。

A well-formed execution E = < P, A, po, so, W, V, sw, hb > is validated by committing actions from A. If all of the actions in A can be committed, then the execution satisfies the causality requirements of the Java programming language memory model.

一个格式良好的执行 E = < P, A, po, so, W, V, sw, hb >是通过提交 A 中的动作来验证的。如果 A 中的所有动作都能被提交，那么这个执行就满足了 Java 编程语言内存模型的因果关系要求。

Starting with the empty set as C0, we perform a sequence of steps where we take actions from the set of actions A and add them to a set of committed actions Ci to get a new set of committed actions Ci+1. To demonstrate that this is reasonable, for each Ci we need to demonstrate an execution E containing Ci that meets certain conditions.

从作为 C0 的空集开始，我们执行一连串的步骤，从行动集 A 中抽取行动，并将它们加入到已承诺的行动集Ci 中，得到一个新的已承诺行动集 Ci + 1。为了证明这一点是合理的，对于每一个 Ci，我们需要证明一个包含 Ci 的、符合某些条件的执行 E。

Formally, an execution E satisfies the causality requirements of the Java programming language memory model if and only if there exist:

从形式上看，当且仅当存在一个执行 E 时，它满足了 Java 编程语言内存模型的因果关系要求。

+ Sets of actions C0, C1, ... such that:
  + C0 is the empty set
  + Ci is a proper subset of Ci + 1
  + A = ∪ (C0, C1, ...)
    If A is finite, then the sequence C0, C1, ... will be finite, ending in a set Cn = A.

    If A is infinite, then the sequence C0, C1, ... may be infinite, and it must be the case that the union of all elements of this infinite sequence is equal to A.

+ Well-formed executions E1, ..., where Ei = < P, Ai, poi, soi, Wi, Vi, swi, hbi >.

Given these sets of actions C0, ... and executions E1, ... , every action in Ci must be one of the actions in Ei. All actions in Ci must share the same relative happens-before order and synchronization order in both Ei and E. Formally:

鉴于这些行动集 C0，...和执行集 E1，... 中的每个动作都必须是 Ei 中的一个动作。Ci 中的所有行动必须在 Ei 和 E 中共享相同的相对 happens-before 顺序和同步顺序。

1. Ci is a subset of Ai

2. hbi|Ci = hb|Ci

3. soi|Ci = so|Ci

The values written by the writes in Ci must be the same in both Ei and E. Only the reads in Ci-1 need to see the same writes in Ei as in E. Formally:

在 Ci 中写的值在 Ei 和 E 中必须是相同的。只有 Ci - 1 中的读需要在 Ei 中看到与 E 中相同的写。

4. Vi|Ci = V|Ci

5. Wi|Ci-1 = W|Ci-1

All reads in Ei that are not in Ci-1 must see writes that happen-before them. Each read r in Ci - Ci-1 must see writes in Ci-1 in both Ei and E, but may see a different write in Ei from the one it sees in E. Formally:

Ei 中所有不在 Ci - 1 中的读必须看到发生在它们之前的写。在 Ci-Ci-1 中的每个读必须在 Ei 和 E 中看到 Ci-1 中的写，但在 Ei 中看到的写可能与在 E 中看到的不同。

6. For any read r in Ai - Ci-1, we have hbi(Wi(r), r)

7. For any read r in (Ci - Ci-1), we have Wi(r) in Ci-1 and W(r) in Ci-1

Given a set of sufficient synchronizes-with edges for Ei, if there is a release-acquire pair that happens-before (§17.4.5) an action you are committing, then that pair must be present in all Ej, where j ≥ i. Formally:

给定 Ei 的一组充分同步边，如果有一个释放-获取对发生在（§17.4.5）你正在实施的行动之前，那么该对必须存在于所有 Ej 中，其中 j ≥ i。

8. Let sswi be the swi edges that are also in the transitive reduction of hbi but not in po. We call sswi the sufficient synchronizes-with edges for Ei. If sswi(x, y) and hbi(y, z) and z in Ci, then swj(x, y) for all j ≥ i. (让 sswi 成为也在 hbi 的反式还原中但不在 po 中的 swi 边。我们称 sswi 为 Ei 的充分同步化边。如果 sswi(x, y) 和 hbi(y, z) 以及 z 在 Ci 中，那么 swj(x, y) 对于所有 j ≥ i。)

   If an action y is committed, all external actions that happen-before y are also committed. (如果一个行动 y 被提交，所有发生在 y 之前的外部行动也被提交。)

9. If y is in Ci, x is an external action and hbi(x, y), then x in Ci.

#### Example 17.4.8-1. Happens-before Consistency Is Not Sufficient
Happens-before consistency is a necessary, but not sufficient, set of constraints. Merely enforcing happens-before consistency would allow for unacceptable behaviors - those that violate the requirements we have established for programs. For example, happens-before consistency allows values to appear "out of thin air". This can be seen by a detailed examination of the trace in Table 17.6.

happens-before 的一致性是一套必要的，但不是充分的约束。仅仅强制执行 happens-before 一致性会允许不可接受的行为--那些违反了我们为程序建立的要求的行为。例如，发生前的一致性允许数值 "凭空"出现。通过对表 17.6 中的跟踪的详细检查，我们可以看到这一点。

Table 17.6. Happens-before consistency is not sufficient

Thread 1 | Thread 2
-------  | -------
r1 = x;	 | r2 = y;
if (r1 != 0) y = 1;	| if (r2 != 0) x = 1;

The code shown in Table 17.6 is correctly synchronized. This may seem surprising, since it does not perform any synchronization actions. Remember, however, that a program is correctly synchronized if, when it is executed in a sequentially consistent manner, there are no data races. If this code is executed in a sequentially consistent way, each action will occur in program order, and neither of the writes will occur. Since no writes occur, there can be no data races: the program is correctly synchronized.

表 17.6 中所示的代码是正确的同步。这似乎令人惊讶，因为它没有执行任何同步操作。然而，请记住，如果程序以顺序一致的方式执行时，不存在数据竞争，那么它就是正确的同步。如果这段代码以顺序一致的方式执行，每个动作都会按程序顺序发生，而且都不会发生写操作。由于没有写操作发生，所以不可能有数据竞争：程序是正确同步的。

Since this program is correctly synchronized, the only behaviors we can allow are sequentially consistent behaviors. However, there is an execution of this program that is happens-before consistent, but not sequentially consistent:

由于这个程序是正确同步的，我们能允许的唯一行为是顺序一致的行为。然而，这个程序有一个执行是 happens-before 一致的，但不是顺序一致的。

```
r1 = x;  // sees write of x = 1
y = 1;
r2 = y;  // sees write of y = 1
x = 1; 
```

This result is happens-before consistent: there is no happens-before relationship that prevents it from occurring. However, it is clearly not acceptable: there is no sequentially consistent execution that would result in this behavior. The fact that we allow a read to see a write that comes later in the execution order can sometimes thus result in unacceptable behaviors.

这个结果在 happens-before 是一致的：没有任何 happens-before 的关系可以阻止它发生。然而，这显然是不可接受的：没有任何顺序上一致的执行会导致这种行为。事实上，我们允许读看到执行顺序中较晚的写，这有时会导致不可接受的行为。

Although allowing reads to see writes that come later in the execution order is sometimes undesirable, it is also sometimes necessary. As we saw above, the trace in Table 17.5 requires some reads to see writes that occur later in the execution order. Since the reads come first in each thread, the very first action in the execution order must be a read. If that read cannot see a write that occurs later, then it cannot see any value other than the initial value for the variable it reads. This is clearly not reflective of all behaviors.

尽管允许读看到执行顺序中较晚的写数有时并不可取，但有时也是必要的。正如我们在上面看到的，表 17.5 中的跟踪需要一些读来看到执行顺序中较晚出现的写。由于每个线程都是先读后写，执行顺序中的第一个动作必须是读。如果这个读不能看到后来发生的写，那么它就不能看到它所读的变量的初始值以外的任何其他值。这显然不是所有行为的反映。

We refer to the issue of when reads can see future writes as causality, because of issues that arise in cases like the one found in Table 17.6. In that case, the reads cause the writes to occur, and the writes cause the reads to occur. There is no "first cause" for the actions. Our memory model therefore needs a consistent way of determining which reads can see writes early.

我们把读何时能看到未来的写的问题称为因果关系，因为在像表 17.6 中发现的情况下会出现问题。在这种情况下，读会导致写的发生，而写会导致读的发生。这些行为没有"第一原因"。因此，我们的内存模型需要一种一致的方式来确定哪些读可以提前看到写。

Examples such as the one found in Table 17.6 demonstrate that the specification must be careful when stating whether a read can see a write that occurs later in the execution (bearing in mind that if a read sees a write that occurs later in the execution, it represents the fact that the write is actually performed early).

像表 17.6 中的例子表明，在说明读是否能看到执行中较晚发生的写时，规范必须谨慎（请记住，如果读看到执行中较晚发生的写，则代表该写实际上是提前执行的）。

The memory model takes as input a given execution, and a program, and determines whether that execution is a legal execution of the program. It does this by gradually building a set of "committed" actions that reflect which actions were executed by the program. Usually, the next action to be committed will reflect the next action that can be performed by a sequentially consistent execution. However, to reflect reads that need to see later writes, we allow some actions to be committed earlier than other actions that happen-before them.

内存模型将一个给定的执行和程序作为输入，并确定该执行是否是程序的合法执行。它通过逐步建立一套 "已提交"的行动来反映程序执行了哪些行动。通常情况下，下一个被提交的动作将反映出顺序一致的执行所能执行的下一个动作。然而，为了反映需要看到后来的写的读，我们允许一些动作比发生在它们之前的其他动作更早被提交。

Obviously, some actions may be committed early and some may not. If, for example, one of the writes in Table 17.6 were committed before the read of that variable, the read could see the write, and the "out-of-thin-air" result could occur. Informally, we allow an action to be committed early if we know that the action can occur without assuming some data race occurs. In Table 17.6, we cannot perform either write early, because the writes cannot occur unless the reads see the result of a data race.

很明显，有些动作可能会提前提交，有些则不会。例如，如果表17.6 中的一个写操作在读取该变量之前被提交，那么读取可以看到这个写操作，就会出现 "空中楼阁 "的结果。非正式地，如果我们知道一个动作可以在不假设发生某种数据竞争的情况下发生，我们就允许该动作提前提交。在表 17.6 中，我们不能提前执行任何一个写操作，因为除非读操作看到数据竞争的结果，否则写操作不可能发生。

### 17.4.9. Observable Behavior and Nonterminating Executions
For programs that always terminate in some bounded finite period of time, their behavior can be understood (informally) simply in terms of their allowable executions. For programs that can fail to terminate in a bounded amount of time, more subtle issues arise.

对于总是在某个有界的有限时间内终止的程序，它们的行为可以（非正式地）简单地理解为它们的可执行性。对于那些在一定时间内不能终止的程序，就会出现更微妙的问题。

The observable behavior of a program is defined by the finite sets of external actions that the program may perform. A program that, for example, simply prints "Hello" forever is described by a set of behaviors that for any non-negative integer i, includes the behavior of printing "Hello" i times.

一个程序的可观察行为是由该程序可能执行的有限的外部行为集定义的。例如，一个简单地永远打印 "Hello "的程序是由一组行为描述的，对于任何非负整数i，包括打印 "Hello "i 次的行为。

Termination is not explicitly modeled as a behavior, but a program can easily be extended to generate an additional external action executionTermination that occurs when all threads have terminated.

终止没有被明确地建模为一种行为，但一个程序可以很容易地被扩展以生成一个额外的外部动作executionTermination，该动作在所有线程都终止时发生。

We also define a special hang action. If behavior is described by a set of external actions including a hang action, it indicates a behavior where after the external actions are observed, the program can run for an unbounded amount of time without performing any additional external actions or terminating. Programs can hang if all threads are blocked or if the program can perform an unbounded number of actions without performing any external actions.

我们还定义了一个特殊的挂起动作。如果行为是由包括挂起动作在内的一组外部动作描述的，它表示的行为是在观察到外部动作后，程序可以运行不受限制的时间而不执行任何额外的外部动作或终止。如果所有的线程都被阻塞，或者程序可以在不执行任何外部动作的情况下执行不受限制的动作，程序就会挂起。

A thread can be blocked in a variety of circumstances, such as when it is attempting to acquire a lock or perform an external action (such as a read) that depends on external data.

一个线程可以在各种情况下被阻塞，例如当它试图获得一个锁或执行一个依赖于外部数据的外部动作（如读取）时。

An execution may result in a thread being blocked indefinitely and the execution's not terminating. In such cases, the actions generated by the blocked thread must consist of all actions generated by that thread up to and including the action that caused the thread to be blocked, and no actions that would be generated by the thread after that action.

一个执行可能会导致一个线程被无限期地阻塞，而执行的没有终止。在这种情况下，被阻塞的线程产生的动作必须包括该线程产生的所有动作，直到并包括导致该线程被阻塞的动作，以及该线程在该动作之后将产生的任何动作。

To reason about observable behaviors, we need to talk about sets of observable actions.

为了推理可观察的行为，我们需要谈论可观察行为的集合。

If O is a set of observable actions for an execution E, then set O must be a subset of E's actions, A, and must contain only a finite number of actions, even if A contains an infinite number of actions. Furthermore, if an action y is in O, and either hb(x, y) or so(x, y), then x is in O.

如果 O 是一个执行 E 的可观察行动集，那么 O 集一定是 E 的行动 A 的一个子集，而且一定只包含有限的行动，即使 A 包含无限多的行动。此外，如果一个行动 y 在 O 中，并且 hb(x, y) 或 so(x, y)，那么x 就在 O 中。

Note that a set of observable actions are not restricted to external actions. Rather, only external actions that are in a set of observable actions are deemed to be observable external actions.

请注意，一组可观察的行动并不限于外部行动。相反，只有在一组可观察行动中的外部行动才被认为是可观察的外部行动。

A behavior B is an allowable behavior of a program P if and only if B is a finite set of external actions and either:

当且仅当 B 是一个外部行动的有限集合，并且是其中之一时，行为 B 就是程序 P 的可允许行为。

There exists an execution E of P, and a set O of observable actions for E, and B is the set of external actions in O (If any threads in E end in a blocked state and O contains all actions in E, then B may also contain a hang action); or

存在一个 P 的执行 E，和一个 E 的可观察动作集 O，B 是 O 中的外部动作集（如果E中的任何线程以阻塞状态结束，O 包含 E 中的所有动作，那么 B 也可能包含一个挂起的动作）；或者

There exists a set O of actions such that B consists of a hang action plus all the external actions in O and for all k ≥ | O |, there exists an execution E of P with actions A, and there exists a set of actions O' such that:

存在一个行动集 O，使得 B 由一个挂起的行动加上 O 中的所有外部行动组成，对于所有 k ≥ |O|，存在一个带有行动 A 的 P 的执行 E，并且存在一个行动集 O'，使得。

+ Both O and O' are subsets of A that fulfill the requirements for sets of observable actions. (O 和 O'都是 A 的子集，满足可观察行动集的要求。)

+ O ⊆ O' ⊆ A

+ | O' | ≥ k

+ O' - O contains no external actions

Note that a behavior B does not describe the order in which the external actions in B are observed, but other (internal) constraints on how the external actions are generated and performed may impose such constraints.

请注意，一个行为 B 并不描述观察 B 中的外部行动的顺序，但关于外部行动如何产生和执行的其他（内部）约束可能会施加这种约束。

## 17.5. final Field Semantics
Fields declared final are initialized once, but never changed under normal circumstances. The detailed semantics of final fields are somewhat different from those of normal fields. In particular, compilers have a great deal of freedom to move reads of final fields across synchronization barriers and calls to arbitrary or unknown methods. Correspondingly, compilers are allowed to keep the value of a final field cached in a register and not reload it from memory in situations where a non-final field would have to be reloaded.

声明为 final 的字段被初始化一次，但在正常情况下不会改变。final 字段的详细语义与正常字段的语义有些不同。特别是，编译器有很大的自由，可以跨越同步障碍和对任意或未知方法的调用来读取最终字段。相应地，编译器可以将 final 字段的值保存在寄存器中，在非 final 字段必须被重新加载的情况下，不从内存中重新加载它。

final fields also allow programmers to implement thread-safe immutable objects without synchronization. A thread-safe immutable object is seen as immutable by all threads, even if a data race is used to pass references to the immutable object between threads. This can provide safety guarantees against misuse of an immutable class by incorrect or malicious code. final fields must be used correctly to provide a guarantee of immutability.

final 字段还允许程序员实现线程安全的不可变对象而不需要同步。一个线程安全的不可变对象会被所有线程视为不可变的，即使线程之间使用数据竞争来传递对不可变对象的引用。这可以提供安全保障，防止不正确的或恶意的代码滥用不可变的类。最终字段必须正确使用，以提供不可变性的保障。

An object is considered to be completely initialized when its constructor finishes. A thread that can only see a reference to an object after that object has been completely initialized is guaranteed to see the correctly initialized values for that object's final fields.

当一个对象的构造函数完成时，它就被认为是完全初始化了。一个线程只有在一个对象被完全初始化后才能看到对该对象的引用，并保证能看到该对象 final 字段的正确初始化值。

The usage model for final fields is a simple one: Set the final fields for an object in that object's constructor; and do not write a reference to the object being constructed in a place where another thread can see it before the object's constructor is finished. If this is followed, then when the object is seen by another thread, that thread will always see the correctly constructed version of that object's final fields. It will also see versions of any object or array referenced by those final fields that are at least as up-to-date as the final fields are.

final 字段的使用模式很简单。在一个对象的构造函数中设置该对象的 final 字段；在该对象的构造函数完成之前，不要把正在构造的对象的引用写到其他线程可以看到的地方。如果这样做了，那么当该对象被其他线程看到时，该线程将始终看到该对象 final 字段的正确构造版本。它还将看到由这些 final 字段引用的任何对象或数组的版本，这些版本至少与 final 字段一样是最新的。

### Example 17.5-1. final Fields In The Java Memory Model
The program below illustrates how final fields compare to normal fields.

下面的程序说明了 final 字段与正常字段的对比情况。

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
            int i = f.x;  // guaranteed to see 3  
            int j = f.y;  // could see 0
        } 
    } 
}
```

The class FinalFieldExample has a final int field x and a non-final int field y. One thread might execute the method writer and another might execute the method reader.

Because the writer method writes f after the object's constructor finishes, the reader method will be guaranteed to see the properly initialized value for f.x: it will read the value 3. However, f.y is not final; the reader method is therefore not guaranteed to see the value 4 for it.

### Example 17.5-2. final Fields For Security
final fields are designed to allow for necessary security guarantees. Consider the following program. One thread (which we shall refer to as thread 1) executes:

final 的字段是为了允许必要的安全保证。考虑一下下面的程序。一个线程（我们将称为线程 1）执行。

```
Global.s = "/tmp/usr".substring(4);
```

while another thread (thread 2) executes

```
String myS = Global.s; 
if (myS.equals("/tmp"))System.out.println(myS);
```

String objects are intended to be immutable and string operations do not perform synchronization. While the String implementation does not have any data races, other code could have data races involving the use of String objects, and the memory model makes weak guarantees for programs that have data races. In particular, if the fields of the String class were not final, then it would be possible (although unlikely) that thread 2 could initially see the default value of 0 for the offset of the string object, allowing it to compare as equal to "/tmp". A later operation on the String object might see the correct offset of 4, so that the String object is perceived as being "/usr". Many security features of the Java programming language depend upon String objects being perceived as truly immutable, even if malicious code is using data races to pass String references between threads.

字符串对象的目的是不可改变的，字符串操作不执行同步。虽然 String 的实现没有任何数据竞争，但其他代码可能有涉及使用 String 对象的数据竞争，而且内存模型对有数据竞争的程序做了弱保证。特别是，如果String 类的字段不是 final 的，那么线程 2 有可能（尽管不太可能）最初看到字符串对象的偏移量的默认值为 0，允许它比较为等于 "/tmp"。后来对字符串对象的操作可能会看到正确的偏移量 4，所以字符串对象被认为是 "/usr"。Java 编程语言的许多安全特性依赖于 String 对象被认为是真正不可改变的，即使恶意代码在线程之间使用数据竞争来传递 String 引用。

### 17.5.1. Semantics of final Fields
Let o be an object, and c be a constructor for o in which a final field f is written. A freeze action on final field f of o takes place when c exits, either normally or abruptly.

让 o 是一个对象，c 是 o 的一个构造函数，其中写有一个最终字段 f。当 c 正常或突然退出时，对 o 的final 字段 f 的冻结动作就会发生。

Note that if one constructor invokes another constructor, and the invoked constructor sets a final field, the freeze for the final field takes place at the end of the invoked constructor.

注意，如果一个构造函数调用另一个构造函数，而被调用的构造函数设置了一个 final 字段，那么 final 字段的冻结就发生在被调用的构造函数的末尾。

For each execution, the behavior of reads is influenced by two additional partial orders, the dereference chain dereferences() and the memory chain mc(), which are considered to be part of the execution (and thus, fixed for any particular execution). These partial orders must satisfy the following constraints (which need not have a unique solution):

对于每个执行，读的行为受到两个额外的部分命令的影响，即 dereference 链 dereferences() 和内存链 mc()，它们被认为是执行的一部分（因此，对于任何特定的执行都是固定的）。这些部分命令必须满足以下约束（不需要有唯一的解决方案）。

Dereference Chain: If an action a is a read or write of a field or element of an object o by a thread t that did not initialize o, then there must exist some read r by thread t that sees the address of o such that r dereferences(r, a).

解引用链。如果一个动作 a 是由没有初始化 o 的线程 t 对对象 o 的一个字段或元素进行的读或写，那么一定存在一些由线程 t 进行的读 r，它看到了 o 的地址，从而使 r dereferences(r, a)。

Memory Chain: There are several constraints on the memory chain ordering:

内存链。对内存链的排序有几个限制。

+ If r is a read that sees a write w, then it must be the case that mc(w, r). (如果 r 是一个看到写 w 的读，那么一定是 mc(w, r) 的情况)

+ If r and a are actions such that dereferences(r, a), then it must be the case that mc(r, a). 

+ If w is a write of the address of an object o by a thread t that did not initialize o, then there must exist some read r by thread t that sees the address of o such that mc(r, w). (如果 w 是一个没有初始化 o 的线程 t 对一个对象 o 的地址的写，那么一定存在一些线程 t 的读 r，它可以看到 o 的地址，这样 mc(r, w))

Given a write w, a freeze f, an action a (that is not a read of a final field), a read r1 of the final field frozen by f, and a read r2 such that hb(w, f), hb(f, a), mc(a, r1), and dereferences(r1, r2), then when determining which values can be seen by r2, we consider hb(w, r2). (This happens-before ordering does not transitively close with other happens-before orderings.)

给定一个写 w，一个冻结 f，一个动作 a（不是对 final 字段的读），一个对 f 冻结的 final 字段的读 r1，以及一个读 r2，使得 hb(w, f), hb(f, a), mc(a, r1), 和 dereferences(r1, r2)，那么当确定哪些值可以被 r2 看到时，我们考虑 hb(w, r2)。(这个 happens-before 的排序与其他 happens-before 的排序不发生关系。)

Note that the dereferences order is reflexive, and r1 can be the same as r2.

需要注意的是，转述的顺序是反身的，r1 可以和 r2 一样。

For reads of final fields, the only writes that are deemed to come before the read of the final field are the ones derived through the final field semantics.

对于 final 字段的读取，唯一被认为是在 final 字段的读取之前的写入是通过 final 字段语义派生的。

### 17.5.2. Reading final Fields During Construction
A read of a final field of an object within the thread that constructs that object is ordered with respect to the initialization of that field within the constructor by the usual happens-before rules. If the read occurs after the field is set in the constructor, it sees the value the final field is assigned, otherwise it sees the default value.

在构造对象的线程中，对该对象的 final 字段的读取是按照通常的 happens-before 的规则，相对于构造函数中该字段的初始化而言的。如果读取发生在构造函数中设置该字段之后，它将看到 final 字段被分配的值，否则它将看到默认值。

### 17.5.3. Subsequent Modification of final Fields
In some cases, such as deserialization, the system will need to change the final fields of an object after construction. final fields can be changed via reflection and other implementation-dependent means. The only pattern in which this has reasonable semantics is one in which an object is constructed and then the final fields of the object are updated. The object should not be made visible to other threads, nor should the final fields be read, until all updates to the final fields of the object are complete. Freezes of a final field occur both at the end of the constructor in which the final field is set, and immediately after each modification of a final field via reflection or other special mechanism.

在某些情况下，比如反序列化，系统需要在构造后改变对象的 final 字段。final 字段可以通过反射和其他依赖于实现的方式来改变。唯一具有合理语义的模式是构建对象，然后更新对象的 final 字段。在对对象的 final 字段的所有更新完成之前，该对象不应该被其他线程看到，也不应该被读取 final 字段。final 字段的冻结既发生在设置 final 字段的构造函数的末尾，也发生在通过反射或其他特殊机制对 final 字段的每次修改之后。

Even then, there are a number of complications. If a final field is initialized to a compile-time constant expression (§15.28) in the field declaration, changes to the final field may not be observed, since uses of that final field are replaced at compile time with the value of the constant expression.

即使如此，也有一些复杂的情况。如果一个 final 字段在字段声明中被初始化为一个编译时常量表达式（§15.28），那么对 final 字段的改变可能不会被观察到，因为对该 final 字段的使用在编译时被替换成常量表达式的值。

Another problem is that the specification allows aggressive optimization of final fields. Within a thread, it is permissible to reorder reads of a final field with those modifications of a final field that do not take place in the constructor.

另一个问题是，该规范允许对 final 字段进行积极的优化。在一个线程中，允许将 final 字段的读取与那些不在构造函数中发生的对 final 字段的修改重新排序。

```
// Example 17.5.3-1. Aggressive Optimization of final Fields
class A {
    final int x;
    A() { 
        x = 1; 
    } 

    int f() { 
        return d(this,this); 
    } 

    int d(A a1, A a2) { 
        int i = a1.x; 
        g(a1); 
        int j = a2.x; 
        return j - i; 
    }

    static void g(A a) { 
        // uses reflection to change a.x to 2 
    } 
}

// In the d method, the compiler is allowed to reorder the reads of x and the call to g freely. 
// Thus, new A().f() could return -1, 0, or 1.
```

An implementation may provide a way to execute a block of code in a final-field-safe context. If an object is constructed within a final-field-safe context, the reads of a final field of that object will not be reordered with modifications of that final field that occur within that final-field-safe context.

一个实现可以提供一种方法来在 final 字段安全上下文中执行一个代码块。如果一个对象是在final-field-safe 上下文中构造的，那么该对象的 final 字段的读将不会随着该 final 字段在final-field-safe 上下文中的修改而被重新排序。

A final-field-safe context has additional protections. If a thread has seen an incorrectly published reference to an object that allows the thread to see the default value of a final field, and then, within a final-field-safe context, reads a properly published reference to the object, it will be guaranteed to see the correct value of the final field. In the formalism, code executed within a final-field-safe context is treated as a separate thread (for the purposes of final field semantics only).

final-field-safe 上下文有额外的保护措施。如果一个线程看到了一个错误发布的对象的引用，允许该线程看到一个 final 字段的默认值，然后，在一个 final 字段安全上下文中，读取一个正确发布的对象的引用，它将被保证看到 final 字段的正确值。在形式主义中，在 final-field-safe 上下文中执行的代码被视为一个单独的线程（仅用于 final field 语义的目的）。

In an implementation, a compiler should not move an access to a final field into or out of a final-field-safe context (although it can be moved around the execution of such a context, so long as the object is not constructed within that context).

在实现中，编译器不应该将对 final 字段的访问移入或移出 final 字段安全上下文（尽管它可以在执行这样的上下文时被移动，只要对象不是在该上下文中构造的）。

One place where use of a final-field-safe context would be appropriate is in an executor or thread pool. By executing each Runnable in a separate final-field-safe context, the executor could guarantee that incorrect access by one Runnable to a object o will not remove final field guarantees for other Runnables handled by the same executor.

在执行器或线程池中，适合使用 final 字段安全上下文的地方是。通过在一个单独的 final 字段安全上下文中执行每个 Runnable，执行器可以保证一个 Runnable 对一个对象 o 的错误访问不会消除由同一个执行器处理的其他 Runnable 的 final 字段保证。

### 17.5.4. Write-protected Fields
Normally, a field that is final and static may not be modified. However, System.in, System.out, and System.err are static final fields that, for legacy reasons, must be allowed to be changed by the methods System.setIn, System.setOut, and System.setErr. We refer to these fields as being write-protected to distinguish them from ordinary final fields.

通常情况下，一个静态的 final 字段是不能被修改的。然而，System.in、System.out 和 System.err是静态的 final 字段，由于遗留的原因，必须允许通过 System.setIn、System.setOut 和 System.setErr 等方法进行修改。我们把这些字段称为受写保护的字段，以区别于普通的 final 字段。

The compiler needs to treat these fields differently from other final fields. For example, a read of an ordinary final field is "immune" to synchronization: the barrier involved in a lock or volatile read does not have to affect what value is read from a final field. Since the value of write-protected fields may be seen to change, synchronization events should have an effect on them. Therefore, the semantics dictate that these fields be treated as normal fields that cannot be changed by user code, unless that user code is in the System class.

编译器需要将这些字段与其他 final 字段区别对待。例如，对普通 final 字段的读取对同步是"免疫"的：锁或 volatile 读取所涉及的障碍不必影响从 final 字段中读取什么值。由于受写保护的字段的值可能会被看到发生变化，同步事件应该对它们产生影响。因此，语义决定了这些字段应被视为不能被用户代码改变的正常字段，除非该用户代码是在系统类中。

## 17.6. Word Tearing
One consideration for implementations of the Java Virtual Machine is that every field and array element is considered distinct; updates to one field or element must not interact with reads or updates of any other field or element. In particular, two threads that update adjacent elements of a byte array separately must not interfere or interact and do not need synchronization to ensure sequential consistency.

对 Java 虚拟机实现的一个考虑是，每个字段和数组元素都被认为是独立的；对一个字段或元素的更新不得与任何其他字段或元素的读取或更新相互影响。特别是，两个分别更新一个字节数组的相邻元素的线程必须不相互干扰或相互作用，不需要同步以确保顺序一致性。

Some processors do not provide the ability to write to a single byte. It would be illegal to implement byte array updates on such a processor by simply reading an entire word, updating the appropriate byte, and then writing the entire word back to memory. This problem is sometimes known as word tearing, and on processors that cannot easily update a single byte in isolation some other approach will be required.

一些处理器不提供写到单个字节的能力。在这样的处理器上，通过简单地读取整个字，更新适当的字节，然后将整个字写回内存，来实现字节阵列的更新是不合法的。这个问题有时被称为 "字撕裂"，在那些不能轻易单独更新一个字节的处理器上，需要采用其他方法。

```
// Example 17.6-1. Detection of Word Tearing

// The following program is a test case to detect word tearing:

public class WordTearing extends Thread { 
    static final int LENGTH = 8;
    static final int ITERS  = 1000000; 
    static byte[] counts    = new byte[LENGTH]; 
    static Thread[] threads = new Thread[LENGTH]; 

    final int id; 
    WordTearing(int i) { 
        id = i; 
    }

    public void run() { 
        byte v = 0; 
        for (int i = 0; i < ITERS; i++) { 
            byte v2 = counts[id]; 
            if (v != v2) { 
                System.err.println("Word-Tearing found: " + 
                              "counts[" + id + "] = "+ v2 +
                              ", should be " + v); 
                return; 
            } 
            v++; 
            counts[id] = v; 
        } 
    }

    public static void main(String[] args) { 
        for (int i = 0; i < LENGTH; ++i) 
            (threads[i] = new WordTearing(i)).start(); 
    } 
}

// This makes the point that bytes must not be overwritten by writes to adjacent bytes.
```

## 17.7. Non-atomic Treatment of double and long
For the purposes of the Java programming language memory model, a single write to a non-volatile long or double value is treated as two separate writes: one to each 32-bit half. This can result in a situation where a thread sees the first 32 bits of a 64-bit value from one write, and the second 32 bits from another write.

就 Java 编程语言的内存模型而言，对非 volatile long 或 double 值的单次写入被视为两个独立的写入：每个 32 位的一半。这可能会导致这样一种情况：一个线程从一次写入中看到一个 64 位值的前 32 位，而从另一次写入中看到后 32 位。

Writes and reads of volatile long and double values are always atomic.

写入和读取 volatile 的 long 和 double 值始终是原子性的。

Writes to and reads of references are always atomic, regardless of whether they are implemented as 32-bit or 64-bit values.

对引用的写和读总是原子的，不管它们是作为 32 位还是 64 位的值来实现。

Some implementations may find it convenient to divide a single write action on a 64-bit long or double value into two write actions on adjacent 32-bit values. For efficiency's sake, this behavior is implementation-specific; an implementation of the Java Virtual Machine is free to perform writes to long and double values atomically or in two parts.

一些实现可能会发现，将一个 64 位长值或双倍值的单一写操作分为两个相邻的 32 位值的写操作是很方便的。为了提高效率，这种行为是特定于实现的；Java 虚拟机的实现可以自由地以原子方式或分两部分来执行对长值和双值的写入。

Implementations of the Java Virtual Machine are encouraged to avoid splitting 64-bit values where possible. Programmers are encouraged to declare shared 64-bit values as volatile or synchronize their programs correctly to avoid possible complications.

我们鼓励 Java 虚拟机的实现尽可能避免分割 64 位值。我们鼓励程序员将共享的 64 位值声明为 volatile，或者正确地同步他们的程序，以避免可能的复杂情况。
