package com.future.concurrent.java8;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用与类 PriorityQueue 相同的排序规则并提供阻塞检索操作的无界阻塞队列。
 * 虽然此队列在逻辑上是无界的，但由于资源耗尽（导致 OutOfMemoryError），尝试添加可能会失败。
 */
@SuppressWarnings({"unchecked", "unused"})
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * 要分配的最大数组大小。有些虚拟机在数组中保留一些头字。
     * 尝试分配较大的数组可能会导致 OutOfMemoryError：请求的数组大小超过 VM 限制
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 优先级队列表示为平衡二叉堆：
     * queue[n] 的两个子结点是 queue[2 * n + 1] 和 queue[2 *（n + 1）]。
     * 优先级队列由比较器排序，如果比较器为空，则由元素的自然顺序排序：
     * 对于堆中的每个节点 n 和 n 的每个后代 d，n <= d。
     * 假设队列为非空，则具有最低值的元素位于 queue[0] 中。
     */
    private transient Object[] queue;

    private transient int size;

    private transient Comparator<? super E> comparator;

    private final ReentrantLock lock;

    /**
     * 当队列为空的时候用于阻塞的条件队列。
     */
    private final Condition notEmpty;

    /**
     * 用于分配的自旋锁，通过 CSA 获取
     */
    private transient volatile int allocationSpinLock;

    /**
     * 仅用于序列化的普通 PriorityQueue，用于保持与此类早期版本的兼容性。
     * 仅在序列化/反序列化期间为非 null。
     */
    private PriorityQueue<E> q;

    public PriorityBlockingQueue() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[initialCapacity];
    }

    public PriorityBlockingQueue(Collection<? extends E> c) {
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        boolean heapify = true;
        boolean screen = true;
        if (c instanceof SortedSet<?>) {
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();
            heapify = false;
        } else if (c instanceof PriorityBlockingQueue<?>) {
            PriorityBlockingQueue<? extends E> pq = (PriorityBlockingQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            screen = false;
            if (pq.getClass() == PriorityBlockingQueue.class)
                heapify = false;
        }
        Object[] a = c.toArray();
        int n = a.length;
        if (c.getClass() != java.util.ArrayList.class)
            a = Arrays.copyOf(a, n, Object[].class);
        if (screen && (n == 1 || this.comparator != null)) {
            for (int i = 0; i < n; ++i)
                if (a[i] == null)
                    throw new NullPointerException();
        }
        this.queue = a;
        this.size = n;
        if (heapify)
            heapify();
    }

    private void heapify() {
        Object[] array = queue;
        int n = size;
        int half = (n >>> 1) - 1;
        Comparator<? super E> cmp = comparator;
        if (cmp == null) {
            for (int i = half; i >= 0; i--)
                siftDownComparable(i, (E) array[i], array, n);
        } else {
            for (int i = half; i >= 0; i--)
                siftDownUsingComparator(i, (E) array[i], array, n, cmp);
        }
    }

    private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (key.compareTo((T) e) >= 0) {
                break;
            }
            array[k] = e;
            k = parent;
        }
        array[k] = x;
    }

    private static <T> void siftUpUsingComparator(int k, T x, Object[] array, Comparator<? super T> cmp) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (cmp.compare(x, (T) e) >= 0) {
                break;
            }
            array[k] = e;
            k = parent;
        }
        array[k] = x;
    }

    /**
     * 在位置 k 处插入项 x，通过反复将 x 降级到树下，直到它小于或等于其子项或是叶，从而保持堆不变。
     */
    private static <T> void siftDownComparable(int k, T x, Object[] array, int n) {
        if (n > 0) {
            Comparable<? super T> key = (Comparable<? super T>) x;
            int half = n >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                Object c = array[child];
                int right = child + 1;
                if (right < n && ((Comparable<? super T>) c).compareTo((T) array[right]) > 0) {
                    c = array[child = right];
                }
                if (key.compareTo((T) c) <= 0)
                    break;
                array[k] = c;
                k = child;
            }
            array[k] = key;
        }
    }

    private static <T> void siftDownUsingComparator(int k, T x, Object[] array, int n, Comparator<? super T> cmp) {
        if (n > 0) {
            int half = n >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                Object c = array[child];
                int right = child + 1;
                if (right < n && cmp.compare((T) c, (T) array[right]) > 0)
                    c = array[child = right];
                if (cmp.compare(x, (T) c) <= 0)
                    break;
                array[k] = c;
                k = child;
            }
            array[k] = x;
        }
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    // poll(）的机制。仅在保持锁定状态时调用。
    private E dequeue() {
        int n = size - 1;
        if (n < 0)
            return null;
        else {
            Object[] array = queue;
            E result = (E) array[0];
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftDownComparable(0, x, array, n);
            else
                siftDownUsingComparator(0, x, array, n, cmp);
            size = n;
            return result;
        }
    }

    /**
     * 尝试增加数组以容纳至少一个以上的元素（但通常会扩展大约 50%），
     * 放弃（允许重试）争用（我们认为这是罕见的）。仅在保持锁定状态时调用。
     */
    private void tryGrow(Object[] array, int oldCap) {
        // must release and then re-acquire main lock
        lock.unlock();
        Object[] newArray = null;
        if (allocationSpinLock == 0 && unsafe.compareAndSwapInt(this, allocationSpinLockOffset, 0, 1)) {
            try {
                int newCap = oldCap + ((oldCap < 64) ? (oldCap + 2) : (oldCap >> 1));
                if (newCap - MAX_ARRAY_SIZE > 0) {
                    int minCap = oldCap + 1;
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError();
                    newCap = MAX_ARRAY_SIZE;
                }
                if (newCap > oldCap && queue == array)
                    newArray = new Object[newCap];
            } finally {
                allocationSpinLock = 0;
            }
        }
        // back off if another thread is allocating
        if (newArray == null)
            Thread.yield();
        lock.lock();
        if (newArray != null && queue == array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
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
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 由于队列是无界的，因此此方法永远不会阻塞。
     */
    @Override
    public void put(E e) {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ((result = dequeue()) == null)
                notEmpty.await();
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ((result = dequeue()) == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(size, maxElements);
            for (int i = 0; i < n; i++) {
                c.add((E) queue[0]);
                dequeue();
            }
        } finally {
            lock.unlock();
        }
        return 0;
    }

    @Override
    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        int n, cap;
        Object[] array;
        while ((n = size) >= (cap = (array = queue).length))
            tryGrow(array, cap);
        try {
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftUpComparable(n, e, array);
            else
                siftUpUsingComparator(n, e, array, cmp);
            size = n + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (size == 0) ? null : (E) queue[0];
        } finally {
            lock.unlock();
        }
    }

    private int indexOf(Object o) {
        if (o != null) {
            Object[] array = queue;
            int n = size;
            for (int i = 0; i < n; i++)
                if (o.equals(array[i]))
                    return i;
        }
        return -1;
    }

    private void removeAt(int i) {
        Object[] array = queue;
        int n = size - 1;
        if (n == i)
            array[i] = null;
        else {
            E moved = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null) {
                siftDownComparable(i, moved, array, n);
            } else {
                siftDownUsingComparator(i, moved, array, n, cmp);
            }
            if (array[i] == moved) {
                if (cmp == null)
                    siftUpComparable(i, moved, array);
                else
                    siftUpUsingComparator(i, moved, array, cmp);
            }
        }
        size = n;
    }

    @Override
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = indexOf(o);
            if (i == -1) return false;
            removeAt(i);
            return true;
        } finally {
            lock.unlock();
        }
    }

    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            for (int i = 0, n = size; i < n; i++) {
                if (o == array[i]) {
                    removeAt(i);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return indexOf(o) != -1;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            int n = size;
            size = 0;
            for (int i = 0; i < n; i++) {
                array[i] = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return Arrays.copyOf(queue, size);
        } finally {
            lock.unlock();
        }
    }

    final class Itr implements Iterator<E> {
        final Object[] array;
        int cursor;
        int lastRet;

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        @Override
        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E) array[cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }

    private static final Unsafe unsafe;
    private static final long allocationSpinLockOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
            allocationSpinLockOffset = unsafe.objectFieldOffset(PriorityBlockingQueue.class.getDeclaredField("allocationSpinLock"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
