package com.future.concurrent.history.javaold;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("all")
public class Java5Exchanger<V> {

    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * 竞技场的容量。 设置为一个能提供足够空间处理竞争的值。
     * 在小机器上，大多数槽不会被使用，但它仍然没有被浪费，
     * 因为额外的空间提供了一些机器级的地址填充，以减少对大量 CAS 槽位置的干扰。
     * 而在非常大的机器上，性能最终会受到内存带宽的限制，而不是线程/中央处理器的数量。
     * 如果不修改索引和散列算法，就不能改变这个常数。
     */
    private static final int CAPACITY = 32;

    /**
     * 可以容纳所有线程而不发生争执的 "最大 "值。 当这个值小于 CAPACITY 时，可以避免一些原本浪费的扩展。
     */
    private static final int FULL = Math.max(0, Math.min(CAPACITY, NCPU / 2) - 1);

    /**
     * 在阻塞或在等待履行时放弃之前，旋转的次数（除了轮询一个内存位置之外什么都不做）。
     * 在单处理器上应该是 0。 在多处理器上，这个值应该足够大，
     * 以便两个尽可能快地交换项目的线程只有在其中一个停滞时（由于 GC 或抢占）才会阻塞，
     * 但不会更长，以避免浪费 CPU 资源。 换个角度看，这个值是大多数系统上平均上下文切换时间的一半多一点。
     * 这里的数值大约是一系列测试中的平均数值。系统的平均值。
     */
    private static final int SPINS = (NCPU == 1) ? 0 : 2000;

    /**
     * 在定时等待中，阻塞前的旋转次数。定时等待的旋转速度更慢，
     * 因为检查时间需要时间。 最佳值主要取决于System.nanoTime与内存访问的相对速率。
     * 根据经验，这个值在各种系统中都能很好地工作。
     */
    private static final int TIMED_SPINS = SPINS / 20;

    /**
     * 哨兵，代表由于中断、超时或过期的旋转等待而取消的等待。
     * 这个值在取消时被放置在孔中，并作为等待方法的返回值来表示设置或获取孔的失败。
     */
    private static final Object CANCEL = new Object();

    /**
     * 代表公共方法的空参数/返回的值。 这与内部要求区分开来，即洞开始为 null，意味着它们还没有被设置。
     */
    private static final Object NULL_ITEM = new Object();

    /**
     * 节点持有部分交换的数据。 这个类适时地子类化了 AtomicReference 来表示洞。
     * 所以 get() 返回 hole，而 compareAndSet CAS 的值进入 hole。 这个类不能被参数化为 "V"，
     * 因为它使用了非 V 的 CANCEL 哨兵。
     */
    private static final class Node extends AtomicReference<Object> {
        public final Object item;
        public volatile Thread waiter;

        public Node(Object item) {
            this.item = item;
        }
    }

    /**
     * Slot是一个带有启发式填充的AtomicReference，以减少这个重度 CAS 的位置的缓存影响。
     * 虽然填充会增加明显的空间，但所有的槽都是按需创建的，只有在提高吞吐量超过使用额外空间时才会有多个槽。
     */
    private static final class Slot extends AtomicReference<Object> {
        // 提高 <= 64 字节高速缓存行的隔离的可能性
        long q0, q1, q2, q3, q4, q5, q6, q7, q8, q9, qa, qb, qc, qd, qe;
    }

    /**
     * 槽位数组。 当需要时，元素被懒惰地初始化。
     * 声明为 volatile，以实现双重检查的懒惰构建。
     */
    private volatile Slot[] arena = new Slot[CAPACITY];

    /**
     * 正在使用的最大插槽索引。 当一个线程经历了太多的 CAS 争夺时，这个值有时会增加，有时会在自旋等待结束后减少。
     * 仅通过 compareAndSet 进行更改，以避免在设置前线程恰好停滞时出现陈旧的值。
     */
    private final AtomicInteger max = new AtomicInteger();

    /**
     * 主交换函数，处理不同的策略变体。 使用 Object，而不是 "V "作为参数和返回值，以简化对哨兵值的处理。
     * 来自公共方法的调用者会相应地进行解码转换。
     */
    private Object doExchange(Object item, boolean timed, long nanos) {
        Node me = new Node(item);
        int index = hashIndex();
        int fails = 0;

        for (; ; ) {
            Object y;
            Slot slot = arena[index];
            if (slot == null)
                createSlot(index);
            else if ((y = slot.get()) != null && slot.compareAndSet(y, null)) {
                Node you = (Node) y;
                if (you.compareAndSet(null, item)) {
                    LockSupport.unpark(you.waiter);
                    return you.item;
                }
            } else if (y == null && slot.compareAndSet(null, me)) {
                if (index == 0)
                    return timed ? awaitNanos(me, slot, nanos) : await(me, slot);
                Object v = spinWait(me, slot);
                if (v != CANCEL)
                    return v;
                me = new Node(item);
                int m = max.get();
                if (m > (index >>>= 1))
                    max.compareAndSet(m, m - 1);
            } else if (++fails > 1) {
                int m = max.get();
                if (fails > 3 && m < FULL && max.compareAndSet(m, m + 1))
                    index = m + 1;
                else if (--index < 0)
                    index = m;
            }
        }
    }

