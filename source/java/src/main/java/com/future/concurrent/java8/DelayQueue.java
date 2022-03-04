package com.future.concurrent.java8;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一种延迟元素的无界阻塞队列，其中一个元素只有在其延迟过期时才能被取走。
 * 队列的头是延迟过期时间最长的延迟元素。
 * 如果没有延迟过期，则没有 head，poll 将返回 null。
 * 当元素的getDelay（TimeUnit.NANOSECONDS）方法返回小于或等于零的值时，将发生过期。
 * 即使无法使用 take 或 poll 删除未过期的元素，它们也会被视为正常元素。
 * 例如，size 方法返回过期和未过期元素的计数。
 * 此队列不允许空元素。
 */
@SuppressWarnings("unused")
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final transient ReentrantLock lock = new ReentrantLock();
    private final PriorityQueue<E> q = new PriorityQueue<>();

    /**
     * 指定在队列头部等待元素的线程。
     * 这种 leader/follower 模式的变体用于最小化不必要的定时等待。
     * 当一个线程成为 leader 时，它只等待下一个延迟过去，而其他线程则无限期地等待。
     * 在从 take（）或poll（..）返回之前，leader 线程必须向其他线程发送信号，
     * 除非其他线程在此期间已经成为 leader。
     * 每当队列的头被一个过期时间较早的元素替换时，leader字段就会被重置为 null 而失效，
     * 并且一些等待的线程（不一定是当前的 leader）会收到信号。
     * 因此，等待线程必须做好准备，在等待过程中获得并失去领导权。
     */
    private Thread leader = null;

    private final Condition available = lock.newCondition();

    public DelayQueue() {
    }

    public DelayQueue(Collection<? extends E> collection) {
        this.addAll(collection);
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    @Override
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offer(e);
    }

    /**
     * 该方法为笔者添加，主要是为了区别 leader/follower 工作者线程在这运用的好处。
     * 如该方法所示，当队列最小延迟还没到时，多个线程都会被 awaitNanos(delay)。
     * 在时间到了，all threads 都会被唤醒，然后竞争一个共享队列的头部元素，谁先到谁先获得。
     * 其他线程又要进入 await。导致有比较多的上下文切换开销。
     * 此处运用 leader/follower 模式的好处在于，只有一个线程 awaitNanos。
     * 超时后，leader 线程被唤醒，尝试读取共享队列，同时 leader置为空。
     * 当获取到队首元素，退出队列后，又接着唤醒等待中的线程。
     * 使其最多一个成为新的 leader 线程。
     */
    private E normalTake() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                if (first == null) {
                    available.await();
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) return q.poll();
                    first = null;
                    available.awaitNanos(delay);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检索并移除此队列的头，如有必要，等待此队列上具有过期延迟的元素可用。
     */
    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                if (first == null)
                    available.await();
                else {
                    // processor
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0)
                        return q.poll();
                    first = null; // don't retain ref while waiting
                    if (leader != null)
                        // follower
                        available.await();
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            // leader
                            available.awaitNanos(delay);
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }

    /**
     * 检索并删除此队列的头，如有必要，等待此队列上具有过期延迟的元素可用，或指定的等待时间过期。
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0)
                        return q.poll();
                    if (nanos <= 0)
                        return null;
                    first = null; //don't retain ref while waiting
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E e; (e = peekExpired()) != null; ) {
                c.add(e);
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E e; n < maxElements && (e = peekExpired()) != null; ) {
                c.add(e);
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            if (q.peek() == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                return null;
            else
                return q.poll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检索但不删除此队列的头，如果此队列为空，则返回null。
     * 与poll不同，如果队列中没有过期的元素，则此方法返回下一个将过期的元素（如果存在）。
     */
    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }

    private E peekExpired() {
        E first = q.peek();
        return (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) ? null : first;
    }

    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
                if (o == it.next()) {
                    it.remove();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private class Itr implements Iterator<E> {
        final Object[] array;
        int cursor;
        int lastRet;

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        @Override
        public E next() {
            if (cursor > array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E) array[cursor++];
        }

        @Override
        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }
}

