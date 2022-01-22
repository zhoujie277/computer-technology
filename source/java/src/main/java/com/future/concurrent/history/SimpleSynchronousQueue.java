package com.future.concurrent.history;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 该算法出自
 * Nonblocking Concurrent Data Structures with Condition Synchronization
 * 和 2009 年 Doug lea Scalable Synchronous Queues.
 *
 * @author future
 */
//@SuppressWarnings("unused")
public class DualDataStructure {

    /**
     * 双栈结构
     */
    static class DualStack<E> {

        /**
         * 表示栈中结点
         */
        private static class SNode<E> {
            volatile SNode next;
            E item;
            int mode;

            static final Unsafe unsafe;
            static final long nextOffset;

            boolean casNext(SNode<E> cmp, SNode<E> update) {
                return unsafe.compareAndSwapObject(this, nextOffset, cmp, update);
            }

            static {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                    nextOffset = unsafe.objectFieldOffset(SNode.class.getDeclaredField("next"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }

        static final int REQUEST = 0;
        static final int DATA = 1;
        static final int FULFILLING = 2;

        private volatile SNode head;

        void enqueue() {
            SNode<E> h = head;
            if (h == null || h.mode == DATA) {
                SNode<E> next = h.next;
            }
        }

        E dequeue() {
            return null;
        }


    }

    static class DualQueue {

    }
}