    private void createSlot(int index) {
        // 在锁外创建槽，缩小同步区域
        Slot newSlot = new Slot();
        Slot[] a = arena;
        synchronized (a) {
            if (a[index] == null)
                a[index] = newSlot;
        }
    }

    private static Object spinWait(Node node, Slot slot) {
        int spins = SPINS;
        for (; ; ) {
            Object v = node.get();
            if (v != null)
                return v;
            else if (spins > 0)
                --spins;
            else
                tryCancel(node, slot);
        }
    }

    /**
     * 试图取消在给定槽中等待的给定节点的等待，如果是这样，帮助从其槽中清除节点，以避免垃圾保留。
     */
    private static boolean tryCancel(Node node, Slot slot) {
        if (!node.compareAndSet(null, CANCEL))
            return false;
        if (slot.get() == node)
            slot.compareAndSet(node, null);
        return true;
    }

    private static Object await(Node node, Slot slot) {
        Thread w = Thread.currentThread();
        int spins = SPINS;
        for (; ; ) {
            Object v = node.get();
            if (v != null)
                return v;
            else if (spins > 0)
                --spins;
            else if (node.waiter == null)
                node.waiter = w;
            else if (w.isInterrupted())
                tryCancel(node, slot);
            else
                LockSupport.park();
        }
    }

    private Object awaitNanos(Node node, Slot slot, long nanos) {
        int spins = TIMED_SPINS;
        long lastTime = 0;
        Thread w = null;
        for (; ; ) {
            Object v = node.get();
            if (v != null)
                return v;
            long now = System.nanoTime();
            if (w == null)
                w = Thread.currentThread();
            else
                nanos -= now - lastTime;
            lastTime = now;
            if (nanos > 0) {
                if (spins > 0)
                    --spins;
                else if (node.waiter == null)
                    node.waiter = w;
                else if (w.isInterrupted())
                    tryCancel(node, slot);
                else
                    LockSupport.parkNanos(nanos);
            } else if (tryCancel(node, slot) && !w.isInterrupted())
                return scanOnTimeout(node);
        }
    }

    /**
     * 扫过竞技场，检查是否有等待的线程。
     * 只有在 0 号槽等待时从超时返回时才会被调用。
     * 当一个线程放弃定时等待时，有可能先前进入的线程仍在其他槽中等待。
     * 所以我们要扫描以检查是否有。
     * 这几乎是多余的，但当有其他线程存在时，超时的可能性会大大降低，
     * 而在基于锁的交换器中，较早到达的线程可能还在 在这种情况下，较早到达的线程可能仍在等待入口锁。
     */
    private Object scanOnTimeout(Node node) {
        Object y;
        for (int j = arena.length - 1; j >= 0; --j) {
            Slot slot = arena[j];
            if (slot != null) {
                while ((y = slot.get()) != null) {
                    if (slot.compareAndSet(y, null)) {
                        Node you = (Node) y;
                        if (you.compareAndSet(null, node.item)) {
                            LockSupport.unpark(you.waiter);
                            return you.item;
                        }
                    }
                }
            }
        }
        return CANCEL;
    }

    public V exchange(V x) throws InterruptedException {
        if (!Thread.interrupted()) {
            Object v = doExchange(x == null ? NULL_ITEM : x, false, 0);
            if (v == NULL_ITEM)
                return null;
            if (v != CANCEL)
                return x;
            Thread.interrupted();
        }
        throw new InterruptedException();
    }

    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (!Thread.interrupted()) {
            Object v = doExchange(x == null ? NULL_ITEM : x, true, unit.toNanos(timeout));
            if (v == NULL_ITEM)
                return null;
            if (v != CANCEL)
                return (V) v;
            if (!Thread.interrupted())
                throw new TimeoutException();
        }
        throw new InterruptedException();
    }

    /**
     * 为了返回一个介于 0 和 max 之间的索引，我们使用了一个廉价的近似于 mod 的操作，
     * 这也修正了由于非 2 次方重磨而产生的偏差。
     * 哈希码的位被屏蔽为 "nbits"，即表大小的2的最高幂（在一个装入三个ints的表中查找）。
     * 如果太大，在旋转哈希码nbits位后重新尝试，同时强迫新的顶位为0，
     * 这保证了最终的终止（虽然有一个非随机的偏向）。
     * 对于所有表的大小，这需要平均不到 2 次的尝试，并且当应用于所有的槽概率时，
     * 与完全均匀的槽概率有最大 2% 的差异。小于 32 的所有可能的哈希代码时，与完全均匀的槽概率的差别最大为 2%。
     */
    private final int hashIndex() {
        long id = Thread.currentThread().getId();
        int hash = (((int) (id ^ (id >>> 32))) ^ 0x811c9dc5) * 0x01000193;

        int m = max.get();
        int nbits = (((0xfffffc00 >> m) & 4) | // Compute ceil(log2(m+1))
                ((0x000001f8 >>> m) & 2) | // The constants hold
                ((0xffff00f2 >>> m) & 1)); // a lookup table
        int index;
        while ((index = hash & ((1 << nbits) - 1)) > m)       // May retry on
            hash = (hash >>> nbits) | (hash << (33 - nbits)); // non-power-2 m
        return index;
    }
}



























