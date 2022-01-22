package com.future.concurrent.history;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 经典的互斥算法
 */
@SuppressWarnings("all")
class ClassicMutualExclusion {

    private MutualExclusion lock;

    interface MutualExclusion {
        void enterCriticalSection();

        void exitCriticalSection();
    }

    /**
     * 多线程竞争区域
     */
    void competingSection() {
        lock.enterCriticalSection();
        // ... do something
        lock.exitCriticalSection();
    }

    /**
     * 荷兰数学家 Dekker
     * 2 个线程互斥算法
     * 设：线程 A 的 id 为 0；线程 B 的 id 为 1.
     * 则有 the_other_thread.id = 1 - currentThread.id
     * <p>
     */
    static class PetersonAlgorithm implements MutualExclusion {
        boolean interest[] = new boolean[2];
        int turn = 0;

        /**
         * 经典的 双标志-先修改-后检查-后修改者等待算法。
         * 此算法可以实现互斥。
         * 满足"空闲让进，忙则等待"特性。
         * 两种情况。
         * 1. 没有发生竞争。则 interest[other] == false, 当前线程可以直接获取锁。
         * 2. 发生竞争。则后修改者等待。即 turn == current 成立。先修改者不会成立，可获得锁。
         */
        public void enterCriticalSection() {
            int current = ThreadID.get();
            // 此算法只考虑两个线程的互斥
            int other = 1 - current;
            interest[current] = true;
            turn = current;
            while (interest[other] == true && turn == current) {
                // wait...
            }
            // acquire lock, return
        }

        public void exitCriticalSection() {
            int current = (int) ThreadID.get();
            interest[current] = false;
        }
    }

    static class ThreadID {
        private static final AtomicInteger sequencer = new AtomicInteger();
        private static final ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return sequencer.getAndIncrement();
            }
        };

        public static int get() {
            Thread currentThread = Thread.currentThread();
            return threadId.get();
        }
    }

}
