package com.future.concurrent.history.artof;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 8 章 管程和阻塞同步
 * <p>
 * 简单的读写锁。
 * 读者优先。
 * 源源不断的读者将会导致写者线程饥饿。
 *
 * @author future
 */
@SuppressWarnings("all")
public class SimpleReadWriteLock implements ReadWriteLock {

    private int readers = 0;
    private boolean writer = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Lock readLock = new ReadLock();
    private final Lock writeLock = new WriteLock();

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    class ReadLock implements Lock {

        @Override
        public void lock() {
            lock.lock();
            try {
                while (writer) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                readers++;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {
            lock.lock();
            try {
                readers--;
                if (readers == 0) {
                    condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    class WriteLock implements Lock {

        @Override
        public void lock() {
            lock.lock();
            try {
                while (readers > 0 || writer) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                writer = true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {
            lock.lock();
            try {
                writer = false;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }
}
