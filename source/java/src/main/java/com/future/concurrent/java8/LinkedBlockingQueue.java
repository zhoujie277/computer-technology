package com.future.concurrent.java8;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于链接节点的可选有界阻塞队列。该队列对元素进行 FIFO 排序（先进先出）。
 * 队列的头是在队列上停留时间最长的元素。队列的尾部是在队列上停留时间最短的元素。
 * 新元素插入到队列的尾部，队列检索操作获取队列头部的元素。
 * 链接队列通常比基于 Array 的队列具有更高的吞吐量，但在大多数并发应用程序中性能不太可预测。
 * <p>
 * 可选的 capacity-bound 构造函数参数用作防止队列过度扩展的方法。容量（如果未指定）等于 Integer.MAX_VALUE。
 * 链接节点在每次插入时动态创建，除非这会使队列超出容量。
 */
@SuppressWarnings("unused")
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    /**
     * “双锁队列”算法的一种变体。The putLock 锁定 put (and offer)，并具有 waiting puts 关联的条件队列。
     * takeLock 也是如此。
     * 它们都依赖的 “count” 字段作为一个原子进行维护，以避免在大多数情况下需要获得两个锁。
     * 此外，为了最大限度地减少 put 获取 takeLock 的需要，也可以使用级联通知。
     * 当 put 注意到它至少启用了一个 take 时，将向 taker 发送信号。
     * 如果信号发出后输入了更多的项目，则该接受者依次向其他人发出信号。和对称地用于取放。
     * 诸如 remove（Object）和迭代器之类的操作获取这两个锁。
     * <p>
     * 写者和读者之间的可见性如下所示：
     * <p>
     * 每当一个元素排队时，就会获取 putLock 并更新计数。
     * 后续读者通过获取 putLock（通过 fullyLock）或获取 takeLock，然后读取 n = count.get() 来保证排队节点的可见性；
     * 这样就可以看到前 n 个项目。
     * <p>
     * 要实现弱一致性迭代器，我们似乎需要保持所有节点都可以从先前的出列节点 GC 访问。
     * 这将导致两个问题：
     * + 允许恶意迭代器导致无限内存保留
     * + 如果某个节点在使用期间是长期存在的，则会导致旧节点与新节点的跨代链接，这一点各代GCs 很难处理，从而导致重复的主要收集。
     * 然而，只有未删除的节点才需要能够从出列的节点中访问，并且可达性不一定必须是 GC 能够理解的类型。
     * 我们使用的技巧是将刚出列的节点链接到自身。这样的自我链接意味着推进到 head.next。
     */
    static class Node<E> {
        E item;
        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    private final int capacity;

    private final AtomicInteger count = new AtomicInteger();

    transient Node<E> head;

    private transient Node<E> last;

    private final ReentrantLock takeLock = new ReentrantLock();

    private final Condition notEmpty = takeLock.newCondition();

    private final ReentrantLock putLock = new ReentrantLock();

    private final Condition notFull = putLock.newCondition();

    /**
     * 发出等待的信号。仅从 put/offer 调用（通常不锁定takeLock）
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 发出等待的信号。仅从 take/poll 调用。
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    private void enqueue(Node<E> node) {
        last = last.next = node;
    }

    private E dequeue() {
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<>(null);
    }

    public LinkedBlockingQueue(Collection<? extends E> collection) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            int n = 0;
            for (E e : collection) {
                if (e == null)
                    throw new NullPointerException();
                if (n == capacity)
                    throw new IllegalStateException("Queue full");
                enqueue(new Node<>(e));
                ++n;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return count.get();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        int c = -1;
        Node<E> node = new Node<>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
                请注意，计数在等待保护中使用，即使它不受锁保护。
                这是因为计数只能在此时减少（所有其他put都被锁关闭），
                如果容量发生变化，我们（或其他等待put）将收到信号。
                类似地，count在其他等待保护中的所有其他用法。
             */
            while (count.get() == capacity) {
                notFull.await();
            }
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty();
        }
        return true;
    }

    @Override
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0)
                notEmpty.await();
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    @Override
    public int remainingCapacity() {
        return capacity - count.get();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            Node<E> h = head;
            int i = 0;
            try {
                while (i < n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                if (i > 0) {
                    head = h;
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull)
                signalNotFull();
        }
    }

    /**
     * 如果可以在不超过队列容量的情况下立即在此队列末尾插入指定的元素，则在成功时返回 true，
     * 如果此队列已满，则返回false。
     * 当使用容量受限队列时，此方法通常比方法 add 更可取，后者只能通过抛出异常来插入元素。
     */
    @Override
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return false;
        int c = -1;
        Node<E> node = new Node<>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return c >= 0;
    }

    @Override
    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    @Override
    public E peek() {
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }

    void unlink(Node<E> p, Node<E> trail) {
        p.item = null;
        trail.next = p.next;
        if (last == p)
            last = trail;
        if (count.getAndDecrement() == capacity)
            notFull.signal();
    }

    /**
     * 从该队列中删除指定元素的单个实例（如果存在）。
     * 更正式地说，如果此队列包含一个或多个这样的元素，则删除元素 e，使 o.equals（e）。
     * 如果此队列包含指定的元素，则返回 true（如果此队列因调用而更改，则返回等效值）。
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> trail = head, p = trail.next; p != null; trail = p, p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> p = head.next; p != null; p = p.next) {
                if (o.equals(p.item))
                    return true;
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public Object[] toArray() {
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            if (a.length < size)
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public void clear() {
        fullyLock();
        try {
            /**
             * for (Node<E> h = head, p = h.next; p != null; ) {
             *                 h.next = h;
             *                 p.item = null;
             *                 h = p;
             *                 p = h.next;
             *             }
             */
            // 这个代码，上面是简单易懂版
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            if (count.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            fullyUnlock();
        }
    }

    private class Itr implements Iterator<E> {
        private Node<E> current;
        private Node<E> lastRet;
        private E concurrentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null)
                    concurrentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        private Node<E> nextNode(Node<E> p) {
            for (; ; ) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        @Override
        public E next() {
            fullyLock();
            try {
                if (current == null)
                    throw new NoSuchElementException();
                E x = concurrentElement;
                lastRet = current;
                current = nextNode(current);
                concurrentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        @Override
        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next; p != null; trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }
}

















