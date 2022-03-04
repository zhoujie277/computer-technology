package com.future.concurrent.java8;

import com.google.common.graph.Traverser;
import com.sun.org.apache.bcel.internal.classfile.ClassElementValue;
import sun.misc.Unsafe;

import javax.swing.*;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    /**
     * 可能的最大表容量。这个值必须正好是1<<30，以保持在Java数组分配和索引的界限内，
     * 因为 32 位哈希字段的前两位被用于控制目的，所以进一步要求。
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认的初始表容量。必须是 2 的幂（即至少是1），最多就是 MAXIMUM_CAPACITY。
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 最大的可能（非二的幂）数组大小。toArray 和相关方法需要。
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 该表的默认并发级别。未使用，但为了与该类以前的版本兼容而定义。
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 这个表的负载系数。在构造函数中重写这个值只影响初始表的容量。
     * 通常不使用实际的浮点值 -- 使用表达式更简单，如 n - (n >> 2) 来表示相关的调整阈值。
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 使用树形而不是列表的斌的计数阈值。当添加一个元素到一个至少有这么多节点的 bin 时，
     * bin 被转换为树。这个值必须大于 2，而且至少应该是 8，以便与树状移除中关于收缩时转换回普通斌的假设相匹配。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 在调整大小的操作过程中，取消树状物（分割）的斌数阈值。应该小于 TREEIFY_THRESHOLD，并且最多 6 个，
     * 以便在移除时进行网格收缩检测。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 可以树化的最小的表容量。(这个值至少应该是 4 * TREEIFY_THRESHOLD，以避免调整大小和树化阈值之间的冲突。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 每一传输步骤的最小重合次数。范围被细分，以允许多个调整器线程。
     * 这个值作为一个下限，以避免调整器遇到过度的内存争夺。该值应该至少是 DEFAULT_CAPACITY。
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * sizeCtl 中用于生成印章的比特数。对于 32 位数组必须至少是 6 位。
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * 可以帮助调整大小的最大线程数。必须适合32 - RESIZE_STAMP_BITS位。
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * 在 sizeCtl 中记录尺寸印章的位移。
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;


    static final int MOVED = -1;
    static final int TREEBIN = -2;
    static final int RESERVED = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("segments", Segment[].class),
            new ObjectStreamField("segmentMask", Integer.TYPE),
            new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /**
     * 上一版本中使用的辅助类的精简版，为了序列化的兼容性而声明。
     */
    static class Segment<K, V> extends ReentrantLock {
        final float loadFactor;

        Segment(float lf) {
            this.loadFactor = lf;
        }
    }

    /**
     * 一个用于分配计数的填充单元。改编自 LongAdder 和 Striped64。解释见他们的内部文档。
     */
    @sun.misc.Contended
    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            value = x;
        }
    }


    /**
     * 键值条目。这个类永远不会作为用户可修改的 Map.Entry（即支持 setValue 的，见下面的 MapEntry）输出，
     * 但可以用于批量任务中使用的只读遍历。具有负散列字段的 Node 的子类是特殊的，它包含空的键和值（但从未被导出）。
     * 否则，key 和 vals 永远不会为空。
     */
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K, V> next;

        Node(int hash, K key, V val, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return key;
        }

        @Override
        public final V getValue() {
            return null;
        }

        public final int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        public final String toString() {
            return key + "=" + val;
        }

        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u;
            Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * 对 map.get() 的虚拟化支持；在子类中被重写。
         */
        Node<K, V> find(int h, Object k) {
            Node<K, V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h && ((ek = e.key) == k || (k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute
     */
    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        Node<K, V> find(int h, Object k) {
            return null;
        }
    }

    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K, V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer:
            for (Node<K, V>[] tab = nextTable; ; ) {
                Node<K, V> e;
                int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (; ; ) {
                    int eh;
                    K ek;
                    if ((eh = e.hash) == h && ((ek = e.key) == k || k.equals(ek)))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K, V>) e).nextTable;
                            continue outer;
                        } else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * 将哈希值的高位传播（XOR）到低位，并将最高位强制为0。
     * 因为表使用了 2 次方掩码，仅在当前掩码以上的位数不同的一组哈希值总是会发生碰撞。
     * (已知的例子包括在小表中持有连续整数的 Float 键的集合）。
     * 因此，我们应用一个转换，将高位的影响向下分散。
     * 在速度、实用性和位传播的质量之间有一个权衡。因为许多常见的哈希集已经是合理分布的（所以不受益于传播），
     * 而且因为我们使用树来处理大集的碰撞，我们只是以最便宜的方式 XOR 一些移位的比特，以减少系统损失，
     * 以及纳入最高比特的影响，否则由于表的界限，永远不会被用于索引计算。
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * 返回给定的所需容量的表尺寸的 2 次方。见《黑客的快乐》，第3.2节
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            //noinspection ConstantConditions
            if ((ts = c.getGenericInterfaces()) != null) {
                for (Type type : ts) {
                    if (((t = type) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable) k).compareTo(x));
    }

    /*
     volatile 的访问方法用于表元素以及正在进行的下一个表的元素，同时调整大小。
     所有 tab 参数的使用必须被调用者检查为空。
     所有的调用者也会偏执地预先检查 tab 的长度是否为零（或者一个等效的检查），
     从而确保任何采取哈希值形式的索引参数和（length - 1）是一个有效的索引。
      请注意，为了正确应对用户的任意并发错误，
      这些检查必须对局部变量进行操作，这就解释了下面一些看起来很奇怪的内联赋值。
      注意，对 setTabAt 的调用总是发生在锁定区域内，
      因此原则上只需要释放排序，而不是完全的 volatile 语义，
      但目前被编码为 volatile 的写入以示保守。
     */

    @SuppressWarnings("unchecked")
    static final <K, V> Node<K, V> tabAt(Node<K, V>[] tab, int i) {
        return (Node<K, V>) U.getObjectVolatile(tab, ((long) i << ASHIFT) + ABASE);
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
        return U.compareAndSwapObject(tab, ((long) i << ASHIFT) + ABASE, c, v);
    }

    static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
        U.putObjectVolatile(tab, ((long) i << ASHIFT) + ABASE, v);
    }

    /**
     * 仓位的数组。在第一次插入时被懒惰地初始化。大小总是 2 的幂。可以通过迭代器直接访问。
     */
    volatile Node<K, V>[] table;
    /**
     * 下一个要使用的表；只有在调整大小时才是非空的。
     */
    private volatile Node<K, V>[] nextTable;
    /**
     * 基准计数器值，主要在没有竞争的时候使用，但也作为表初始化竞赛期间的回退。通过 CAS 更新。
     */
    private volatile long baseCount;
    /**
     * 表的初始化和调整大小的控制。当为负数时，表正在被初始化或调整大小：
     * -1 为初始化，
     * 否则为 -（1 + 正在进行 resize 线程的数量）。
     * 否则，当表为空时，持有创建时使用的初始表大小，或 0 为默认值。
     * 初始化后，持有下一个元素计数值，据此来调整表的大小。
     * 高 16 位是扩容标识戳，低 16 位是扩容线程数 + 1。
     */
    private volatile int sizeCtl;
    /**
     * 在调整大小时，下一个表的索引（加一）要分割。
     */
    private volatile int transferIndex;
    /**
     * 自旋锁（通过 CAS 锁定）在调整大小和/或创建 CounterCells 时使用。
     */
    private volatile int cellsBusy;
    /**
     * 计数单元的表。当非空时，大小是 2 的幂。
     */
    private volatile CounterCell[] counterCells;

    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        int cap = (size >= (long) MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }

    final long sumCount() {
        CounterCell[] as = counterCells;
        CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (CounterCell counterCell : as) {
                if ((a = counterCell) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long) Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int) n);
    }

    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }


    public V get(Object key) {
        Node<K, V>[] tab;
        Node<K, V> e, p;
        int n, eh;
        K ek;
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || key.equals(ek))
                    return e.val;
            } else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h && ((ek = e.key) == key || (key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        Node<K, V>[] t;
        if ((t = table) != null) {
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || value.equals(v))
                    return true;
            }
        }
        return false;
    }

    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<>(hash, key, value, null)))
                    break;
            } else if ((fh = f.hash) == MOVED)
                continue;
