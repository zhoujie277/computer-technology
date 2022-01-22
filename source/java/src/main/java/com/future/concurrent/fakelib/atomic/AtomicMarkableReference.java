package com.future.concurrent.fakelib.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * AtomicMarkableReference 维护一个对象引用和一个标记位，该标记位可以进行原子更新。
 * 实现说明：此实现通过创建表示“装箱”[reference，boolean]对的内部对象来维护可标记引用。
 */
@SuppressWarnings("all")
public class AtomicMarkableReference<V> {

    private static class Pair<T> {
        final T reference;
        final boolean mark;

        private Pair(T reference, boolean mark) {
            this.reference = reference;
            this.mark = mark;
        }

        static <T> Pair<T> of(T reference, boolean mark) {
            return new Pair<>(reference, mark);
        }
    }

    private volatile Pair<V> pair;

    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        pair = Pair.of(initialRef, initialMark);
    }

    public V getReference() {
        return pair.reference;
    }

    public boolean isMarked() {
        return pair.mark;
    }

    public V get(boolean[] markHolder) {
        Pair<V> pair = this.pair;
        markHolder[0] = pair.mark;
        return pair.reference;
    }

    public void set(V newReference, boolean newMark) {
        Pair<V> current = pair;
        if (newReference != current.reference || newMark != current.mark)
            this.pair = Pair.of(newReference, newMark);
    }

    public boolean attempMark(V expectedReference, boolean newMark) {
        Pair<V> current = pair;
        return expectedReference == current.reference && (newMark == current.mark
                || casPair(current, Pair.of(expectedReference, newMark)));
    }

    public boolean compareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        Pair<V> current = pair;
        return expectedReference == current.reference && expectedMark == current.mark
                && ((newReference == current.reference) && newMark == current.mark)
                || casPair(current, Pair.of(newReference, newMark));
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
            pairOffset = unsafe.objectFieldOffset(AtomicMarkableReference.class.getDeclaredField("pair"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
