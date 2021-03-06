package com.future.concurrent.history;

/**
 * 经典的互斥算法
 */
@SuppressWarnings("all")
class ClassicMutualExclusion {

    private MutualExclusion lock;

    interface MutualExclusion {
        void lock();

        void unlock();
    }

    /**
     * 多线程竞争区域
     */
    void competingSection() {
        lock.lock();
        // ... do something
        lock.unlock();
    }

    /**
     * 荷兰数学家 Dekker
     * 2 个线程互斥算法
     */
    static class DekkerAlgorithm implements MutualExclusion {

        volatile int turn = 0;
        boolean[] interest = new boolean[2];

        @Override
        public void lock() {
            int current = ThreadID.get();
            interest[current] = true;
            int other = 1 - current;
            while (interest[other]) {
                if (turn == other) {
                    interest[current] = false;
                    while (turn != current) ;
                    interest[current] = true;
                }
            }
        }

        @Override
        public void unlock() {
            int current = ThreadID.get();
            int other = 1 - current;
            interest[current] = false;
            turn = other;
        }
    }

    /**
     * 2 个线程互斥算法
     * 设：线程 A 的 id 为 0；线程 B 的 id 为 1.
     * 则有 the_other_thread.id = 1 - currentThread.id
     * <p>
     */
    static class PetersonAlgorithm implements MutualExclusion {
        volatile boolean interest[] = new boolean[2];
        volatile int turn = 0;

        /**
         * 经典的 双标志-先修改-后检查-后修改者等待算法。
         * 此算法可以实现互斥。
         * 满足"空闲让进，忙则等待"特性。
         * 两种情况。
         * 1. 没有发生竞争。则 interest[other] == false, 当前线程可以直接获取锁。
         * 2. 发生竞争。则后修改者等待。即 turn == current 成立。先修改者不会成立，可获得锁。
         */
        public void lock() {
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

        public void unlock() {
            int current = ThreadID.get();
            interest[current] = false;
        }
    }
}
