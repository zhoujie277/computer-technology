package com.future.thread;

/**
 * 两个线程轮流输出A和B。
 * 该算法没有采用信号量和管程来实现。
 * 一种纯软件的实现方式。
 * 
 * 值得一提的是，这种方法可以实现临界区，
 * 但是用这种方法实现的临界区会违反临界区的”空闲让进“原则。
 * 
 * @author zhoujie
 * 2021-10-20
 */
public class TurnPrintAB {

    private static class PrintA extends Thread {
        private int count = 0;
        @Override
        public void run() {
            while (count < COUNT) {
                while (turn != 'A');
                print('A');
                turn = 'B';
                count++;
            }
        }
    }

    private static class PrintB extends Thread {
        private int count = 0;
        @Override
        public void run() {
            while (count < COUNT) {
                while (turn != 'B');
                print('B');
                turn = 'A';
                count++;
            }
        }
    }

    private final static int COUNT = 50;

    // 保证可见性
    private static volatile char turn = 'A';

    private static void print(char c) {
        System.out.print(c);
        if (c == 'B') {
            System.out.print(" ");
        }
    }

    public static void main(String[] args) {
        PrintA a = new PrintA();
        PrintB b = new PrintB();
        a.start();
        b.start();
    }
}