package com.future.concurrent.classic;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 交替输出模式
 *
 * @author future
 */
@Slf4j
@SuppressWarnings("unused")
class TurnPrint {

    static class WaitNotify {
        private int flag;
        private final int loopNumber;

        public WaitNotify(int initFlag, int loopNumber) {
            this.flag = initFlag;
            this.loopNumber = loopNumber;
        }

        void print(String str, int waitFlag, int nextFlag) {
            for (int i = 0; i < loopNumber; i++) {
                synchronized (this) {
                    while (waitFlag != flag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug(str);
                    flag = nextFlag;
                    notifyAll();
                }
            }
        }
    }

    public static void testWaitNotify() {
        WaitNotify wn = new WaitNotify(1, 5);
        new Thread(() -> wn.print("A", 1, 2), "t1").start();
        new Thread(() -> wn.print("B", 2, 3), "t2").start();
        new Thread(() -> wn.print("C", 3, 1), "t3").start();
    }

    static class AwaitSignal extends ReentrantLock {
        private final int loopNumber;

        AwaitSignal(int loopNumber) {
            this.loopNumber = loopNumber;
        }

        void print(String str, Condition current, Condition next) {
            for (int i = 0; i < loopNumber; i++) {
                lock();
                try {
                    try {
                        current.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.debug(str);
                    next.signal();
                } finally {
                    unlock();
                }
            }
        }
    }

    private static void testAwaitSignal() {
        AwaitSignal wn = new AwaitSignal(5);
        Condition a = wn.newCondition();
        Condition b = wn.newCondition();
        Condition c = wn.newCondition();

        new Thread(() -> wn.print("A", a, b), "t1").start();
        new Thread(() -> wn.print("B", b, c), "t2").start();
        new Thread(() -> wn.print("C", c, a), "t3").start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wn.lock();
        a.signal();
        wn.unlock();
    }

    static class ParkUnPack {
        private final int loopNumber;

        ParkUnPack(int loopNumber) {
            this.loopNumber = loopNumber;
        }

        void print(String str, Thread next) {
            for (int i = 0; i < loopNumber; i++) {
                LockSupport.park();
                log.debug(str);
                LockSupport.unpark(next);
            }
        }
    }

    static Thread t1, t2, t3;

    static void testParkUnPark() {
        ParkUnPack pup = new ParkUnPack(5);
        t1 = new Thread(() -> pup.print("A", t2), "t1");
        t2 = new Thread(() -> pup.print("B", t3), "t2");
        t3 = new Thread(() -> pup.print("C", t1), "t3");
        t1.start();
        t2.start();
        t3.start();
        LockSupport.unpark(t1);
    }

    public static void main(String[] args) {
//        testWaitNotify();
        testAwaitSignal();
    }
}
