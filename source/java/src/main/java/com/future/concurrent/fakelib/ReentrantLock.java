package com.future.concurrent.fakelib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@SuppressWarnings("unused")
class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;

    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        abstract void lock();

        /**
         * 锁重入的实现。
         */
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
                if (nextc < 0) throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0);
        }

    }

    /**
     * 所谓公平是指所有线程对临界资源申请访问权限的成功率都一样，它不会让某些线程拥有优先权。
     * 我们已经知道了 JDK 的 AQS 的锁是基于 CLH 锁进行优化的，而其中使用了FIFO队列，
     * 也就是说等待队列是一个先进先出的队列。那是否就可以说每条线程获取锁时就是公平的呢？
     * 关于公平性，严格来说应该分成三个点来看：
     * + 入队阶段
     * + 唤醒阶段
     * + 闯入策略。
     * <p>
     * 唤醒阶段。
     * 当线程节点成功加入等待队列后便成为等待队列中的节点，而且这是一个先入先出队列。
     * 那么我们可以得到一个结论：队列中的所有节点是公平的。
     * 因为等待队列中的所有节点都按照顺序等待自己被前驱节点唤醒并获取锁，
     * 所以等待队列中的节点具有公平性。
     * <p>
     * 闯入策略 是 AQS 框架为了提升性能而设计的一个策略。
     * 具体是指一个新线程到达共享资源边界时不管等待队列中是否存在其它等待节点，新线程都将优先尝试去获取锁，这看起来就像是闯入行为。
     * 闯入策略破坏了公平性，AQS 框架对外体现的公平性主要也由此体现。
     * AQS提供的锁获取操作运用了可闯入算法，即如果有新线程到来先进行一次获取尝试，不成功的情况下才将当前线程加入等待队列。
     * <p>
     * 比如，等待队列中节点线程按照顺序一个接一个尝试去获取共享资源的使用权。
     * 而某一时刻头结点线程准备尝试获取的同时另外一条线程闯入，新线程并非直接加入等待队列的尾部，
     * 而是先跟头结点线程竞争获取资源。闯入线程如果成功获取共享资源则直接执行，头结点线程则继续等待下一次尝试。
     * 如此一来闯入线程成功插队，后来的线程比早到的线程先执行，说明 AQS 锁获取算法是不严格公平的。
     * <p>
     * 为什么要使用闯入策略呢？
     * 闯入策略通常可以提升总吞吐量。
     * 由于一般同步器颗粒度比较小，也可以说共享资源的范围较小，而线程从阻塞状态到被唤醒所消耗的时间周期可能是通过共享资源时间周期的几倍甚至几十倍。
     * 如此一来线程唤醒过程中将存在一个很大的时间周期空窗期，导致资源没有得到充分利用，
     * 同时如果每个线程都先入队再唤醒的话也会导致效率低下。为了避免没必要的线程挂起和唤醒，也为了提高吞吐量，
     * 于是引入这种闯入策略。它可以充分利用阻塞唤醒空窗期，也避免了无谓的挂起和唤醒操作，从而大大增加了吞吐率。
     * <p>
     * 闯入机制的实现对外提供一种竞争调节机制，开发者可以在自定义同步器中定义闯入尝试获取的次数。
     * 假设次数为 n 则不断重复获取直到 n 次都获取不成功才把线程加入等待队列中，随着次数 n 的增加可以增大成功闯入的几率。
     * 同时，这种闯入策略可能导致等待队列中的线程饥饿，因为锁可能一直被闯入的线程获取。
     * 但由于一般持有同步器的时间很短暂所以能避免饥饿的发生，反之如果持有锁的时间较长，则将大大增加等待队列无限等待的风险。
     */
    static final class NonfairSync extends Sync {
        @Override
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        @Override
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        @Override
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) throw new Error("Maximum Lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    private final Sync sync;

    public ReentrantLock() {
        sync = new NonfairSync();
    }

    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获得锁。
     * 如果锁未被另一个线程持有，则获取锁并立即返回，将锁持有计数设置为1。
     * 如果当前线程已经持有锁，那么持有计数将增加 1，并且方法立即返回。
     * 如果锁由另一个线程持有，则当前线程出于线程调度目的将被禁用，并处于休眠状态，直到获得锁为止，此时锁持有计数设置为1。
     */
    @Override
    public void lock() {
        sync.lock();
    }

    /**
     * 获取锁，除非当前线程被中断。
     * 如果锁未被另一个线程持有，则获取锁并立即返回，将锁持有计数设置为 1。
     * 如果当前线程已经持有该锁，那么持有计数将增加 1，并且该方法立即返回。
     * 如果锁由另一个线程持有，则出于线程调度目的，当前线程将被禁用，并处于休眠状态，直到发生以下两种情况之一：
     * + 锁被当前线程获取；
     * + 某个线程或其他线程中断了当前线程。
     * <p>
     * 如果当前线程获取了锁，则锁保持计数设置为 1。
     * 如果当前线程：
     * + 在进入该方法时设置其中断状态；
     * + 或在获取锁时被中断，
     * 然后抛出 InterruptedException，并清除当前线程的中断状态。
     * <p>
     * 在该实现中，由于该方法是一个显式中断点，因此相对于锁的正常或可重入获取，优先响应中断。
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程对此锁的保留数。
     * 对于每个锁定操作，线程都有一个锁定保持，而该锁定操作与解锁操作不匹配。
     * 保持计数信息通常仅用于测试和调试目的。
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    protected Thread getOwner() {
        return sync.getOwner();
    }

    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    public boolean hasWaiters(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }
}
