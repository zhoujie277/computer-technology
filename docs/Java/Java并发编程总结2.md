# Java并发编程总结 2

>统观并发算法的历史和 Java 并发库的版本演进，几乎所有的并发算法都是在解决一个问题：减少总线争用。术语中描述的“总线”又可细分为软件级别的总线（或称语言级别的总线）和硬件级别的总线。软件级别的总线指的是在 Java 中如 Synchronized 关键字或者 Reentrantlock。而硬件级别的总线在 Java 中的体现是 Volatile 变量的读写和 CAS 操作。

> 减少总线争用在软件级别的体现比如 Java 中 LinkeBlockingQueue 的双锁，ReadWriteReentrantLock 以及 CopyOnWriteList 的实现等。而其在硬件级别的体现比如 TAS/TTAS/BACKOFF/CLH/MCS 的算法的演进，从频繁争用到退避争用到本地询问等等。都体现了减少总线争用的思想，从而提高整个数据容器的吞吐量。

[TOC]

## 1. AbstractQueuedSynchronizer
所谓同步器是指对线程进行某个临界区的访问建立一整套规则。以实现一个线程或者多个线程对该临界区的同步与互斥。临界区通常是包含共享资源的代码区。通常，只允许一个线程访问临界区，叫独占访问；而允许多个线程同时访问临界区，则称为共享访问。

AbstractQueuedSynchronizer 的设计总共从独占和共享、MESA 管程、支持可取消、公平性等几个方面来展开实现的。

### 1.1 独占和共享

#### 1.1.1 状态依赖的管理
AQS 的任何子类都会需要进行状态的管理，它通过管理一个整数状态信息，可以通过 getState()、setState() 以及 compareAndSetState() 等 protected 类型方法来进行操作。这个整数可以用于表示任意状态。例如，ReentrantLock 用它来表示所有者线程已经重复获取该锁的次数，Semaphore 用它来表示剩余的许可数量，FutureTask 用它来表示任务的状态（尚未开始，正在运行，已完成以及已取消）。在同步器类中还可以自行管理一些额外的状态变量。例如，ReentrantLock 保存了锁的当前所有者的信息，这样做就能区分某个获取操作时重入的还是竞争的。

#### 1.1.2 独占锁的实现
```
    // 获取锁
    if (!tryAcquire(arg)) {
        doAcquire(arg);
    }

    // 实现简单的独占
    boolean tryAcquire(int arg) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }

    // 释放锁
    boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    // 简单独占锁的释放
    boolean tryRelease(int arg) {
        if (getState() == 0)
            throw new IllegalMonitorStateException();
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }
```

#### 1.1.3 共享锁的实现
```
    // 获取锁
    void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    // 简单共享锁的获取
    int tryAcquireShared(int arg) {
        return getState() != 0 ? 1 : -1;
    }

    // 释放锁
    boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // 简单共享锁的释放
    boolean tryReleaseShared(int arg) {
        setState(1);
        return true;
    }
```

### 1.2 MESA 管程
AQS 提供了实现管程的基本原语。管程的定义是一个共享资源的数据结构以及一组能为并发进程在其上执行的针对该资源的一组操作，这组操作能同步进程和改变管程中的数据。管程的发展史中，先后出现过三种管程模型，Hasen 模型、Hoare 模型和 MESA 模型，Java 使用的是 MESA 模型。管程的特征如下。
+ 局限于管程的共享变量（数据结构）只能被管程的过程访问，任何外部过程都不能访问。
+ 一个线程通过调用管程的一个过程进入管程。
+ 任何时候只能有一个进程在管程中执行，调用管程的任何其他进程都被挂起，以等待管程变为可用，即管程有效地实现互斥。

在 MESA 管程中，当一个线程通过某个过程进入管程时，便获得了独占的访问权。与此同时，如果有其它线程也试图进入该管程区，会被阻塞在一个被称作同步队列的队列上。另外，如果在管程区内的线程由于某个条件不满足，需要挂起的时候，该线程会被挂起在管程中一个被称作条件队列的队列上。如果由于不同条件挂起的线程，则会被挂起在不同条件的条件队列上。挂起之后，释放管程中的锁，以便同步队列中的某个线程能获得锁，以进入管程区。当挂起在条件队列中的线程由于满足条件时，将被移动到同步队列上，以便下次重新获得锁，进入管程区。