//                tab =
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f; ; ++binCount) {
                                K ek;
                                if (e.hash == hash && ((ek = e.key) == key || key.equals(ek))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<>(hash, key, value, null);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            Node<K, V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K, V>) f).putTreeVal(hash, key, value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * 添加到计数中，如果表太小且尚未调整大小，则启动转移。
     * 如果已经在调整大小，如果有工作可做，则帮助执行转移。
     * 转移后重新检查占用情况，看是否已经需要再次调整大小，因为调整大小是滞后于增加的。
     */
    private final void addCount(long x, int check) {
//        CounterCell[] as;
//        long b, s;
//        if ((as = counterCells) != null || !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
//            CounterCell a;
//            long v;
//            int m;
//            boolean uncontended = true;
//            if (as == null || (m = as.length - 1) < 0 ||
//                    (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
//                    !(uncontended =
//                            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
////                fullAddCount(x, uncontended);
//                return;
//            }
//        }
    }


    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return ConcurrentMap.super.getOrDefault(key, defaultValue);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return ConcurrentMap.super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.compute(key, remappingFunction);
    }

    @Override
    public V remove(Object key) {
        return super.remove(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        ConcurrentMap.super.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        ConcurrentMap.super.replaceAll(function);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return ConcurrentMap.super.merge(key, value, remappingFunction);
    }

    private final Node<K, V>[] initTable() {
        Node<K, V>[] tab;
        int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield();
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
                        table = tab = nt;
                        // sc = n * (3/4) = n - n/4 = n - (n >>> 2)
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }

    final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
        Node<K, V>[] nextTab;
        int sc;
        if (tab != null && (f instanceof ForwardingNode) && (nextTab = ((ForwardingNode<K, V>) f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && tab == table && (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

    /**
     * 试图预设表格大小，以适应给定的元素数量。
     */
    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K, V>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = Math.max(sc, c);
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n > MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    Node<K, V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                } else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }

    private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {

    }

    /**
     * 返回用于调整大小为 n 的表的印章位。当被 RESIZE_STAMP_SHIFT 左移时，必须是负数。
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * 在给定的索引处替换 bin 中的所有链接节点，除非表太小，在这种情况下，会调整大小。
     */
    private final void treeifyBin(Node<K, V>[] tab, int index) {
        Node<K, V> b;
        int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY) {

            } else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K, V> hd = null, tl = null;
                        for (Node<K, V> e = b; e != null; e = e.next) {
                            TreeNode<K, V> p = new TreeNode<>(e.hash, e.key, e.val, null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<>(hd));
                    }
                }
            }
        }
    }

    static <K, V> Node<K, V> untreeify(Node<K, V> b) {
        Node<K, V> hd = null, tl = null;
        for (Node<K, V> q = b; q != null; q = q.next) {
            Node<K, V> p = new Node<>(q.hash, q.key, q.val, null);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }
        return hd;
    }


    /**
     * 封装了诸如 containsValue 等方法的遍历；也可作为其他迭代器和分割器的基类。
     * 方法提前访问一次在迭代器构建时可到达的每个仍然有效的节点。
     * 它可能会遗漏一些在访问了 bin 之后被添加到 bin 的节点，这在一致性保证方面是没问题的。
     * 在可能的持续调整中保持这一属性需要相当数量的簿记状态，这在不稳定的访问中很难优化掉。
     * 即便如此，遍历仍能保持合理的吞吐量。通常情况下，迭代的过程是逐个 bin 遍历列表。
     * 然而，如果表已经被调整了大小，那么所有未来的步骤都必须在当前索引和（索引 + baseSize）处遍历 bin；
     * 以此类推，进一步调整大小。为了偏执地应对用户在线程间共享迭代器的可能性，如果读表时边界检查失败，迭代就会终止。
     */
    static class Traverser<K, V> {
        Node<K, V>[] tab;        // current table; updated if resized
        Node<K, V> next;         // the next entry to use
        TableStack<K, V> stack, spare; // to save/restore on ForwardingNodes
        int index;              // index of bin to use next
        int baseIndex;          // current index of initial table
        int baseLimit;          // index bound for initial table
        final int baseSize;     // initial table size

        Traverser(Node<K, V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        final Node<K, V> advance() {
            return null;
        }
    }

    /**
     * 记录表、其长度和当前的遍历索引，该遍历器必须在继续处理当前表之前处理转发表的一个区域。
     */
    static final class TableStack<K, V> {
        int length;
        int index;
        Node<K, V>[] tab;
        TableStack<K, V> next;
    }

    /**
     * 树状节点用在 bins 的头部。TreeBins 并不持有用户的键或值，而是指向 TreeNodes 的列表和它们的根。
     * 它们还保持着一个寄生的读写锁，迫使写作者（持有 bin 锁的人）在树形重组操作之前等待读者（没有的人）完成。
     */
    static final class TreeBin<K, V> extends Node<K, V> {
        TreeNode<K, V> root;
        TreeNode<K, V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        TreeBin(TreeNode<K, V> b) {
            super(TREEBIN, null, null, null);
        }

        final TreeNode<K, V> putTreeVal(int h, K k, V v) {
            return null;
        }
    }

    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> parent;  // red-black tree links
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K, V> next,
                 TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }
    }

    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset(k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset(k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset(k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset(k.getDeclaredField("cellsBusy"));
            CELLVALUE = U.objectFieldOffset(k.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}























