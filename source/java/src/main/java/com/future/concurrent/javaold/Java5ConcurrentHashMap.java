package com.future.concurrent.javaold;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * 一个哈希表，支持检索的完全并发性和更新的可调预期并发性。
 * This class obeys the same functional specification as java.util.Hashtable, and includes versions of methods corresponding to each method of Hashtable.
 * 然而，尽管所有的操作都是线程安全的，但检索操作并不涉及锁定，也不支持以阻止所有访问的方式锁定整个表。
 * 在依赖其线程安全但不依赖其同步细节的程序中，该类与 Hashtable 完全可互操作。
 * <p>
 * 检索操作（包括获取）一般不会阻塞，所以可能与更新操作（包括投放和删除）重叠。
 * 检索反映了最近完成的更新操作的结果，在其开始时保持。
 * 对于像 putAll 和 clear 这样的聚合操作，并发的检索可能只反映一些条目的插入或移除。
 * 同样，迭代器和枚举返回反映哈希表在迭代器/枚举创建时或创建后某一时刻的状态的元素。
 * 它们不会抛出 ConcurrentModificationException。 然而，迭代器被设计为一次只能由一个线程使用。
 * <p>
 * 更新操作中允许的并发性由可选的 concurrencyLevel 构造参数（默认为16）来指导，它被用作内部规模的提示。
 * 该表在内部进行分区，以尝试允许指定数量的并发更新而不发生争执。
 * 因为在哈希表中的放置基本上是随机的，实际的并发性会有所不同。
 * 理想情况下，你应该选择一个值来容纳尽可能多的线程来并发修改该表。
 * 使用一个明显高于你需要的值会浪费空间和时间，而一个明显较低的值会导致线程争用。
 * 但在一个数量级内的高估和低估通常不会有什么明显的影响。
 * 当知道只有一个线程会修改，而所有其他的线程只会读取时，一个 1 的值是合适的。
 * 此外，调整这个或其他类型的哈希表的大小是一个相对缓慢的操作，
 * 因此，在可能的情况下，提供预期表大小的估计在 构造函数中提供预期的表大小。
 */
