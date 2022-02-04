## The "Double-Checked Locking is Broken" Declaration

Double-Checked Locking is widely cited and used as an efficient method for implementing lazy initialization in a multithreaded environment.

双重校验锁被广泛引用，并被用作在多线程环境中实现懒惰初始化的有效方法。

Unfortunately, it will not work reliably in a platform independent way when implemented in Java, without additional synchronization. When implemented in other languages, such as C++, it depends on the memory model of the processor, the reorderings performed by the compiler and the interaction between the compiler and the synchronization library. Since none of these are specified in a language such as C++, little can be said about the situations in which it will work. Explicit memory barriers can be used to make it work in C++, but these barriers are not available in Java.

不幸的是，当用 Java 实现时，如果没有额外的同步，它将不能以独立于平台的方式可靠地工作。当用其他语言（如 C++）实现时，它取决于处理器的内存模型、编译器进行的重新排序以及编译器和同步库之间的交互。由于这些在 C++ 等语言中都没有规定，所以几乎不能说它会在什么情况下工作。在 C++ 中可以使用显式内存屏障来使其工作，但这些屏障在 Java 中是不存在的。

To first explain the desired behavior, consider the following code:

首先解释一下所需的行为，考虑以下代码。

```
    // Single threaded version
    class Foo {
        private Helper helper = null;
        public Helper getHelper() {
            if (helper == null) 
                helper = new Helper();
            return helper;
        }
        // other functions and members...
    }
```

