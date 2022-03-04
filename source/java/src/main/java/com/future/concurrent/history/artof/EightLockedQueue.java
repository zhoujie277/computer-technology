package com.future.concurrent.history.artof;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 8 章 管程和阻塞同步
 * <p>
 *
 * @author future
 */
@SuppressWarnings({"all"})
class EightLockedQueue {
    static class LockedQueue<T> {
        final Lock lock = new ReentrantLock();
        final Condition notFull = lock.newCondition();
        final Condition notEmpty = lock.newCondition();
        final T[] items;
        int tail, head, count;

        public LockedQueue(int capacity) {
            items = (T[]) new Object[capacity];
        }

        public void enq(T x) {
            lock.lock();
            try {
                while (count == items.length) {
                    try {
                        notFull.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                items[tail++] = x;
                if (tail == items.length)
                    tail = 0;
                count++;
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        public T deq() {
            lock.lock();
            try {
                while (count == 0) {
                    try {
                        notEmpty.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                T result = items[head++];
                if (head == items.length)
                    head = 0;
                count--;
                notFull.signal();
                return result;
            } finally {
                lock.unlock();
            }
        }
    }
}
