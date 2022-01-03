package com.future.concurrent.fakelib.atomic;

import java.io.InvalidObjectException;
import java.io.Serializable;

/**
 * LongAdder
 */
//@SuppressWarnings("all")
class LongAdder extends Striped64 implements Serializable {

    private static final long serialVersionUID = 7249069246863182397L;

    public LongAdder() {
    }

    public void add(long x) {
        Cell[] as;
        long b, v;
        int m;
        Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            // uncontented 为 false， 表示多个线程更新同一个 Cell。
            // uncontented 为 true， 表示还没有多线程竞争同一个 Cell。
            boolean uncontented = true;
            if (as == null || (m = as.length - 1) < 0 || (a = as[getProbe() & m]) == null
                    || !(uncontented = a.cas(v = a.value, v + x))) {
                longAccumulate(x, null, uncontented);
            }
        }
    }


    public void increment() {
        add(1L);
    }

    public void decrement() {
        add(-1L);
    }

    public long sum() {
        Cell[] as = cells;
        long sum = base;
        if (as != null) {
            for (Cell cell : as) {
                if (cell != null) {
                    sum += cell.value;
                }
            }
        }
        return sum;
    }

    public void reset() {
        Cell[] as = cells;
        base = 0L;
        if (as != null) {
            for (Cell cell : as) {
                if (cell != null) {
                    cell.value = 0L;
                }
            }
        }
    }

    public long sumThenReset() {
        Cell[] as = cells;
        long sum = base;
        if (as != null) {
            for (Cell cell : as) {
                if (cell != null) {
                    sum += cell.value;
                    cell.value = 0L;
                }
            }
        }
        return sum;
    }

    @Override
    public String toString() {
        return Long.toString(sum());
    }

    @Override
    public long longValue() {
        return sum();
    }

    @Override
    public int intValue() {
        return (int) sum();
    }

    @Override
    public float floatValue() {
        return (float) sum();
    }

    @Override
    public double doubleValue() {
        return (double) sum();
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        private final long value;

        SerializationProxy(LongAdder adder) {
            value = adder.sum();
        }

        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }

    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(java.io.ObjectInputStream stream) throws InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }
}













