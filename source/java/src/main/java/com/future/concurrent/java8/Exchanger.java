package com.future.concurrent.java8;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

@SuppressWarnings("all")
public class Exchanger {

    /**
     * 竞技场中任何两个使用的插槽之间的字节距离（作为一个移位值）。1 << ASHIFT 应该至少是高速缓存行的大小
     */
    private static final int ASHIFT = 7;

    /**
     * 支持的最大竞技场索引。可分配的最大竞技场大小为 MMASK + 1。必须是 2 减 1 的幂，
     * 小于（1<<（31 - ASHIFT））。255（0xff）的上限足以满足主要算法的预期缩放限制。
     */
    private static final int MMASK = 0xff;

    /**
     * 绑定字段的序列/版本位的单位。绑定的每一次成功改变也会增加SEQ。
     */
    private static final int SEQ = MMASK + 1;

    /**
     * CPU 的数量，用于尺寸和旋转控制
     */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * 竞技场的最大槽位索引。原则上可以容纳所有线程而不发生争执的槽位数量，或最多可索引的最大值。
     */
    static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;

    /**
     * 在等待匹配的过程中，旋转的界限。由于随机化的原因，实际迭代次数平均约为该值的两倍。
     * 注意：当 NCPU == 1 时，旋转功能被禁用。
     */
    private static final int SPINS = 1 << 10;

    /**
     * 代表公共方法的空参数/返回的值。之所以需要这个值，是因为 API 最初并不允许空参数，它应该是这样的。
     */
    private static final Object NULL_ITEM = new Object();

    /**
     * 内部交换方法在超时时返回的哨兵值，以避免这些方法的单独定时版本。
     */
    private static final Object TIMED_OUT = new Object();

    @sun.misc.Contended
    static final class Node {
        int index;
        int bound;
        int collides;
        int hash;
        Object item;
        volatile Object match;
        volatile Thread parked;
    }

    static final class Participant extends ThreadLocal<Node> {
        @Override
        protected Node initialValue() {
            return new Node();
        }
    }

    private final Participant participant;

    /**
     * 消除数组；在启用之前为空（在 slotExchange 内）。
     * 元素访问使用仿真的 volatile 获取和 CAS。
     */
    private volatile Node[] arena;

    // 槽位使用，直到检测到争夺。
    private volatile Node slot;

    /**
     * 最大的有效竞技场位置的索引，与高位的 SEQ 号码进行 OR'ed，在每次更新时递增。
     * 从 0 到 SEQ 的初始更新是用来确保竞技场数组只被构建一次。
     */
    private volatile int bound;

    public Exchanger() {
        participant = new Participant();
    }

    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = arena;
        Node p = participant.get();
        for (int i = p.index; ; ) {
            int b, m, c;
            long j;
            Node q = (Node) U.getObjectVolatile(a, j = (i << ASHIFT) + ABASE);
            if (q != null && U.compareAndSwapObject(a, j, q, null)) {
                Object v = q.item;
                q.match = item;
                Thread w = q.parked;
                if (w != null)
                    U.unpark(w);
                return v;
            } else if (i <= (m = (b = bound) & MMASK) && q == null) {
                p.item = item;
                if (U.compareAndSwapObject(a, j, null, p)) {
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                    Thread t = Thread.currentThread();
                    for (int h = p.hash, spins = SPINS; ; ) {
                        Object v = p.match;
                        if (v != null) {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;
                            p.hash = h;
                            return v;
                        } else if (spins > 0) {
                            h ^= h << 1;
                            h ^= h >>> 3;
                            h ^= h << 10;
                            if (h == 0)
                                h = SPINS | (int) t.getId();
                            else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                                Thread.yield();
                        } else if (U.getObjectVolatile(a, j) != p)
                            spins = SPINS;
                        else if (!t.isInterrupted() && m == 0 && (!timed || (ns = end - System.nanoTime()) > 0L)) {
                            U.putObject(t, BLOCKER, this);
                            p.parked = t;
                            if (U.getObjectVolatile(a, j) == p)
                                U.park(false, ns);
                            p.parked = null;
                            U.putObject(t, BLOCKER, null);
                        } else if (U.getObjectVolatile(a, j) == p && U.compareAndSwapObject(a, j, p, null)) {
                            if (m != 0)
                                U.compareAndSwapInt(this, BOUND, b, b + SEQ - 1);
                            p.item = null;
                            p.hash = h;
                            i = p.index >>>= 1;
                            if (Thread.interrupted())
                                return null;
                            if (timed && m == 0 && ns <= 0L)
                                return TIMED_OUT;
                            break;
                        }
                    }
                } else {
                    p.item = null;
                }
            } else {
                if (p.bound != b) {
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                } else if ((c = p.collides) < m || m == FULL || !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) {
                    p.collides = c + 1;
                    i = (i == 0) ? m : i - 1;
                } else {
                    i = m + 1;
                }
                p.index = i;
            }
        }
    }

    private final Object slotExchange(Object item, boolean timed, long ns) {
        Node p = participant.get();
        Thread t = Thread.currentThread();
        if (t.isInterrupted())
            return null;

        for (Node q; ; ) {
            if ((q = slot) != null) {
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    Object v = q.item;
                    q.match = item;
                    Thread w = q.parked;
                    if (w != null)
                        U.unpark(w);
                    return v;
                }
                if (NCPU > 1 && bound == 0 && U.compareAndSwapInt(this, BOUND, 0, SEQ))
                    arena = new Node[(FULL + 2) << ASHIFT];
            } else if (arena != null) {
                return null;
            } else {
                p.item = item;
                if (U.compareAndSwapObject(this, SLOT, null, p))
                    break;
                p.item = null;
            }
        }

        // await release
        int h = p.hash;
        long end = timed ? System.nanoTime() + ns : 0L;
        int spins = (NCPU > 1) ? SPINS : 1;
        Object v;
        while ((v = p.match) == null) {
            if (spins > 0) {
                h ^= h << 1;
                h ^= h >>> 3;
                h ^= h << 10;
                if (h == 0)
                    h = SPINS | (int) t.getId();
                else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                    Thread.yield();
            } else if (slot != p) {
                spins = SPINS;
            } else if (!t.isInterrupted() && arena == null && (!timed || (ns = end - System.nanoTime()) > 0L)) {
                U.putObject(t, BLOCKER, this);
                p.parked = t;
                if (slot == p)
                    U.park(false, ns);
                p.parked = null;
                U.putObject(t, BLOCKER, null);
            } else if (U.compareAndSwapObject(this, SLOT, p, null)) {
                v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;
                break;
            }
        }
        U.putOrderedObject(p, MATCH, null);
        p.item = null;
        p.hash = h;
        return v;
    }


    private static final sun.misc.Unsafe U;
    private static final long BOUND;
    private static final long SLOT;
    private static final long MATCH;
    private static final long BLOCKER;
    private static final int ABASE;

    static {
        int s;
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
            BOUND = U.objectFieldOffset(Exchanger.class.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset(Exchanger.class.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset(Node.class.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            s = U.arrayIndexScale(Node[].class);
            ABASE = U.arrayBaseOffset(Node[].class) + (1 << ASHIFT);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
        if ((s & (s - 1)) != 0 || s > (1 << ASHIFT))
            throw new Error("Unsupported array scale");
    }
}























