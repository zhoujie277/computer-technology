package com.future.concurrent.fakelib.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongBinaryOperator;
import java.util.stream.IntStream;

public class Striped64 extends Number {
    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    static final int NCPU = Runtime.getRuntime().availableProcessors();

    transient volatile Cell[] cells;

    transient volatile long base;

    transient volatile int cellsBusy;

    private static final sun.misc.Unsafe UNSAFE;
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final long PROBE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            UNSAFE = (Unsafe) theUnsafe.get(null);
            BASE = UNSAFE.objectFieldOffset(Striped64.class.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset(Striped64.class.getDeclaredField("cellsBusy"));
            PROBE = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    Striped64() {

    }

    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapObject(this, BASE, cmp, val);
    }

    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    static int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * 累加器思想
     * <p>
     * 1. 将累加操作分摊到多个核上线程。
     * 2. 尽量 一个核更新一个 Cell
     * 3. 如果没有竞争，只更新 base。
     * 4. 如果在 casBase 上已经出现竞争，尝试创建 Cells，
     * 5. 如果 Cells 已被创建，则直接尝试在对应的 Cells 上进行更新。
     * 6. 线程映射到 Cell 的逻辑是用一个属于线程随机值 hash 到对应的Cell： getProbe() & m；
     * 7. 如果多个核上的线程映射到同一个 Cell，则需要将该线程 rehash
     * 8. 使用 cellsBusy 来实现创建、更新、扩容 Cells 数组的临界区，已达到原子性（互斥和同步）的效果，使用 volatile 保证可见性和有序性。
     */
    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUnContented) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUnContented = true;
        }
        // collide 表示是否有多个核在同一个 cell 上出现竞争。
        boolean collide = false;
        for (; ; ) {
            Cell[] as;
            Cell a;
            int n;
            long v;
            // cells 如果已经创建过
            if ((as = cells) != null && (n = as.length) > 0) {
                // 如果该线程对应的 cell 为空
                // 第一个 if 和第二个 if 已经囊括了绝大部分情况，
                // 以 LongAdder.add(x) 为例。
                // 假设 3 个甚至更多线程甚至同时竞争一个没有对应 cell 的 Cells 上时
                // 只会有一个线程 创建 Cell 成功，其他线程可能进入 其他 if 情况。
                if ((a = cells[h & (n - 1)]) == null) {
                    if (cellsBusy == 0) {
                        Cell r = new Cell(x);
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {
                                Cell[] rs;
                                int m, j;
                                // casCellsBusy only protect a thread enter critical section.
                                // so double check
                                if ((rs = cells) != null && (m = rs.length) > 0 && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created) break;
                            continue;
                        }
                    }
                    collide = false;
                } else if (!wasUnContented) {
                    // 如果是因为 wasUnContented 导致的 rehash，只做一次。
                    wasUnContented = true;
                } else if (a.cas(v = a.value, (fn == null) ? v + x : fn.applyAsLong(v, x))) {
                    // 更新成功则退出循环
                    break;
                } else if (n >= NCPU || cells != as) {
                    // cells 上限不能超过物理 CPU 核数。
                    collide = false;
                } else if (!collide) {
                    // 能进入这个 if，说明前面的 cas 操作失败了，出现了竞争。
                    // 虽然调用前 uncontented 为 false。
                    // 但是在该方法内部，cas value 失败了，出现了多线程竞争同一个 cell。
                    // 只做一次 rehash，下一次则仍竞争失败，则尝试扩容。
                    collide = true;
                } else if (cellsBusy == 0 && casCellsBusy()) {
                    // 嫩进入这个 if，说明该线程对应的 cell 存在，而且已经出现了剧烈竞争，至少 3 个线程。
                    // 故需要扩容。此处是扩容逻辑。
                    try {
                        if (cells == as) {
                            Cell[] rs = new Cell[n << 1];
                            IntStream.range(0, n).forEach(i -> rs[i] = as[i]);
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;
                }
                // rehash。如下情况需要进行 rehash 操作
                // 已经出现多个线程竞争同一个 cell
                // 1. 多个线程同时试图创建 cell,并且 casCellsBusy 失败时.
                // 2. wasContented == false
                // 3. collide 为 true
                // 4. Cells 数量达到了上限: 大于等于过 CPU 核心数的二次方

                h = advanceProbe(h);

            } else if (cellsBusy == 0 && cells == as && !casCellsBusy()) {
                // cells 没有创建过，则尝试创建 cells。
                // 更新成功之后，重置 cellsBusy。
                // 此时，另外的线程有可能又进来创建 cells。此时不能让其创建。
                // 故需要 double check: if (as == cells)
                boolean init = false;
                try {
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init) {
                    break;
                }
            } else if (casBase(v = base, (fn == null) ? v + x : fn.applyAsLong(v, x))) {
                // 若在创建 cells 的时机仍然出现了竞争，则将出现竞争的线程更新 base。
                // 更新成功，则退出。这时候更新 base 仍然出现了竞争，则会进入下一轮 for 循环。
                break;
            }
        }
    }

    @sun.misc.Contended
    static final class Cell {
        volatile long value;
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;

        Cell(long x) {
            value = x;
        }

        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                UNSAFE = (Unsafe) theUnsafe.get(null);
                valueOffset = UNSAFE.objectFieldOffset(Cell.class.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
}
