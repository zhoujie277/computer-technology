package com.future.concurrent.javaold;

import io.netty.channel.unix.Errors;
import sun.security.provider.SHA;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 */
class Java5ReentrantReadWriteLock {
    static final int SHARED_SHIFT = 16;
    static final int SHARED_UNIT = (1 << SHARED_SHIFT);
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

    /**
     * Returns the number of shared holds represented in count
     */
    static int sharedCount(int c) {
        return c >>> SHARED_SHIFT;
    }

    /**
     * Returns the number of exclusive holds represented in count
     */
    static int exclusiveCount(int c) {
        return c & EXCLUSIVE_MASK;
    }

    private final Sync sync;
    private final ReadLock readerLock;
    private final WriteLock writerLock;

    public Java5ReentrantReadWriteLock() {
        sync = new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public Java5ReentrantReadWriteLock(boolean fair) {
        sync = (fair) ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public WriteLock writeLock() {
        return writerLock;
    }

    public ReadLock readLock() {
        return readerLock;
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {
        Thread owner;

        abstract void wlock();

        final boolean nonfairTryAcquire(int acquires) {
            acquires = exclusiveCount(acquires);
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (w + acquires >= SHARED_UNIT)
                throw new Error("Maximum lock count exceeded");
            if (c != 0 && (w == 0 || current != owner)) {
                return false;
            }
            if (!compareAndSetState(c, c + acquires))
                return false;
            owner = current;
            return true;
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (; ; ) {
                int c = getState();
                int nextc = c + (acquires << SHARED_SHIFT);
                if (nextc < c)
                    throw new Error("Maximum lock count exceeded");
                if (exclusiveCount(c) != 0 && owner != Thread.currentThread())
                    return -1;
                if (compareAndSetState(c, nextc))
                    return 1;
            }
        }

        protected final boolean tryRelease(int releases) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (owner != current)
                throw new IllegalMonitorStateException();
            int nextc = c - releases;
            boolean free = false;
            if (exclusiveCount(c) == releases) {
                free = true;
                owner = null;
            }
            setState(nextc);
            return free;
        }

        protected final boolean tryReleaseShared(int releases) {
            for (; ; ) {
                int c = getState();
                int nextc = c - (releases << SHARED_SHIFT);
                if (nextc < 0)
                    throw new IllegalMonitorStateException();
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }

        @Override
        protected boolean isHeldExclusively() {
            return exclusiveCount(getState()) != 0 && owner == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            int c = exclusiveCount(getState());
            Thread o = owner;
            return (c == 0) ? null : o;
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            int c = exclusiveCount(getState());
            Thread o = owner;
            return (o == Thread.currentThread()) ? c : 0;
        }

        final int getCount() {
            return getState();
        }
    }

    final static class NonfairSync extends Sync {
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }

        @Override
        void wlock() {
            if (compareAndSetState(0, 1))
                owner = Thread.currentThread();
            else
                acquire(1);
        }
    }

    final static class FairSync extends Sync {
        @Override
        protected final boolean tryAcquire(int acquires) {
            acquires = exclusiveCount(acquires);
            Thread current = Thread.currentThread();
            Thread first;
            int c = getState();
            int w = exclusiveCount(c);
            if (w + acquires >= SHARED_UNIT)
                throw new Error("Maximum lock count exceeded");
            if ((w == 0 || current != owner) &&
                    (c != 0 || ((first = getFirstQueuedThread()) != null && first != current)))
                return false;
            if (!compareAndSetState(c, c + acquires))
                return false;
            owner = current;
            return true;
        }

        @Override
        protected final int tryAcquireShared(int acquires) {
            Thread current = Thread.currentThread();
            for (; ; ) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (owner != current)
                        return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + (acquires << SHARED_SHIFT);
                if (nextc < c)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        @Override
        void wlock() {
            acquire(1);
        }
    }

    public static class ReadLock {
        private final Sync sync;

        protected ReadLock(Java5ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {
            sync.acquireShared(1);
        }

        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        public boolean tryLock() {
            return sync.nonfairTryAcquireShared(1) >= 0;
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        public void unlock() {
            sync.releaseShared(1);
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

    }

    public static class WriteLock {
        private final Sync sync;

        protected WriteLock(Java5ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {
            sync.wlock();
        }

        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        public boolean tryLock() {
            return sync.nonfairTryAcquire(1);
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        public void unlock() {
            sync.release(1);
        }

        public Condition newCondition() {
            return sync.newCondition();
        }
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    protected Thread getOwner() {
        return sync.getOwner();
    }

    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
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
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
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
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }
}



