If this code was used in a multithreaded context, many things could go wrong. Most obviously, two or more Helper objects could be allocated. (We'll bring up other problems later). The fix to this is simply to synchronize the getHelper() method:

如果在多线程环境下使用这段代码，很多事情都会出错。最明显的是，两个或更多的 Helper 对象可能被分配。(我们将在后面提出其他问题）。解决这个问题的方法很简单，就是同步 getHelper() 方法。

```
    // Correct multithreaded version
    class Foo {
        private Helper helper = null;
        public synchronized Helper getHelper() {
            if (helper == null)
                helper = new Helper();
            return helper;
        }
    }
    
```
The code above performs synchronization every time getHelper() is called. The double-checked locking idiom tries to avoid synchronization after the helper is allocated:

上面的代码在每次调用 getHelper()时都会执行同步化。双重检查锁的习惯做法是试图避免在 helper 被分配后进行同步。

```
    // Broken multithreaded version
    // "Double-Checked Locking" idiom
    class Foo {
        private Helper helper = null;
        public Helper getHelper() {
            if (helper == null)
                synchronized(this) {
                    if (helper == null)
                        helper = new Helper();
                }
            return helper;
        }
    }
```
Unfortunately, that code just does not work in the presence of either optimizing compilers or shared memory multiprocessors.

不幸的是，在优化编译器或共享内存多处理器的情况下，这种代码就是不工作。

### It doesn't work
There are lots of reasons it doesn't work. The first couple of reasons we'll describe are more obvious. After understanding those, you may be tempted to try to devise a way to "fix" the double-checked locking idiom. Your fixes will not work: there are more subtle reasons why your fix won't work. Understand those reasons, come up with a better fix, and it still won't work, because there are even more subtle reasons.

有很多原因使它不起作用。我们将描述的前几个原因是比较明显的。在了解了这些之后，你可能会试图设计出一种方法来 "修复 "双重检查锁定的成例。你的修复方法不会奏效：你的修复方法不会奏效，还有更微妙的原因。理解了这些原因，想出了一个更好的修复方法，它仍然不会起作用，因为还有更微妙的原因。

Lots of very smart people have spent lots of time looking at this. There is no way to make it work without requiring each thread that accesses the helper object to perform synchronization.

很多非常聪明的人已经花了很多时间来研究这个问题。如果不要求每个访问辅助对象的线程都执行同步，就没有办法使其工作。

#### The first reason it doesn't work
The most obvious reason it doesn't work it that the writes that initialize the Helper object and the write to the helper field can be done or perceived out of order. Thus, a thread which invokes getHelper() could see a non-null reference to a helper object, but see the default values for fields of the helper object, rather than the values set in the constructor.

最明显的原因是，初始化 Helper 对象的写入和对 helper 字段的写入可以不按顺序进行或被感知。因此，调用 getHelper() 的线程可能会看到一个对帮助者对象的非空引用，但看到的是帮助者对象字段的默认值，而不是构造函数中设置的值。

If the compiler inlines the call to the constructor, then the writes that initialize the object and the write to the helper field can be freely reordered if the compiler can prove that the constructor cannot throw an exception or perform synchronization.

如果编译器内联了对构造函数的调用，那么如果编译器能够证明构造函数不能抛出异常或执行同步，那么初始化对象的写入和对辅助字段的写入可以自由地重新排序。

Even if the compiler does not reorder those writes, on a multiprocessor the processor or the memory system may reorder those writes, as perceived by a thread running on another processor.

即使编译器不对这些写入进行重新排序，在多处理器上，处理器或内存系统也可能对这些写入进行重新排序，这一点被运行在另一个处理器上的线程所感知。

Doug Lea has written a more detailed description of compiler-based reorderings.

##### A fix that doesn't work
Given the explanation above, a number of people have suggested the following code:

```
    // (Still) Broken multithreaded version
    // "Double-Checked Locking" idiom
    class Foo { 
        private Helper helper = null;
        public Helper getHelper() {
            if (helper == null) {
                Helper h;
                synchronized(this) {
                    h = helper;
                    if (h == null) 
                        synchronized (this) {
                            h = new Helper();
                        } // release inner synchronization lock
                    helper = h;
                } 
            }    
            return helper;
        }
        // other functions and members...
    }
```

This code puts construction of the Helper object inside an inner synchronized block. The intuitive idea here is that there should be a memory barrier at the point where synchronization is released, and that should prevent the reordering of the initialization of the Helper object and the assignment to the field helper.

这段代码将帮助者对象的构建放在一个内部同步块中。这里的直观想法是，在释放同步的地方应该有一个内存屏障，这应该能防止对 Helper 对象的初始化和对字段帮助器的赋值进行重新排序。

Unfortunately, that intuition is absolutely wrong. The rules for synchronization don't work that way. The rule for a monitorexit (i.e., releasing synchronization) is that actions before the monitorexit must be performed before the monitor is released. However, there is no rule which says that actions after the monitorexit may not be done before the monitor is released. It is perfectly reasonable and legal for the compiler to move the assignment helper = h; inside the synchronized block, in which case we are back where we were previously. Many processors offer instructions that perform this kind of one-way memory barrier. Changing the semantics to require releasing a lock to be a full memory barrier would have performance penalties.

不幸的是，这种直觉是绝对错误的。同步的规则并不是这样的。monitorexit（即释放同步）的规则是，monitorexit 之前的动作必须在监视器被释放之前执行。然而，并没有任何规则规定 monitorexit 之后的动作不能在监视器释放之前进行。编译器将赋值 helper = h;移到同步块内是完全合理和合法的，在这种情况下，我们又回到了之前的位置。许多处理器提供了执行这种单向内存屏障的指令。如果改变语义，要求释放锁成为一个完整的内存屏障，会对性能产生影响。

##### More fixes that don't work
There is something you can do to force the writer to perform a full bidirectional memory barrier. This is gross, inefficient, and is almost guaranteed not to work once the Java Memory Model is revised. Do not use this. In the interests of science, I've put a description of this technique on a separate page. Do not use it.

你可以做一些事情来强迫写入者执行一个完整的双向内存屏障。这是粗暴的，低效的，而且一旦 Java 内存模型被修改，几乎可以保证不工作。请不要使用这个方法。为了科学起见，我把这个技术的描述放在另一个页面上。不要使用它。

However, even with a full memory barrier being performed by the thread that initializes the helper object, it still doesn't work.

然而，即使由初始化辅助对象的线程执行全内存屏障，它仍然无法工作。

The problem is that on some systems, the thread which sees a non-null value for the helper field also needs to perform memory barriers.

问题是，在一些系统上，看到 helper 字段为非空值的线程也需要执行内存屏障。

Why? Because processors have their own locally cached copies of memory. On some processors, unless the processor performs a cache coherence instruction (e.g., a memory barrier), reads can be performed out of stale locally cached copies, even if other processors used memory barriers to force their writes into global memory.

为什么？因为处理器有自己的本地缓存的内存副本。在一些处理器上，除非处理器执行缓存一致性指令（例如，内存屏障），否则可以从陈旧的本地缓存副本中进行读取，即使其他处理器使用内存屏障来强制将其写入全局内存。

##### Reordering on an Alpha processor
A very non-intuitive property of the Alpha processor is that it allows the following behavior:

阿尔法处理器的一个非常非直观的特性是，它允许以下行为。

```
    // Initially:
    p = &x, x = 1, y = 0;

    // Thread 1
    y = 1;
    memoryBarrier
    p = &y

    // Thread 2
    i = *p

    // Can result in: i = 0
```
This behavior means that the reader needs to perform a memory barrier in lazy initialization idioms (e.g., Double-checked locking) and creates issues for synchronization-free immutable objects (e.g., ensuring. that other threads see the correct value for fields of a String object).

这种行为意味着读者需要在懒惰的初始化习惯中执行内存屏障（例如，双重检查锁定），并为无同步的不可变对象创造问题（例如，确保.其他线程看到一个字符串对象的字段的正确值）。

Kourosh Gharachorloo wrote a note explaining how it can actually happen on an Alpha multiprocessor:

Kourosh Gharachorloo写了一份说明，解释了它如何在Alpha多处理器上实际发生。

The anomalous behavior is currently only possible on a 21264-based system. And obviously you have to be using one of our multiprocessor servers. Finally, the chances that you actually see it are very low, yet it is possible.

异常行为目前只可能在基于21264的系统上出现。而且显然你必须使用我们的多处理器服务器之一。最后，你真正看到它的几率非常低，但这是可能的。

Here is what has to happen for this behavior to show up. Assume T1 runs on P1 and T2 on P2. P2 has to be caching location y with value 0. P1 does y=1 which causes an "invalidate y" to be sent to P2. This invalidate goes into the incoming "probe queue" of P2; as you will see, the problem arises because this invalidate could theoretically sit in the probe queue without doing an MB on P2. The invalidate is acknowledged right away at this point (i.e., you don't wait for it to actually invalidate the copy in P2's cache before sending the acknowledgment). Therefore, P1 can go through its MB. And it proceeds to do the write to p. Now P2 proceeds to read p. The reply for read p is allowed to bypass the probe queue on P2 on its incoming path (this allows replies/data to get back to the 21264 quickly without needing to wait for previous incoming probes to be serviced). Now, P2 can derefence P to read the old value of y that is sitting in its cache (the inval y in P2's probe queue is still sitting there).

以下是必须发生的情况，以使这种行为显示出来。假设 T1 在 P1 上运行，T2 在 P2 上运行。P2 必须以 0 的值缓存位置 y。P1 做了 y = 1，导致一个 "invalidate y"被发送到 P2。这个无效信息进入了 P2的 "探测队列"；正如你将看到的，问题出现了，因为这个无效信息理论上可以放在探测队列中，而不在 P2 上做一个 MB。在这一点上，无效化被立即确认（也就是说，在发送确认之前，你不需要等待它在 P2 的高速缓存中实际无效化副本）。因此，P1 可以通过它的 MB。现在 P2 开始读 p。读 p 的回复被允许在其传入路径上绕过 P2 的探测队列（这允许回复/数据快速回到 21264，而不需要等待先前传入的探测被服务）。现在，P2 可以取消对 P 的否定，以读取其缓存中的 y 的旧值（P2 的探测队列中的 inval y 仍然在那里）。

How does an MB on P2 fix this? The 21264 flushes its incoming probe queue (i.e., services any pending messages in there) at every MB. Hence, after the read of P, you do an MB which pulls in the inval to y for sure. And you can no longer see the old cached value for y.

P2 上的 MB 是如何解决这个问题的？ 21264 在每次 MB 时都会刷新其传入的探针队列（即服务于其中的任何未决消息）。因此，在读完 P 后，你做了一个 MB，肯定会拉到 y 的调用。你就不能再看到 y 的旧缓存值了。

Even though the above scenario is theoretically possible, the chances of observing a problem due to it are extremely minute. The reason is that even if you setup the caching properly, P2 will likely have ample opportunity to service the messages (i.e., inval) in its probe queue before it receives the data reply for "read p". Nonetheless, if you get into a situation where you have placed many things in P2's probe queue ahead of the inval to y, then it is possible that the reply to p comes back and bypasses this inval. It would be difficult for you to set up the scenario though and actually observe the anomaly.

即使上述情况在理论上是可能的，但由于它而观察到问题的机会是极小的。原因是，即使你正确地设置了缓存，P2 在收到 "读 p"的数据回复之前，很可能有足够的机会为其探测队列中的消息（即 inval）提供服务。尽管如此，如果你在 P2 的探针队列中放置了许多东西，比对 y 的 inval 要早，那么对p的回复就有可能回来并绕过这个 inval。但你很难设置这种情况并实际观察到这种异常现象。

The above addresses how current Alpha's may violate what you have shown. Future Alpha's can violate it due to other optimizations. One interesting optimization is value prediction.

上述内容涉及目前的 Alpha 可能违反你所展示的内容。未来的 Alpha 可能会因为其他优化而违反它。一个有趣的优化是价值预测。

#### Is it worth the trouble? (这是否值得麻烦呢？)
For most applications, the cost of simply making the getHelper() method synchronized is not high. You should only consider this kind of detailed optimizations if you know that it is causing a substantial overhead for an application.

对于大多数应用程序来说，简单地使 getHelper() 方法同步化的成本并不高。只有当你知道这种详细的优化会给应用程序带来大量的开销时，你才应该考虑这种优化。

Very often, more high level cleverness, such as using the builtin mergesort rather than handling exchange sort (see the SPECJVM DB benchmark) will have much more impact.

很多时候，更高层次的聪明才智，比如使用内置的合并排序而不是处理交换排序（见SPECJVM DB基准）会产生更大的影响。

#### Making it work for static singletons
If the singleton you are creating is static (i.e., there will only be one Helper created), as opposed to a property of another object (e.g., there will be one Helper for each Foo object, there is a simple and elegant solution.

如果你要创建的单例是静态的（即只会创建一个 Helper），而不是另一个对象的属性（例如，每个 Foo 对象会有一个 Helper），有一个简单而优雅的解决方案。

Just define the singleton as a static field in a separate class. The semantics of Java guarantee that the field will not be initialized until the field is referenced, and that any thread which accesses the field will see all of the writes resulting from initializing that field.

只要在一个单独的类中把单例定义为一个静态字段。Java 的语义保证该字段在被引用之前不会被初始化，而且任何访问该字段的线程都会看到初始化该字段所产生的所有写入。

```
    class HelperSingleton {
        static Helper singleton = new Helper();
    }
```

#### It will work for 32-bit primitive values
Although the double-checked locking idiom cannot be used for references to objects, it can work for 32-bit primitive values (e.g., int's or float's). Note that it does not work for long's or double's, since unsynchronized reads/writes of 64-bit primitives are not guaranteed to be atomic.

尽管双重检查锁的习惯做法不能用于对对象的引用，但它可以用于 32 位基元值（例如 int 或 float）。注意，它不适用于 long 或 double，因为 64 位基元的非同步读/写不能保证是原子的。

```
    // Correct Double-Checked Locking for 32-bit primitives
    class Foo { 
        private int cachedHashCode = 0;
        public int hashCode() {
        int h = cachedHashCode;
        if (h == 0) 
            synchronized(this) {
                if (cachedHashCode != 0) return cachedHashCode;
                h = computeHashCode();
                cachedHashCode = h;
            }
            return h;
        }
        // other functions and members...
    }
```

In fact, assuming that the computeHashCode function always returned the same result and had no side effects (i.e., idempotent), you could even get rid of all of the synchronization.

事实上，假设 computeHashCode 函数总是返回相同的结果，并且没有副作用（即，同位素），你甚至可以摆脱所有的同步。

```
    // Lazy initialization 32-bit primitives
    // Thread-safe if computeHashCode is idempotent
    class Foo { 
        private int cachedHashCode = 0;
        public int hashCode() {
            int h = cachedHashCode;
            if (h == 0) {
                h = computeHashCode();
                cachedHashCode = h;
            }
            return h;
        }
        // other functions and members...
    }
```

### Making it work with explicit memory barriers
It is possible to make the double checked locking pattern work if you have explicit memory barrier instructions. For example, if you are programming in C++, you can use the code from Doug Schmidt et al.'s book:

如果你有明确的内存屏障指令，就有可能使双重检查的锁定模式发挥作用。例如，如果你是用 C++ 编程，你可以使用 Doug Schmidt 等人书中的代码。

```
    // C++ implementation with explicit memory barriers
    // Should work on any platform, including DBC Alphas
    // From "Patterns for Concurrent and Distributed Objects",
    // by Doug Schmidt
    template <class TYPE, class LOCK> 
    TYPE * Singleton<TYPE, LOCK>::instance(void) {
        // First check
        TYPE* tmp = instance_;
        // Insert the CPU_specific memory barrier instruction
        // to synchronize ths cache lines on multi-processor.
        asm ("memoryBarrier");
        if (tmp == 0) {
            // Ensure serialization (guard constructor acquires lock_).
            Guard<LOCK> guard (lock_);
            // Double check.
            tmp = instance_;
            if (tmp == 0) {
                tmp = new TYPE;
                // Insert the CPU-specific memory barrier instruction to 
                // synchronize the cache lines on multi-processor.
                asm ("memoryBarrier");
                instance_ = tmp;
            }
        }
        return tmp;
    }
```

#### Fixing Double-Checked Locking using Thread Local Storage
Alexander Terekhov (TEREKHOV@de.ibm.com) came up clever suggestion for implementing double checked locking using thread local storage. Each thread keeps a thread local flag to determine whether that thread has done the required synchronization.

Alexander Terekhov (TEREKHOV@de.ibm.com)提出了一个巧妙的建议，即使用线程本地存储来实现双检查锁。每个线程都保留一个线程本地标志，以确定该线程是否已经完成了所需的同步。

```
    class Foo {
        // If perThreadInstance.get() returns a non-null value, this thread has done synchronization needed to see initialization of helper
        private final ThreadLocal perThreadInstance = new ThreadLocal();
        private Helper helper = null;
        public Helper getHelper() {
            if (perThreadInstance.get() == null) createHelper();
            return helper;
        }

        private final void createHelper() {
            synchronized (this) {
                if (helper == null)
                    helper = new Helper();
            }
            // Any non-null value would do as the argument here
            perThreadInstance.set(perThreadInstance);
        }
    }
```

The performance of this technique depends quite a bit on which JDK implementation you have. In Sun's 1.2 implementation, ThreadLocal's were very slow. They are significantly faster in 1.3, and are expected to be faster still in 1.4. Doug Lea analyzed the performance of some techniques for implementing lazy initialization.

这种技术的性能在很大程度上取决于你的JDK实现。在Sun的1.2实现中，ThreadLocal的速度非常慢。在1.3中，它们的速度明显加快，预计在1.4中还会更快。

### Under the new Java Memory Model

#### Fixing Double-Checked Locking using Volatile
JDK5 and later extends the semantics for volatile so that the system will not allow a write of a volatile to be reordered with respect to any previous read or write, and a read of a volatile cannot be reordered with respect to any following read or write. See this entry in Jeremy Manson's blog for more details.

JDK5 和更高版本扩展了 volatile 的语义，这样系统就不允许对 volatile 的写与之前的任何读或写进行重新排序，对 volatile 的读也不能与之后的任何读或写进行重新排序。

With this change, the Double-Checked Locking idiom can be made to work by declaring the helper field to be volatile. This does not work under JDK4 and earlier.

有了这一变化，通过将 helper 字段声明为 volatile，就可以使双重检查锁定的成例发挥作用。这在JDK4和更早的版本中不起作用。

```
    // Works with acquire/release semantics for volatile
    // Broken under current semantics for volatile

    class Foo {
        private volatile Helper helper = null;
        public Helper getHelper() {
            if (helper == null) {
                synchronized (this) {
                    if (helper == null)
                        helper = new Helper();
                }
            }
            return helper;
        }
    }
```

#### Double-Checked Locking Immutable Objects
If Helper is an immutable object, such that all of the fields of Helper are final, then double-checked locking will work without having to use volatile fields. The idea is that a reference to an immutable object (such as a String or an Integer) should behave in much the same way as an int or float; reading and writing references to immutable objects are atomic.

如果 Helper 是一个不可变的对象，比如 Helper 的所有字段都是 final 的，那么双重检查锁就可以工作，而不需要使用 volatile 字段。我们的想法是，对不可变对象（如 String 或 Integer）的引用应该与 int 或 float 的行为方式基本相同；对不可变对象的读写引用是原子的。