@SuppressWarnings("rawtypes")
public class Java5ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    static int DEFAULT_INITIAL_CAPACITY = 16;

    static final int MAXIMUM_CAPACITY = 1 << 30;

    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    static final int DEFAULT_SEGMENTS = 16;

    static final int MAX_SEGMENTS = 1 << 16;

    static final int RETRIES_BEFORE_LOCK = 2;

    final int segmentMask;

    final int segmentShift;

    final Segment[] segments;

    Set<K> keySet;
    Set<Map.Entry<K, V>> entrySet;
    Collection<V> values;

    static int hash(Object x) {
        int h = x.hashCode();
        h += ~(h << 9);
        h ^= (h >>> 14);
        h += (h << 4);
        h ^= (h >>> 10);
        return h;
    }

    final Segment<K, V> segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    /**
     * 请注意，这永远不会被导出 输出为用户可见的 Map.Entry。
     * <p>
     * 因为值字段是 volatile 的，而不是 final 的，所以在 Java 内存模型中，
     * 当通过数据竞争读取时，非同步读者看到 null 而不是初始值，这是合法的。
     * 虽然导致这种情况的重新排序不太可能实际发生，
     * 但 Segment.readValueUnderLock 方法被用作备份，以防在非同步访问方法中看到一个空（预初始化）值。
     */
    static final class HashEntry<K, V> {
        final K key;
        final int hash;
        volatile V value;
        final HashEntry<K, V> next;

        HashEntry(K key, int hash, HashEntry<K, V> next, V value) {
            this.key = key;
            this.hash = hash;
            this.next = next;
            this.value = value;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class Segment<K, V> extends ReentrantLock {

        volatile int count;

        int modCount;

        int threshold;

        volatile HashEntry[] table;

        final float loadFactor;

        Segment(int initialCapacity, float lf) {
            loadFactor = lf;
            setTable(new HashEntry[initialCapacity]);
        }

        void setTable(HashEntry[] newTable) {
            threshold = (int) (newTable.length * loadFactor);
            table = newTable;
        }

        HashEntry<K, V> getFirst(int hash) {
            HashEntry[] tab = table;
            return tab[hash & (tab.length - 1)];
        }

        V readValueUnderLock(HashEntry<K, V> e) {
            lock();
            try {
                return e.value;
            } finally {
                unlock();
            }
        }

        V get(Object key, int hash) {
            if (count != 0) {
                HashEntry<K, V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        V v = e.value;
                        if (v != null)
                            return v;
                        return readValueUnderLock(e);
                    }
                    e = e.next;
                }
            }
            return null;
        }

        boolean containsKey(Object key, int hash) {
            if (count != 0) {
                HashEntry<K, V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key))
                        return true;
                    e = e.next;
                }
            }
            return false;
        }

        boolean containsValue(Object value) {
            if (count != 0) {
                HashEntry[] tab = table;
                int len = tab.length;
                for (HashEntry hashEntry : tab) {
                    for (HashEntry<K, V> e = hashEntry; e != null; e = e.next) {
                        V v = e.value;
                        if (v == null)
                            v = readValueUnderLock(e);
                        if (value.equals(v))
                            return true;
                    }
                }
            }
            return false;
        }

        boolean replace(K key, int hash, V oldValue, V newValue) {
            lock();
            try {
                HashEntry<K, V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                boolean replaced = false;
                if (e != null && oldValue.equals(e.value)) {
                    replaced = true;
                    e.value = newValue;
                }
                return replaced;
            } finally {
                unlock();
            }
        }

        V replace(K key, int hash, V newValue) {
            lock();
            try {
                HashEntry<K, V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                V oldValue = null;
                if (e != null) {
                    oldValue = e.value;
                    e.value = newValue;
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        V put(K key, int hash, V value, boolean onlyIfAbsent) {
            lock();
            try {
                int c = count;
                if (c++ > threshold)
                    rehash();
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<K, V> first = (HashEntry<K, V>) tab[index];
                HashEntry<K, V> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                V oldValue;
                if (e != null) {
                    oldValue = e.value;
                    if (!onlyIfAbsent)
                        e.value = value;
                } else {
                    oldValue = null;
                    ++modCount;
                    tab[index] = new HashEntry(key, hash, first, value);
                    count = c;
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        /**
         * 将每个列表中的节点重新分类为新的 Map。
         * 因为我们使用的是 2 次方扩展，每个 bin 中的元素必须保持在相同的索引，或者以 2 次方的偏移量移动。
         * 我们通过捕捉旧节点可以被重复使用的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。
         * 据统计，在默认的阈值下，当一个表增加一倍时，只有大约六分之一的节点需要克隆。
         * 只要它们不再被任何可能正在遍历表的读者线程所引用，它们所替换的节点就可以被垃圾回收。现在。
         */
        void rehash() {
            HashEntry[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity >= MAXIMUM_CAPACITY)
                return;
            HashEntry[] newTable = new HashEntry[oldCapacity << 1];
            threshold = (int) (newTable.length * loadFactor);
            int sizeMask = newTable.length - 1;
            for (HashEntry<K, V> e : oldTable) {
                if (e != null) {
                    HashEntry<K, V> next = e.next;
                    int idx = e.hash & sizeMask;

                    if (next == null)
                        newTable[idx] = e;
                    else {
                        HashEntry<K, V> lastRun = e;
                        int lastIdx = idx;
                        for (HashEntry<K, V> last = next; last != null; last = last.next) {
                            int k = last.hash & sizeMask;
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        newTable[lastIdx] = lastRun;

                        for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
                            int k = p.hash & sizeMask;
                            HashEntry<K, V> n = (HashEntry<K, V>) newTable[k];
                            newTable[k] = new HashEntry(p.key, p.hash, n, p.value);
                        }
                    }
                }
            }
            table = newTable;
        }

        V remove(Object key, int hash, Object value) {
            lock();
            try {
                int c = count - 1;
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<K, V> first = (HashEntry<K, V>) tab[index];
                HashEntry<K, V> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                V oldValue = null;
                if (e != null) {
                    V v = e.value;
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        ++modCount;
                        HashEntry<K, V> newFirst = e.next;
                        for (HashEntry<K, V> p = first; p != e; p = p.next)
                            newFirst = new HashEntry<>(p.key, p.hash, newFirst, p.value);
                        tab[index] = newFirst;
                        count = c;
                    }
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        void clear() {
            if (count != 0) {
                lock();
                try {
                    HashEntry[] tab = table;
                    for (int i = 0; i < tab.length; i++) {
                        tab[i] = null;
                    }
                    ++modCount;
                    count = 0;
                } finally {
                    unlock();
                }
            }
        }
    }

    public Java5ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (concurrencyLevel > MAX_SEGMENTS)
            concurrencyLevel = MAX_SEGMENTS;

        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        this.segments = new Segment[ssize];

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity)
            ++c;
        int cop = 1;
        while (cop < c)
            cop <<= 1;

        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment<K, V>(cop, loadFactor);
        }
    }

    public Java5ConcurrentHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
    }

    public Java5ConcurrentHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
    }

    public Java5ConcurrentHashMap(Map<? extends K, ? extends V> t) {
        this(Math.max((int) (t.size() / DEFAULT_LOAD_FACTOR) + 1, 11), DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
        putAll(t);
    }

    /**
     * 我们跟踪每个段的 modCounts，以避免 ABA 问题，
     * 即在遍历过程中，一个段的元素被添加，而另一个段的元素被删除，
     * 在这种情况下，表在任何时候都不会是空的。
     * 请注意，在 size() 和 containsValue() 方法中也使用了类似的 modCounts，这是唯一的其他方法，
     * 也容易受到 ABA 问题的影响。遇到 ABA 问题。
     */
    public boolean isEmpty() {
        final Segment[] segments = this.segments;
        int[] mc = new int[segments.length];
        int mcsum = 0;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].count != 0)
                return false;
            else
                mcsum += mc[i] = segments[i].modCount;
        }
        if (mcsum != 0) {
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].count != 0 || mc[i] != segments[i].modCount)
                    return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        final Segment[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];

        for (int k = 0; k < RETRIES_BEFORE_LOCK; k++) {
            check = 0;
            sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; i++) {
                sum += segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; i++) {
                    check += segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        check = -1;
                        break;
                    }
                }
            }
            if (check == sum)
                break;
        }

        if (check != sum) {
            sum = 0;
            for (Segment segment : segments) {
                segment.lock();
            }
            for (Segment segment : segments) {
                sum += segment.count;
            }
            for (Segment segment : segments) {
                segment.unlock();
            }
        }
        if (sum > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int) sum;
    }

    public V get(Object key) {
        int hash = hash(key);
        return segmentFor(hash).get(key, hash);
    }

    @Override
    public boolean containsKey(Object key) {
        int hash = hash(key);
        return segmentFor(hash).containsKey(key, hash);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();

        final Segment[] segments = this.segments;
        int[] mc = new int[segments.length];

        for (int k = 0; k < RETRIES_BEFORE_LOCK; k++) {
            int sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; i++) {
                int c = segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
                if (segments[i].containsValue(value))
                    return true;
            }
            boolean cleanSweep = true;
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; i++) {
                    int c = segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        cleanSweep = false;
                        break;
                    }
                }
            }
            if (cleanSweep)
                return false;
        }

        for (Segment item : segments) {
            item.lock();
        }
        boolean found = false;
        try {
            for (Segment segment : segments) {
                if (segment.containsValue(value)) {
                    found = true;
                    break;
                }
            }
        } finally {
            for (Segment segment : segments) {
                segment.unlock();
            }
        }
        return found;
    }

    @Override
    public V put(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, false);
    }

    public V putIfAbsent(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, true);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash, null);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return ConcurrentMap.super.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        ConcurrentMap.super.forEach(action);
    }

    @Override
    public boolean remove(Object key, Object value) {
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash, value) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (oldValue == null || newValue == null)
            throw new NullPointerException();
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, oldValue, newValue);
    }

    public V replace(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, value);
    }

    @Override
    public void clear() {
        for (Segment segment : segments) {
            segment.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        return keySet;
    }

    public static void main(String[] args) {
        Java5ConcurrentHashMap<Object, Object> map = new Java5ConcurrentHashMap<>();
        map.put("Hello", "world");
    }
}




























