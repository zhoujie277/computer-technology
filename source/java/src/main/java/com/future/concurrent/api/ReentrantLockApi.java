package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 的几个特性。
 * 1. 可重入
 * 2. 可打断
 * 3. 可超时
 * 4. 可实现公平锁
 * 5. 多个条件队列
 */
@Slf4j
class ReentrantLockApi {
    ReentrantLock lock = new ReentrantLock();

    private void m2() {
        try {
            lock.lock();
            log.debug("m2");
        } finally {
            lock.unlock();
        }
    }

    public void reentrant() {
        try {
            lock.lock();
            log.debug("reentrant");
            m2();
        } finally {
            lock.unlock();
        }
    }

    public void interruptibly() {
        Thread t1 = new Thread(() -> {
            log.debug("t1 run");
            try {
                // 尝试获取锁
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.debug("没有获取到锁，返回");
                return;
            }

            try {
                log.debug("获取到锁");
            } finally {
                lock.unlock();
            }

        }, "t1");

        lock.lock();
        t1.start();

        try {
            TimeUnit.SECONDS.sleep(1);
            t1.interrupt();
            log.debug("执行打断");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void tryLock() {
        Thread t1 = new Thread(() -> {
            log.debug("t1 run");
            // 尝试获取锁
            try {
                if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                    log.debug("没有获取到锁，返回");
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                log.debug("获取到锁");
            } finally {
                lock.unlock();
            }

        }, "t1");

        lock.lock();
        t1.start();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void fairLock() {
        ReentrantLock lock = new ReentrantLock(true);
        lock.lock();
        for (int i = 0; i < 500; i++) {
            new Thread(() -> {
                lock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + " running...");
                } finally {
                    lock.unlock();
                }
            }, "t" + i).start();
        }
        // 1s 之后去争抢锁
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 强行插入，总是在最后输出
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " start...");
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " running...");
            } finally {
                lock.unlock();
            }
        }, "强行插入").start();
        lock.unlock();
    }

    public static void main(String[] args) {
        ReentrantLockApi api = new ReentrantLockApi();
        api.tryLock();
    }
}