#### 1.2.1 互斥的实现
请参见 [1.1.2](#112-独占锁的实现) 小节独占锁的实现

#### 1.2.2 同步队列的实现
+ 入队操作。同步队列是一个 CLH 队列的变体，与传统的 CLH 队列不同的是，它显式地维护了一个队列的头指针和尾指针，将需要等待的线程表示为队列中的一个结点，并在结点中维护等待状态。当新结点插入队列时，先初始化新结点的 prev 指针，而后线性化更新队列中的尾结点，并随后更新之前尾结点的 next 指针。也就是说，任意一个时间点上，都可以从 tail 结点通过 prev 指针向前追溯到队列中所有结点。但 next 指针并不提供这种保证，它只提供了要么指向 null，要么指向 队列中下一个结点的保证。由于在大部分情况下 next 指针都还是准确地描述队列中的节点，所以 next 指针可以用来进行快速访问。即 next 能访问的到结点必定在队列中，next 指针访问不到的结点不一定不在队列中。

```
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    private void doAcquire(int arg) {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

+ 状态说明。如果当前线程需要阻塞等待，那么会将当前线程所在结点的前一个结点状态字段设置为 -1，即下列代码的 Node.SIGNAL。

```
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            return true;
        if (ws > 0) {
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // 在等待之前，将结点状态设置为 -1
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
```

+ 出队操作。当有线程释放锁时，便检查队列头结点，只要不为 0，就表示有线程曾经等待过。然后唤醒下一个需要被唤醒的线程结点，即结点的 waitStatus 小于 0。

```
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }
```

#### 1.2.3 条件队列的实现
+ 条件队列的创建。需要使用条件队列的时候，通过使用 newCondition 获得一个 Condition 对象。每个 Condition 对象维护了一个属于自己的条件队列，通常 Condition 对象字段如下表示。
    ```
        class ConditionObject {
            Node firstWaiter;

            Node lastWaiter;
        }
    ```

+ 将线程添加进条件队列。当应用需要使用在某个条件下需要挂起线程时，调用 await() 方法。将线程封装为一个结点添加至条件队列，并释放锁，然后线程让出 CPU，进入等待状态。

    ```
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }
    ```

+ 将线程唤醒并移到同步队列。并在结点入队成功后，将在它所在结点的前面一个结点的状态字段设置为 -1。等到该结点在同步队列上被调度重新运行时，会从 await 方法唤醒出来。
  ```
    public final void signal() {
        if (!isHeldExclusively())
            throw new IllegalMonitorStateException();
        Node first = firstWaiter;
        if (first != null)
            doSignal(first);
    }

    private void doSignal(Node first) {
        do {
            if ((firstWaiter = first.nextWaiter) == null)
                lastWaiter = null;
            first.nextWaiter = null;
        } while (!transferForSignal(first) && (first = firstWaiter) != null);
    }

    final boolean transferForSignal(Node node) {
        // If cannot change waitStatus, the node has been cancelled.
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }
  ```

### 1.3 支持可取消
不论是在同步队列上，还是在条件队列上，AQS 同步器都提供了两种取消的策略，一个是超时，一个是中断。

#### 1.3.1 同步队列的取消
+ 中断的处理。当在同步队列中的线程遇到阻塞，并且被中断后，会抛出一个 InterruptedException。在退出方法前，会检测是不是要更新当前线程所在结点的状态。

    ```
        private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
            final Node node = addWaiter(Node.EXCLUSIVE);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }
    ```

+ 超时处理。和中断的处理类似。检测到超时之后，退出阻塞，并在退出方法前，检测是否要更新当前线程所在结点的状态。
  ```
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
  ```

+ 取消后，结点状态的处理。注意，这里是将要取消的线程所在的结点的状态设置为 1。然后将被取消的结点所在的前驱结点的 next 指针更新为 被取消结点的后继节点（如果存在）。但这只是一个优化操作，并不保证其他线程任何时候都能看到一个结点的 next 指针永远是未取消的结点。最后，将被取消的结点的 next 指针指向自身，作为 GC 优化。

  ```
    private void cancelAcquire(Node node) {
        if (node == null) return;
        node.thread = null;

        // 过滤掉前面已经被取消了的结点，找到前面没有被取消的结点。
        Node pred = node.prev;
        while (pred.waitStatus > 0) {
            node.prev = pred = pred.prev;
        }

        Node predNext = pred.next;

        // 将结点状态置为取消。
        node.waitStatus = Node.CANCELLED;

        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            int ws;
            if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL
                    || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL)))
                    && pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(predNext, predNext, next);
            } else {
                unparkSuccessor(node);
            }
            node.next = node; // help GC
        }
    }
  ```

注意，取消的时候，是当前线程自己所在结点的状态变为 CANCELLED，而不是前驱结点的 waitStatus，假设在 node.waitStatus = Node.CANCELLED 之后，发生有其他线程争用，需要进行出队操作，并且已经轮到了这个被取消的线程，故在唤醒的时候，需要判断需要被唤醒的线程是否被取消，如果需要被唤醒的线程需要被取消，则需要找到位于队列最前面的未被取消的线程进行唤醒。

```
    private void unparkSuccessor(Node node) {
        // 如果状态为负（即，可能需要信号），则尝试在预期信号的情况下清除。
        // 如果此操作失败或者等待线程更改了状态，则可以。
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        // unpark 的线程保存在后续节点中，通常只是下一个节点。
        // 但如果取消或 next 为空，则从尾部向后遍历以找到实际的未取消的后续项。
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }
```

#### 1.3.2 条件队列的取消
+ 中断的处理。
  ```
    public final void await() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        Node node = addConditionWaiter();
        int savedState = fullyRelease(node);
        int interruptMode = 0;
        while (!isOnSyncQueue(node)) {
            LockSupport.park(this);
            if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                break;
        }
        if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
            interruptMode = REINTERRUPT;
        if (node.nextWaiter != null) // clean up if cancelled
            unlinkCancelledWaiters();
        if (interruptMode != 0)
            reportInterruptAfterWait(interruptMode);
    }

    private int checkInterruptWhileWaiting(Node node) {
        return Thread.interrupted() ?
            (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
    }

    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }
  ```

+ 超时的处理。
  ```
    public final long awaitNanos(long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        Node node = addConditionWaiter();
        int savedState = fullyRelease(node);
        final long deadline = System.nanoTime() + nanosTimeout;
        int interruptMode = 0;
        while (!isOnSyncQueue(node)) {
            if (nanosTimeout <= 0L) {
                transferAfterCancelledWait(node);
                break;
            }
            if (nanosTimeout >= spinForTimeoutThreshold)
                LockSupport.parkNanos(this, nanosTimeout);
            if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                break;
            nanosTimeout = deadline - System.nanoTime();
        }
        if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
            interruptMode = REINTERRUPT;
        if (node.nextWaiter != null)
            unlinkCancelledWaiters();
        if (interruptMode != 0)
            reportInterruptAfterWait(interruptMode);
        return deadline - System.nanoTime();
    }
  ```

从上面可以看到，两者方式的取消处理其实差不多。都是将等待条件的结点移交到同步队列，当从同步队列中返回时（重新获得锁），会从条件队列删除。如果有中断发生，则抛出中断异常或者标记线程被中断过。

### 1.4 公平性
AQS 提供了公平性原理的支持，可以通过 ReentrantLock 看到公平和非公平的版本。所谓公平性是相对于闯入线程和在同步队列中的线程而言的。也就是说，如果同步队列中已经有在等待的线程了，那么新来的线程就不可以闯入获取锁，而是必须要去排队。而不公平则是指，新闯入的线程可以不排队，而是直接尝试获取锁，获取失败了再去排队。

一般而言，在 ReentrantLock 的应用中，非公平性会提高系统的吞吐量，因为唤醒一个被阻塞的线程中间会有一定的时延，如果一个临界区相对较短的话，那么这段时间足以让一个闯入线程获取锁并执行完成了，这样最终就提高了吞吐量。关键代码如下：

```
    // 非公平锁
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        } else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }

    // 公平锁
    static final class FairSync extends Sync {

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        public final boolean hasQueuedPredecessors() {
            // Read fields in reverse initialization order
            Node t = tail;
            Node h = head;
            Node s;
            return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
        }
    }
```

## FutureTask
FutureTask 异步任务的设计主要是从执行任务、等待任务、取消任务、异常处理以及查询任务状态等几个方面来展开实现的，在 Java 中其典型实现有 Java5 版本和 Java8 的版本。Java5 中采用的是 AbstractQueuedSynchronizer 的同步队列来实现等待队列。而 Java8 中采用 TreiberStack 来实现的可取消等待队列。

## ThreadPoolExecutor
