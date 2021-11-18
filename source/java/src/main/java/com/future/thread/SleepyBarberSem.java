package com.future.thread;

import java.util.concurrent.Semaphore;

/**
 * 瞌睡理发师问题 理发店有一名理发师，一把理发椅，还有 N 把供等候理发的顾客坐的普通椅子。如果没有顾客到来，理发师就坐在理发椅上打瞌睡；
 * 当顾客到来时，就唤醒理发师；如果顾客到来时，理发师正在理发，顾客就坐下来等待；如果N把椅子都坐满了，顾客就离开该理发店去别处理发。<br/>
 * 分析：<br />
 * 该问题与典型的生产者-消费者问题不同。生产者-消费者解决的是有界产品模型问题，生产N个产品到缓冲区供消费者消费。<br />
 * 该理发店问题，明确顾客是线程，并且在理发师线程忙的时候，需要等待理发师线程的问题模型。<br/>
 * 另外，该问题还引出了缓冲区越界的拒绝策略思想。在该例中，体现的是越界鸵鸟策略。
 * 
 * @author zhoujie
 */
public class SleepyBarberSem {

    private static class BarberShop extends Thread {

        private static final int CHIARS = 5;
        private Semaphore customers = new Semaphore(0);
        // 需要的理发师资源数量，初始为0，表示瞌睡。
        private Semaphore barbers = new Semaphore(0);
        private Semaphore mutex = new Semaphore(1);
        private int waiterCount = 0;

        private void hairCut() {
            System.out.println("hair cutting...");
        }

        @Override
        public void run() {
            while (true) {
                try {
                    customers.acquire(); // 如果无顾客，理发师瞌睡等待
                    mutex.acquire();
                    waiterCount--; // 有顾客，则处理顾客。
                    barbers.release(); // 理发师处理完一个，需要的理发师数量减一。
                    mutex.release();
                    hairCut();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void addCustomer() {
            try {
                mutex.acquire();
                if (waiterCount < CHIARS) {
                    System.out.println("waitting hair cut...");
                    waiterCount++;
                    customers.release();
                    mutex.release();
                    barbers.acquire(); // 顾客需要的理发师数量加一，没有空闲理发师则等待。
                    // get_haircut 顾客获得理发服务
                } else {
                    mutex.release();
                    System.out.println("barberShop was full, new customer would leave barber shop...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Consumer extends Thread {
        private BarberShop shop = null;

        public Consumer(BarberShop shop) {
            this.shop = shop;
        }

        @Override
        public void run() {
            shop.addCustomer();
        }
    }

    public static void main(String[] args) {
        BarberShop shop = new BarberShop();
        shop.start();
        for (int i = 0; i < 10; i++) {
            new Consumer(shop).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
