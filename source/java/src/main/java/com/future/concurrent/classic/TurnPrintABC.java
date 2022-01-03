package com.future.concurrent.classic;

import java.util.concurrent.Semaphore;

class TurnPrintABC {

    private static final int COUNT = 20;
    private static Semaphore aSemaphore = new Semaphore(1);
    private static Semaphore bSemaphore = new Semaphore(0);
    private static Semaphore cSemaphore = new Semaphore(0);

    private static StringBuilder builder = new StringBuilder();
    private static void print(char c) {
        builder.append(c);
        if (c == 'C') {
            builder.append(' ');
        }
    }

    private static class A extends Thread {
        private int count = 0;
        @Override
        public void run() {
            while (count < COUNT) {
                try {
                    aSemaphore.acquire();
                    print('A');
                    count++;
                    bSemaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class B extends Thread {
        private int count = 0;

        @Override
        public void run() {
            while (count < COUNT) {
                try {
                    bSemaphore.acquire();
                    print('B');
                    count++;
                    cSemaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class C extends Thread {
        private int count = 0;

        @Override
        public void run() {
            while (count < COUNT) {
                try {
                    cSemaphore.acquire();
                    print('C');
                    count++;
                    aSemaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        A a = new A();
        B b = new B();
        C c = new C();
        a.start();
        b.start();
        c.start();
        a.join();
        b.join();
        c.join();
        System.out.println(builder.toString());
    }
    
}
