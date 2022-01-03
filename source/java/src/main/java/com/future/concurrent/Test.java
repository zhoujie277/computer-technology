package com.future.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@SuppressWarnings("unused")
class Test {

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

    public static void debugAQSAcquireQueued() {
        final ReentrantLock lock = new ReentrantLock();
        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }, "t1");
        t1.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }, "t2");
        t2.start();
    }

    public static void main(String[] args) {
//        testThreadPoolDaemon();
//        testInterrupted();
//        testExceptionInThread();
        debugAQSAcquireQueued();
    }
}
