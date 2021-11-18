package com.future.thread;

import java.util.concurrent.Semaphore;

/**
 * 经典 IPC - 哲学家问题 <br/>
 * 思想上采用死锁预防，要么就拿一双筷子，要么就都不拿的方案，预防死锁的发生。<br />
 * 技术上采用记录哲学家状态，运用信号量的解决方案
 * 
 * @author zhoujie
 */
public class PhilosopherSem {

    private static final int STATE_THINK = 1;
    private static final int STATE_HUNGRY = 2;
    private static final int STATE_EATING = 3;

    private static class Table {
        private Semaphore mutex = new Semaphore(1);
        private Philosopher philosophers[] = null;

        public void init(Philosopher... args) {
            philosophers = new Philosopher[args.length];
            for (int i = 0; i < args.length; i++) {
                philosophers[i] = args[i];
            }
        }

        public void start() {
            for (int i = 0; i < philosophers.length; i++) {
                philosophers[i].start();
            }
        }

        private void tryTake(Philosopher p) {
            int left = (p.index - 1 + philosophers.length) % philosophers.length;
            int right = (p.index + 1) % philosophers.length;
            if (p.state == STATE_HUNGRY && !philosophers[left].isEating() && !philosophers[right].isEating()) {
                p.state = STATE_EATING;
                // 拿到了筷子，信号量加1，防止阻塞。
                p.obtainChopsticks();
            }
        }

        public boolean takeChopstick(Philosopher p) {
            try {
                mutex.acquire();
                p.state = STATE_HUNGRY;
                tryTake(p);
                mutex.release();
                // 如果没拿到筷子，就阻塞
                p.acquireChopsticks();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void putChopstick(Philosopher p) {
            try {
                mutex.acquire();
                p.state = STATE_THINK;
                int left = (p.index - 1 + philosophers.length) % philosophers.length;
                int right = (p.index + 1) % philosophers.length;
                tryTake(philosophers[left]);
                tryTake(philosophers[right]);
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Philosopher extends Thread {
        private int index = 0;
        private int state = STATE_THINK;
        private Semaphore semaphore = new Semaphore(0);
        private Table table = null;

        public Philosopher(Table table, int chopstickIndex) {
            this.table = table;
            this.index = chopstickIndex;
        }

        public void obtainChopsticks() {
            System.out.println("The number " + index + " is release...");
            semaphore.release();
        }

        public void acquireChopsticks() {
            try {
                System.out.println("The number " + index + " is acquire...");
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void think() {
            state = STATE_THINK;
            System.out.println("The number " + index + " is think...");
        }

        public void eat() {
            state = STATE_EATING;
            System.out.println("The number " + index + " is eating...");
        }

        public boolean isEating() {
            return state == STATE_EATING;
        }

        public void takeChopstick() {
            table.takeChopstick(this);
        }

        public void putChopstick() {
            System.out.println("The number " + index + " is over...");
            table.putChopstick(this);
        }

        @Override
        public void run() {
            think();
            takeChopstick();
            eat();
            putChopstick();
        }
    }

    public static void main(String[] args) {
        Table table = new Table();
        table.init(new Philosopher(table, 0), new Philosopher(table, 1), new Philosopher(table, 2),
                new Philosopher(table, 3), new Philosopher(table, 4));
        table.start();
    }
}
