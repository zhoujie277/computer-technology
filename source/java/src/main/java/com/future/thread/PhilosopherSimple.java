package com.future.thread;

import java.util.concurrent.Semaphore;

/**
 * 经典 IPC - 哲学家问题 <br/>
 * 思想上采用死锁预防，要么就拿一双筷子，要么就都不拿的方案，预防死锁的发生。<br />
 * 技术上采用记录筷子的状态，运用信号量的解决方案
 * @author zhoujie
 */
public class PhilosopherSimple {

    private static class Table {
        private Semaphore mutex = new Semaphore(1);
        // 筷子可用
        private boolean chopSticks[] = {true, true, true, true, true};

        public boolean takeChopSticks(int index) {
            try {
                int rightIndex = (index + 1) % chopSticks.length;
                mutex.acquire();
                if (chopSticks[index] && chopSticks[rightIndex]) {
                    chopSticks[index] = false;
                    chopSticks[rightIndex] = false;
                    mutex.release();
                    return true;
                }
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void putChopSticks(int index) {
            try {
                int rightIndex = (index + 1) % chopSticks.length;
                mutex.acquire();
                chopSticks[index] = true;
                chopSticks[rightIndex] = true;
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Philosopher extends Thread {
        private int index = 0;
        private Table table = null;

        public Philosopher(Table table, int number) {
            this.table = table;
            this.index = number;
        }

        private void eat() {
            System.out.println("The number " + index + " is eating...");
            // try {
            //     Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //     e.printStackTrace();
            // }
        }

        public void think() {
            System.out.println("The number " + index + " is think...");
        }

        public boolean takeChopSticks() {
            return table.takeChopSticks(index);
        }

        public void putChopSticks() {
            System.out.println("The number " + index + " is over...");
            table.putChopSticks(index);
        }

        @Override
        public void run() {
            think();
            while (!takeChopSticks());
            eat();
            putChopSticks();
        }
    }

    public static void main(String[] args) {
        Table table = new Table();
        for (int i = 0; i < 5; i++) {
            Philosopher p = new Philosopher(table, i);
            p.start();
        }
    }
}
