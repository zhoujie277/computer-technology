package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

/**
 * Java 线程状态演示
 * 与操作系统线程经典五状态不同。
 * Java 的 Thread.State 枚举定义了 Java 中的六种状态。
 * 分别是 New, Runnable, Blocked, Timed_Waiting, Waiting, Terminated
 *
 * @author future
 */
@Slf4j
class ThreadStateAPI {

    public void printState() {

        // new
        Thread t1 = new Thread(()->{
            log.debug("running...");
        } , "t1");

        // terminated
        Thread t2 = new Thread(()->{
            log.debug("running...");
        } , "t2");
        t2.start();

        // runnable
        Thread t3 = new Thread(() -> {
            while (true){}
        }, "t3");
        t3.start();

        // timed-waiting
        Thread t4 = new Thread(() -> {
            synchronized (ThreadStateAPI.class) {
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t4");

        t4.start();

        // waiting
        Thread t5 = new Thread(() -> {
            try {
                t3.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t5");
        t5.start();

        // blocked
        Thread t6 = new Thread(() -> {
            synchronized (ThreadStateAPI.class) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t6");
        t6.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("t1 state {}", t1.getState());
        log.debug("t2 state {}", t2.getState());
        log.debug("t3 state {}", t3.getState());
        log.debug("t4 state {}", t4.getState());
        log.debug("t5 state {}", t5.getState());
        log.debug("t6 state {}", t6.getState());
    }

    public static void main(String[] args) {
        new ThreadStateAPI().printState();
    }
}
