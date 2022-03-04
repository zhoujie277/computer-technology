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
 * 先进先出公平读写锁
 * 无饥饿问题。
 *
 * @author future
 */
@SuppressWarnings("all")
class FIFOReadWriterLck implements ReadWriteLock {

    private int readAcquires = 0;
    private int readReleases = 0;
    private boolean writer = false;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private Lock readLock = new ReadLock();
    private Lock writeLock = new WriteLock();

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    private class ReadLock implements Lock {

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
                readAcquires++;
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
                readReleases++;
                if (readReleases == readAcquires)
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

    private class WriteLock implements Lock {

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
                writer = true;
                while (readAcquires != readReleases) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
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
