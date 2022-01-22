package com.future.concurrent.history;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Anderson 基于数组的锁
 * <p>
 * 假设 n 个线程，L 个锁。
 * 空间复杂度: O(Ln)
 */
@SuppressWarnings({"StatementWithEmptyBody", "unused"})
class ALock {
    private final ThreadLocal<Integer> mySlotIndex = ThreadLocal.withInitial(() -> 0);

    private final AtomicInteger tail;
    volatile boolean[] flag;
    int size;

    public ALock(int capacity) {
        size = capacity;
        tail = new AtomicInteger(0);
        flag = new boolean[capacity];
        flag[0] = true;
    }

    public void lock() {
        int slot = tail.getAndIncrement() % size;
        mySlotIndex.set(slot);
        while (!flag[slot]) ;
    }

    public void unlock() {
        int slot = mySlotIndex.get();
        flag[slot] = false;
        flag[(slot + 1) % size] = true;
    }
}
