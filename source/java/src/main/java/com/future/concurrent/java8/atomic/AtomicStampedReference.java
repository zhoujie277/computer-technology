package com.future.concurrent.java8.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * AtomicStampedReference 维护一个对象引用以及一个整数“stamp”，该整数可以进行原子更新。
 * 实现说明：此实现通过创建表示“装箱”[reference，integer]对的内部对象来维护标记引用。
 */
@SuppressWarnings("unused")
class AtomicStampedReference<V> {

    private static class Pair<T> {
        final T reference;
        final int stamp;

        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }

        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<>(reference, stamp);
        }
    }

    private volatile Pair<V> pair;

    public AtomicStampedReference(V initialRef, int initialStamp) {
        pair = Pair.of(initialRef, initialStamp);
    }

    public V getReference() {
        return pair.reference;
    }

    public int getStamp() {
        return pair.stamp;
    }

    public V get(int[] stampHolder) {
        Pair<V> pair = this.pair;
        stampHolder[0] = pair.stamp;
        return pair.reference;
    }

    public void set(V newReference, int newStamp) {
        Pair<V> current = pair;
        if (newReference != current.reference || newStamp != current.stamp)
            this.pair = Pair.of(newReference, newStamp);
    }

    public boolean compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp) {
        Pair<V> current = pair;
        return expectedReference == current.reference && expectedStamp == current.stamp
                && ((newReference == current.reference && newStamp == current.stamp)
                || casPair(current, Pair.of(newReference, newStamp)));
    }

    public boolean attemptStamp(V expectedReference, int newStamp) {
        Pair<V> current = pair;
        return expectedReference == current.reference && (newStamp == current.stamp
                || casPair(current, Pair.of(expectedReference, newStamp)));
    }

    private boolean casPair(Pair<V> expect, Pair<V> update) {
        return unsafe.compareAndSwapObject(this, pairOffset, expect, update);
    }

    private static final Unsafe unsafe;
    private static final long pairOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
            pairOffset = unsafe.objectFieldOffset(AtomicStampedReference.class.getDeclaredField("pair"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
