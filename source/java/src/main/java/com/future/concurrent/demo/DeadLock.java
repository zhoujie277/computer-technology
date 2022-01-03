package com.future.concurrent.demo;

import lombok.extern.slf4j.Slf4j;

/**
 * 模拟死锁程序；
 * 可使用 JConsole 和 JStack 工具检测死锁
 */
@Slf4j
public class DeadLock {

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public void acquireOne() {
        synchronized (lock1) {
            log.debug("acquire lock1");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("wait lock2");
            synchronized (lock2) {
                log.debug("acquire lock2");
            }
        }
    }

    public void acquireTwo() {
        synchronized (lock2) {
            log.debug("acquire lock2");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("wait lock1");
            synchronized (lock1) {
                log.debug("acquire lock1");
            }
        }
    }

    public static void main(String[] args) {
        DeadLock deadLock = new DeadLock();
        new Thread(deadLock::acquireOne, "t1").start();
        new Thread(deadLock::acquireTwo, "t2").start();
    }
}
