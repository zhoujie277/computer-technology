# Bidirectional memory barriers
under existing Java semantics

<h3 style="color: red">DO NOT USE</h3>

The technique described here is inefficient and will not work once the Java memory model is revised. It is described here as a historical artifact.

这里描述的技术是低效的，一旦 Java 内存模型被修改，它就不会再起作用。在这里，它被描述为一个历史文物。

The rules for a synchronized block are that none of the actions inside the synchronized block can be performed before the lock is obtained or after it is released. However, actions before the synchronized block do not have to be completed before the lock is obtained. Similarly, actions after a synchronized block can be performed before the lock is released.

同步块的规则是，同步块内的任何操作都不能在获得锁之前或释放锁之后进行。然而，在同步块之前的操作不一定要在获得锁之前完成。同样地，在同步块之后的动作也可以在锁被释放之前执行。

In essence, a synchronized block is like a roach motel: statements can move in, but they can't move out.

从本质上讲，同步区块就像一个蟑螂旅馆：语句可以搬进去，但不能搬出来。

```
    // Initially
    x = y = 0; 
    a = new Object(); 
    b = new Object();

    // Thread 1
    synchronized (a) {
        x = 1;
    }
    y = 1;

    // Thread 2
    synchronized (b) {
        j = y;
    }
    i = x;

    // Can result in i = 0 and j = 1;
```

A compiler could legally transform the above code into:

一个编译器可以合法地将上述代码转化为。

```
    // Initially
    x = y = 0;
    a = new Object();
    b = new Object();

    // Thread 1
    synchronized (a) {
        y = 1;
        x = 1;
    }

    // Thread 2
    synchronized (b) {
        i = x;
        j = y;
    }

    // Can result in i = 0 and j = 1
```

However, if each thread contains two synchronized blocks, the under the current semantics, but not under the proposed semantics, all actions in the first block must be performed before any actions in the second block:

然而，如果每个线程包含两个同步块，在当前的语义下，而不是在提议的语义下，第一个块中的所有动作必须在第二个块中的任何动作之前执行。

<h5 style="color: red">DO NOT USE</h5>

```
    // Initially
    x = y = 0;
    a = new Object();
    b = new Object();

    // Thread 1
    synchronized (a) {
        x = 1;
    }
    synchronized (a) {
        y = 1;
    }

    // Thread 2
    synchronized (b) {
        j = y;
    }
    synchronized (b) {
        i = x;
    }

    // Must not result in in i = 0 and j = 1
```

The proposed semantics for eliminating ''useless'' synchronization would allow the compiler to transform the above program to the following, eliminating the bidirectional memory barrier. In addition, under the proposed semantics, since the threads are locking separate objects, the locks have no effect on the visibility of the memory actions to the other thread.

为消除 "无用的 "同步而提出的语义将允许编译器将上述程序转变为以下程序，从而消除双向内存屏障。此外，在建议的语义下，由于线程是在锁定不同的对象，所以锁对其他线程的内存操作的可见性没有影响。

```
    // Initially
    x = y = 0;
    a = new Object();
    b = new Object();

    // Thread 1
    synchronized (a) {
        y = 1;
        x = 1;
    }

    // Thread 2
    synchronized (b) {
        i = x;
        j = y;
    }

    // Can result in in i = 0 and j = 1
```

## Bidirectional memory barriers can't be used to fix double-checked locking
<h4 style="color: red">DO NOT USE</h4>

The above discussion suggests the following technique might make double-checked locking work:

上面的讨论表明，下面的技术可能会使双重检查的锁定发挥作用。

```
    // (Still) Broken multithreaded version
    // "Double-Checked Locking" idiom
    // DO NOT USE
    class Foo {
        private Helper helper = null;
        public Helper getHelper() {
            if (helper == null) {
                Helper h;
                synchronized (this) {
                    h = helper;
                    if (h == null)
                        h = new Helper();
                }
                // force bidirectional memory barrier
                // will not work under proposed semantics
                synchronized (this) {
                    helper = h;
                }
            }
            return helper;
        }
        // other funcitons and members...
    }
    // DO NOT USE
```

Even under the current semantics, this does not work. The reason is subtle, but has to do with the fact that a thread that sees a non-null value for the helper field might not see the correct values for the fields of the helper object.

即使在目前的语义下，这也是不可行的。原因很微妙，但与这样一个事实有关：一个线程如果看到 helper 字段的非空值，就可能看不到 helper 对象的字段的正确值。

This is explained in more detail in the Double-Checked Locking declaration. As a another point to look at is the C++ version with explicit memory barriers; The C++ code has bidirectional memory barriers in two locations. To make the Java version work, you would need to use the technique described on this page to implement both memory barriers. Since that would mean that each invocation of getHelper enters at least two synchronized regions, it would be much slower than just synchronizing the getHelper method.

这在双重检查锁定声明中会有更详细的解释。作为另一个看点是 C++ 版本的显式内存屏障；C++ 代码在两个位置有双向的内存屏障。为了使 Java 版本工作，你需要使用本页描述的技术来实现两个内存屏障。因为这意味着每次调用 getHelper 都要进入至少两个同步区域，这将比仅仅同步 getHelper 方法慢得多。