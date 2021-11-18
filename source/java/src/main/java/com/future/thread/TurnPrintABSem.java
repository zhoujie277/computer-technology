package com.future.thread;

import java.util.concurrent.Semaphore;

/**
 * 轮流输出A和B
 * 该方法使用信号量实现同步
 * 信号量本质上表示一种资源的当前可用数量
 * 
 * @author zhoujie
 */
public class TurnPrintABSem {

    private static final int COUNT = 50;

    private static Semaphore aMutex = new Semaphore(1);
    private static Semaphore bMutex = new Semaphore(0);

    private static void printA() {
        try {
            aMutex.acquire();
            System.out.print("A");
            bMutex.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printB() {
        try {
            bMutex.acquire();
            System.out.println("B");
            aMutex.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class PrintA extends Thread {
        private int count = 0;
        @Override
        public void run() {
            while (count < COUNT) {
                printA();
                count++;
            }
        }
    }

    private static class PrintB extends Thread {
        private int count = 0;

        @Override
        public void run() {
            while (count < COUNT) {
                printB();
                count++;
            }
        }
    }

    public static void main(String[] args) {
        PrintA a = new PrintA();
        PrintB b = new PrintB();
        a.start();
        b.start();
    }
}
