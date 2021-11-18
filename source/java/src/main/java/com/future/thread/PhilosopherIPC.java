package com.future.thread;

import java.security.Policy;
import java.util.concurrent.Semaphore;

/**
 * 经典 IPC - 哲学家问题 <br/>
 * 思想上采用死锁预防，要么就拿一双筷子，要么就都不拿的方案，预防死锁的发生。<br />
 * 技术上采用记录筷子的状态，运用信号量的解决方案
 * 
 * @author zhoujie
 */

@SuppressWarnings("unused")
public class PhilosopherIPC {

    private static class Philosopher extends Thread {
        private int index = 0;
        private Table table = null;

        public Philosopher(Table table, int number) {
            super("Philosopher-" + number);
            this.table = table;
            this.index = number;
        }

        private void eat() {
            System.out.println("The number " + index + " is eating...");
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            // e.printStackTrace();
            // }
        }

        public void think() {
            System.out.println("The number " + index + " is think...");
        }

        public void takeChopSticks() {
            try {
                table.takeChopSticks(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void putChopSticks() {
            System.out.println("The number " + index + " is over...");
            table.putChopSticks(index);
        }

        @Override
        public void run() {
            think();
            takeChopSticks();
            eat();
            putChopSticks();
        }
    }

    private static abstract class Table {
        protected Philosopher philosophers[];
        protected int capacity;

        public Table(int count) {
            capacity = count;
            philosophers = new Philosopher[count];
            for (int i = 0; i < capacity; i++) {
                philosophers[i] = new Philosopher(this, i);
            }
        }

        public abstract void takeChopSticks(int index) throws InterruptedException;

        public abstract void putChopSticks(int index);

        public void start() {
            for (int i = 0; i < capacity; i++) {
                philosophers[i].start();
            }
        }
    }

    /**
     * 可能会产生死锁的解法。<br/>
     * 大家都拿到了左手的筷子，不放手，然后都在等着右边的筷子。（占有并等待） 于是产生了死锁。
     */
    private static class DeadLockSample extends Table {
        private Semaphore chopSticks[] = { new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1),
                new Semaphore(1) };

        public DeadLockSample(int count) {
            super(count);
        }

        @Override
        public void takeChopSticks(int index) throws InterruptedException {
            chopSticks[index].acquire();
            Thread.sleep(1000);
            chopSticks[(index + 1) % capacity].acquire();
        }

        @Override
        public void putChopSticks(int index) {
            chopSticks[index].release();
            chopSticks[(index + 1) % capacity].release();
        }
    }

    /**
     * 采用有序分配资源，破坏环路条件，达到预防死锁的目的<br/>
     * 奇数索引先拿左边筷子，偶数索引先拿右边筷子。<br/>
     * 这个解法不会死锁的原因是：<br />
     * 1. 产生竞争的俩俩线程之间，都已经拿到了一根裤子，在第二根筷子的时候竞争，只要任何一个拿到了竞争资源，就能让自己最终执行完毕。<br/>
     * 2. 或者在两个线程在竞争第一根筷子时，另外一个线程，一根筷子也拿不到。
     */
    private static class OrderAllocResource extends Table {
        private Semaphore chopSticks[] = { new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1),
                new Semaphore(1) };

        public OrderAllocResource(int count) {
            super(count);
        }

        @Override
        public void takeChopSticks(int index) throws InterruptedException {
            if (index % 2 == 0) {
                chopSticks[index].acquire();
                Thread.sleep(1000);
                chopSticks[(index + 1) % capacity].acquire();
            } else {
                chopSticks[(index + 1) % capacity].acquire();
                Thread.sleep(1000);
                chopSticks[index].acquire();
            }
        }

        @Override
        public void putChopSticks(int index) {
            if (index % 2 == 0) {
                chopSticks[index].release();
                chopSticks[(index + 1) % capacity].release();
            } else {
                chopSticks[(index + 1) % capacity].release();
                chopSticks[index].release();
            }
        }
    }

    /**
     * 采用预先分配资源，破坏“占有并等待”条件，达到预防死锁的目的。
     */
    private static class PreAllocationResource extends Table {
        public PreAllocationResource(int count) {
            super(count);
        }

        private Semaphore mutex = new Semaphore(1);
        // 筷子可用
        private boolean chopSticks[] = { true, true, true, true, true };

        public void takeChopSticks(int index) {
            for (;;) {
                try {
                    int rightIndex = (index + 1) % chopSticks.length;
                    mutex.acquire();
                    if (chopSticks[index] && chopSticks[rightIndex]) {
                        chopSticks[index] = false;
                        chopSticks[rightIndex] = false;
                        mutex.release();
                        break;
                    }
                    mutex.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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

    public static void main(String[] args) {
        // Table table = new DeadLockSample(5);
        Table table = new OrderAllocResource(5);
        // Table table = new PreAllocationResource(5);
        table.start();
    }
}
