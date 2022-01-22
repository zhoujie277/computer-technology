package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

@Slf4j
@SuppressWarnings("unused")
class ParkAndUnPark {

    static void testUnparkBeforePark() {
        Thread t1 = new Thread(()->{
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("t1 running...");
            LockSupport.park();
            log.debug("t1 resume park one");

            LockSupport.park();
            log.debug("t1 resume park two");
        }, "t1");
        log.debug("un park running...");
        // 在线程启动之前 un park 是无效的。
        t1.start();
        LockSupport.unpark(t1);
        LockSupport.unpark(t1);
        LockSupport.unpark(t1);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LockSupport.unpark(t1);

    }

    static void normal() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("will park");
            LockSupport.park();
            log.debug("resume");
        }, "t1");
        t1.start();
        Thread.sleep(3000);
        LockSupport.unpark(t1);
        log.debug("un park");
    }

    static void testParkInterrupted() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            log.debug("will park...");
            LockSupport.park();
            // Thread.interrupted() 会清空中断标记
            log.debug("resume isInterrupted: {}", Thread.interrupted());
            log.debug("resume isInterrupted: {}", Thread.currentThread().isInterrupted());
            // interrupt() 会设置打断标记
            Thread.currentThread().interrupt();
            log.debug("resume isInterrupted: {}", Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            log.debug("resume isInterrupted: {}", Thread.currentThread().isInterrupted());
        }, "t1");
        t1.start();
        log.debug("will interrupt t1 after 1s.");
        Thread.sleep(1000);
        t1.interrupt();
        log.debug("interrupt thread t1.");
    }


    public static void main(String[] args) throws InterruptedException {
//        testParkInterrupted();
        testUnparkBeforePark();
    }
}
