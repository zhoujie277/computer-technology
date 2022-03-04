package com.future.concurrent.java8;

import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 由数组支持的有界阻塞队列。该队列对元素进行FIFO排序（先进先出）。
 * 队列的头是在队列上停留时间最长的元素。队列的尾部是在队列上停留时间最短的元素。
 * 新元素插入到队列的尾部，队列检索操作获取队列头部的元素。
 * <p>
 * 这是一个经典的“有界缓冲区”，其中一个固定大小的数组保存生产者插入的元素和消费者提取的元素。
 * 一旦创建，容量就无法更改。尝试将元素放入完整队列将导致操作阻塞；尝试从空队列中获取元素也会被阻塞。
 * <p>
 * 此类支持一个可选的公平策略，用于排序等待的生产者线程和消费者线程。
 * 默认情况下，不保证此顺序。然而，公平性设置为 true 的队列以 FIFO 顺序授予线程访问权限。
 * 公平性通常会降低吞吐量，但会降低可变性并避免饥饿。
 * <p>
 * 此类及其迭代器实现集合和迭代器接口的所有可选方法。
 */
@SuppressWarnings("unused")
class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -817911632652898426L;


    final Object[] items;

    int takeIndex;

    int putIndex;

    int count;

    /*
     * 并发控制使用经典的双条件算法
     * 在任何教科书中都可以找到。
     */

    /**
     * 保护所有通道的主锁
     */
    final ReentrantLock lock;

    private final Condition notEmpty;
    private final Condition notFull;

    transient Itrs itrs = null;

    /**
     * 周期循环递减 i
     */
    final int dec(int i) {
        return ((i == 0) ? items.length : i) - 1;
    }

    @SuppressWarnings("unchecked")
    final E itemAt(int i) {
        return (E) items[i];
    }

    private static void checkNotNull(Object v) {
        if (v == null) throw new NullPointerException();
    }

    private void enqueue(E x) {
        final Object[] items = this.items;
        items[putIndex] = x;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        notEmpty.signal();
    }

    @SuppressWarnings("unchecked")
    private E dequeue() {
        final Object[] items = this.items;
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null) {
            itrs.elementDequeued();
        }
        notFull.signal();
        return x;
    }

    void removeAt(final int removeIndex) {
        final Object[] items = this.items;
        if (removeIndex == takeIndex) {
            items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
            if (itrs != null)
                itrs.elementDequeued();
        } else {
            final int putIndex = this.putIndex;
            for (int i = removeIndex; ; ) {
                int next = i + 1;
                if (next == items.length)
                    next = 0;
                if (next != putIndex) {
                    items[i] = items[next];
                    i = next;
                } else {
                    items[i] = null;
                    this.putIndex = i;
                    break;
                }
            }
            count--;
            if (itrs != null)
                itrs.removedAt(removeIndex);
        }
        notFull.signal();
    }

    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    /**
     * 创建具有给定（固定）容量、指定访问策略的ArrayBlockingQueue，
     * 该队列最初包含给定集合的元素，并按集合迭代器的遍历顺序添加。
     */
    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> collection) {
        this(capacity, fair);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = 0;
            try {
                for (E e : collection) {
                    checkNotNull(e);
                    items[i++] = e;
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                throw new IllegalArgumentException();
            }
            count = i;
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }

    public boolean add(E e) {
        return super.add(e);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        checkNotNull(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        checkNotNull(c);
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            int take = takeIndex;
            int i = 0;
            try {
                while (i < n) {
                    E x = (E) items[take];
                    c.add(x);
                    items[take] = null;
                    if (++take == items.length)
                        take = 0;
                    i++;
                }
                return n;
            } finally {
                if (i > 0) {
                    count -= i;
                    takeIndex = take;
                    if (itrs != null) {
                        if (count == 0)
                            itrs.queueIsEmpty();
                        else if (i > take)
                            itrs.takeIndexWrapped();
                    }
                    for (; i > 0 && lock.hasWaiters(notFull); i--)
                        notFull.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length)
                return false;
            else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        removeAt(i);
                        return true;
                    }
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int puIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        return true;
                    }
                    if (++i == items.length) {
                        i = 0;
                    }
                } while (i != puIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        Object[] a;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            a = new Object[count];
            int n = items.length - takeIndex;
            if (count <= n) {
                System.arraycopy(items, takeIndex, a, 0, count);
            } else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
        } finally {
            lock.unlock();
        }
        return a;
    }

    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <T> T[] toArray(T[] a) {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            final int len = a.length;
            if (len < count)
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
            int n = items.length - takeIndex;
            if (count <= n)
                System.arraycopy(items, takeIndex, a, 0, count);
            else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
            if (len > count)
                a[count] = null;
        } finally {
            lock.unlock();
        }
        return a;
    }

    @Override
    public void clear() {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = count;
            if (k > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    items[i] = null;
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
                takeIndex = putIndex;
                count = 0;
                if (itrs != null)
                    itrs.queueIsEmpty();
                for (; k > 0 && lock.hasWaiters(notFull); k--)
                    notFull.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * ArrayBlockingQueue的迭代器。
     * 为了保持关于 put 和 take 的弱一致性，我们提前读取了一个插槽，以便不报告 hasNext true，
     * 但随后没有要返回的元素。当所有索引都为负数时，或者当 hasNext 第一次返回 false 时，
     * 我们切换到“分离”模式（允许在没有 GC 帮助的情况下提示从 itrs 断开链接）。
     * 这允许迭代器完全准确地跟踪并发更新，但用户调用迭代器的情况除外。
     * hasNext（）返回 false 后删除（）。
     * 即使在这种情况下，我们也通过跟踪 lastItem 中要删除的预期元素来确保不会删除错误的元素。
     * 是的，如果 lastItem 在分离模式下由于交叉内部移除而移动，我们可能无法从队列中移除它。
     */
    private class Itr implements Iterator<E> {
        private int cursor;
        private E nextItem;
        private int nextIndex;
        private E lastItem;
        private int lastRet;
        private int prevTakeIndex;
        private int prevCycles;

        private static final int NONE = -1;
        private static final int REMOVED = -2;
        private static final int DETACHED = -3;

        Itr() {
            lastRet = NONE;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (count == 0) {
                    cursor = NONE;
                    nextIndex = NONE;
                    prevTakeIndex = DETACHED;
                } else {
                    final int takeIndex = ArrayBlockingQueue.this.takeIndex;
                    prevTakeIndex = takeIndex;
                    nextItem = itemAt(nextIndex = takeIndex);
                    cursor = incCursor(takeIndex);
                    if (itrs == null) {
                        itrs = new Itrs(this);
                    } else {
                        itrs.register(this);
                        itrs.doSomeSweeping(false);
                    }
                }
                prevCycles = itrs.cycles;
            } finally {
                lock.unlock();
            }
        }

        boolean isDetached() {
            return prevTakeIndex < 0;
        }

        private int incCursor(int index) {
            if (++index == items.length) {
                index = 0;
            }
            if (index == putIndex) {
                index = NONE;
            }
            return index;
        }

        /**
         * 当itrs应该停止跟踪此迭代器时调用，这可能是因为没有更多的索引要更新
         * （cursor < 0 && nextIndex < 0 && lastRet < 0），
         * 也可能是一个特殊的例外，当 lastRet >= 0 时，因为 hasNext（）将第一次返回 false。
         * 仅从迭代线程调用。
         */
        private void detach() {
            if (prevTakeIndex >= 0) {
                prevTakeIndex = DETACHED;
                itrs.doSomeSweeping(true);
            }
        }

        private boolean invalidated(int index, int prevTakeIndex, long dequeues, int length) {
            if (index < 0)
                return false;
            int distance = index - prevTakeIndex;
            if (distance < 0)
                distance += length;
            return dequeues > distance;
        }

        /**
         * 调整索引以合并自上次在此迭代器上执行操作以来的所有出列。仅从迭代线程调用。
         */
        private void incorporateDequeues() {
            final int cycles = itrs.cycles;
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;

            if (cycles != prevCycles || takeIndex != prevTakeIndex) {
                final int len = items.length;

                long dequeues = (cycles - prevCycles) * len + (takeIndex - prevTakeIndex);

                if (invalidated(lastRet, prevTakeIndex, dequeues, len))
                    lastRet = REMOVED;
                if (invalidated(nextIndex, prevTakeIndex, dequeues, len))
                    nextIndex = REMOVED;
                if (invalidated(cursor, prevTakeIndex, dequeues, len))
                    cursor = takeIndex;

                if (cursor < 0 && nextIndex < 0 && lastRet < 0)
                    detach();
                else {
                    this.prevCycles = cycles;
                    this.prevTakeIndex = takeIndex;
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (nextItem != null)
                return true;
            noNext();
            return false;
        }

        private void noNext() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached()) {
                    incorporateDequeues();
                    if (lastRet >= 0) {
                        lastItem = itemAt(lastRet);
                        detach();
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public E next() {
            final E x = nextItem;
            if (x == null)
                throw new NoSuchElementException();
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached())
                    incorporateDequeues();
                lastRet = nextIndex;
                final int cursor = this.cursor;
                if (cursor >= 0) {
                    nextItem = itemAt(nextIndex = cursor);
                    this.cursor = incCursor(cursor);
                } else {
                    nextIndex = NONE;
                    nextItem = null;
                }
            } finally {
                lock.unlock();
            }
            return x;
        }

        public void remove() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached())
                    incorporateDequeues();
                final int lastRet = this.lastRet;
                this.lastRet = NONE;
                if (lastRet >= 0) {
                    if (!isDetached())
                        removeAt(lastRet);
                    else {
                        final E lastItem = this.lastItem;
                        this.lastItem = null;
                        if (itemAt(lastRet) == lastItem)
                            removeAt(lastRet);
                    }
                } else if (lastRet == NONE)
                    throw new IllegalStateException();

                if (cursor < 0 && nextIndex < 0)
                    detach();
            } finally {
                lock.unlock();
            }
        }

        void shutdown() {
            cursor = NONE;
            if (nextIndex >= 0)
                nextIndex = REMOVED;
            if (lastRet >= 0) {
                lastRet = REMOVED;
                lastItem = null;
            }
            prevTakeIndex = DETACHED;
        }
    }

    class Itrs {
        private class Node extends WeakReference<Itr> {
            Node next;

            Node(Itr iterator, Node next) {
                super(iterator);
                this.next = next;
            }
        }

        int cycles = 0;
        private Node head;

        // 用于删除过时的迭代器
        private Node sweeper = null;

        private static final int SHORT_SWEEP_PROBES = 4;
        private static final int LONG_SWEEP_PROBES = 16;

        Itrs(Itr initial) {

        }

        void doSomeSweeping(boolean tryHarder) {

        }

        void register(Itr itr) {
            head = new Node(itr, head);
        }

        /**
         * 每当takeIndex变为 0 时调用。通知所有迭代器，并删除所有现在过时的迭代器。
         */
        void takeIndexWrapped() {


        }

        void removedAt(int removedIndex) {

        }

        void queueIsEmpty() {
            for (Node p = head; p != null; p = p.next) {
                Itr it = p.get();
                if (it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            head = null;
            itrs = null;
        }

        void elementDequeued() {
            if (count == 0)
                queueIsEmpty();
            else if (takeIndex == 0)
                takeIndexWrapped();
        }
    }

}
