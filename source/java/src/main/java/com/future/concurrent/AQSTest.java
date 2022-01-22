package com.future.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@SuppressWarnings("all")
class AQSTest {

    public static void testThreadPoolDaemon() {
        ExecutorService service = Executors.newFixedThreadPool(3);
        service.execute(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("the thread of thread pool will down");
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        service.shutdown();
        log.debug("main thread will down");
    }

    public static void testInterrupted() {
        Thread t = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                // 抛出InterruptedException异常后，中断标示位会自动清除
                log.debug("was interrupted: {}", Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                log.debug("was interrupted: {}", Thread.currentThread().isInterrupted());
            }
        });
        t.start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        t.interrupt();
    }

    public static void testExceptionInThread() {
        Thread t = new Thread(() -> {
            boolean throwException = true;
            try {
                log.debug("try...");
                if (throwException)
                    throw new RuntimeException("helloException");
            } finally {
                log.debug("finally...");
            }
            log.debug("hello world");
        });
        t.start();
    }

    public static void debugAQSAcquireQueued() throws InterruptedException {
        final boolean backward = false;
        final ReentrantLock lock = new ReentrantLock();
        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                log.debug("t1 is running...");
                printlnAQSInfo(lock, backward);
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "t1");
        t1.start();

        Thread.sleep(1000);

        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                log.debug("t2 is running...");
                printlnAQSInfo(lock, backward);
            } finally {
                lock.unlock();
            }
        }, "t2");
        t2.start();

        Thread.sleep(500);
        log.debug("t2 was started");
        printlnAQSInfo(lock, backward);

        Thread t3 = new Thread(() -> {
            try {
                if (lock.tryLock(10, TimeUnit.SECONDS)) {
                    try {
                        log.debug("t3 is running...");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.debug("t3 was timeout...");
                    printlnAQSInfo(lock, backward);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.debug("t3 was interrupted...");
            }
        }, "t3");
        t3.start();

        Thread.sleep(500);
        log.debug("t3 was started");
        printlnAQSInfo(lock, backward);

        Thread t4 = new Thread(() -> {
            try {
                if (lock.tryLock(20, TimeUnit.SECONDS)) {
                    try {
                        log.debug("t4 is running...");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.debug("t4 was timeout...");
                    printlnAQSInfo(lock, backward);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t4");
        t4.start();

        Thread.sleep(500);
        log.debug("t4 was started");
        printlnAQSInfo(lock, backward);

        Thread t5 = new Thread(() -> {
            try {
                lock.lockInterruptibly();
                try {
                    log.debug("t5 is running...");
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                log.debug("t5 was unlock");
                printlnAQSInfo(lock, backward);
            }
        }, "t5");
        t5.start();

        Thread.sleep(500);
        log.debug("t5 was started");
        printlnAQSInfo(lock, backward);
    }

    private static void printlnAQSInfo(ReentrantLock lock, boolean backward) {
        log.debug("==================printlnAQSInfo===================");
        try {
            Field syncField = lock.getClass().getDeclaredField("sync");
            syncField.setAccessible(true);
            AbstractQueuedSynchronizer synchronizer = (AbstractQueuedSynchronizer) syncField.get(lock);
            Class<?> synchronizerClass = synchronizer.getClass().getSuperclass().getSuperclass();
            Field headField = synchronizerClass.getDeclaredField("head");
            Field tailField = synchronizerClass.getDeclaredField("tail");
            headField.setAccessible(true);
            tailField.setAccessible(true);
            Object head = headField.get(synchronizer);
            if (head == null) {
                log.debug("head is null");
                return;
            }
            Object tail = tailField.get(synchronizer);
            Field nextField = head.getClass().getDeclaredField("next");
            nextField.setAccessible(true);
            Field threadField = head.getClass().getDeclaredField("thread");
            threadField.setAccessible(true);
            Field prevField = head.getClass().getDeclaredField("prev");
            prevField.setAccessible(true);
            Field statusField = head.getClass().getDeclaredField("waitStatus");
            statusField.setAccessible(true);
            // 从头部向后遍历打印
            // 从尾部向前遍历打印
            Object node = backward ? head : tail;
            do {
                int nodeHash = node.hashCode();
                Thread thread = (Thread) threadField.get(node);
                Object status = statusField.get(node);
                Object prev = prevField.get(node);
                Object prevHash = prev == null ? null : prev.hashCode();
                Object next = nextField.get(node);
                Object nextHash = next == null ? null : next.hashCode();
                log.debug("node = {}, node.thread = {}, waitStatus = {}, prev = {}, next = {}", nodeHash, thread, status, prevHash, nextHash);
                node = backward ? next : prev;
            } while (node != null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            log.debug("==================End===================\n");
        }
    }

    public static void main(String[] args) throws InterruptedException {
//        testThreadPoolDaemon();
//        testInterrupted();
//        testExceptionInThread();
        debugAQSAcquireQueued();
    }
}
