package com.future.concurrent.classic;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者-消费者问题
 * 
 * 
 * @author future
 */
@SuppressWarnings("all")
class ProducerConsumer {

    private static class Product {
        int a = 0;

        public Product(int p) {
            a = p;
        }
    }

    private static abstract class BoundedBuffer {
        protected Product[] products = null;
        protected int size = 0;

        public BoundedBuffer(int capacity) {
            products = new Product[capacity];
        }

        public abstract void put(Product product);

        public abstract Product get();
    }

    private static class BoundedBufferLock extends BoundedBuffer {
        private ReentrantLock lock = new ReentrantLock();
        private Condition empty = lock.newCondition();
        private Condition full = lock.newCondition();
        private int capacity = 0;

        public BoundedBufferLock(int capacity) {
            super(capacity);
            this.capacity = capacity;
        }

        @Override
        public void put(Product product) {
            try {
                lock.lock();
                while (size == capacity) {
                    full.await();
                }
                products[size++] = product;
                empty.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Product get() {
            Product tmp = null;
            try {
                lock.lock();
                while (size == 0) {
                    empty.await();
                }
                tmp = products[--size];
                full.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            return tmp;
        }
    }

    /**
     * 用管程实现的有界缓冲区
     */
    @SuppressWarnings("unused")
    private static class BoundedBufferMonitor extends BoundedBuffer {
        private Object lock = new Object();
        private int capacity = 0;

        public BoundedBufferMonitor(int capacity) {
            super(capacity);
            this.capacity = capacity;
        }

        public void put(Product product) {
            synchronized (lock) {
                while (size == capacity) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                products[size++] = product;
                lock.notifyAll();
            }
        }

        public Product get() {
            synchronized (lock) {
                while (size == 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lock.notifyAll();
                return products[--size];
            }
        }
    }

    /**
     * 用信号量实现的有界缓冲区
     */
    @SuppressWarnings("unused")
    private static class BoundedBufferSemaphore extends BoundedBuffer {
        // 有数据的缓冲区数目
        private Semaphore full = null;
        // 空闲缓冲区的数目
        private Semaphore empty = null;
        private Semaphore mutex = new Semaphore(1);

        public BoundedBufferSemaphore(int capacity) {
            super(capacity);
            full = new Semaphore(0);
            empty = new Semaphore(capacity);
        }

        public void put(Product product) {
            try {
                empty.acquire();
                mutex.acquire();
                products[size++] = product;
                mutex.release();
                full.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public Product get() {
            Product tmp = null;
            try {
                full.acquire();
                mutex.acquire();
                tmp = products[--size];
                mutex.release();
                empty.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return tmp;
        }
    }

    private static class Producer extends Thread {
        private BoundedBuffer buffer = null;
        private int base = 0;

        public Producer(BoundedBuffer buffer, int base) {
            this.buffer = buffer;
            this.base = base;
        }

        @Override
        public void run() {
            int i = base;
            int end = base + 100;
            while (i < end) {
                buffer.put(new Product(++i));
            }
        }
    }

    private static class Consumer extends Thread {
        private BoundedBuffer buffer = null;

        public Consumer(BoundedBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            int count = 0;
            while (true) {
                Product p = buffer.get();
                count++;
                System.out.print(p.a + " ");
                if (count % 50 == 0) {
                    System.out.println();
                }
            }
        }
    }

    private static void multiThreads(int producer, int consumer, BoundedBuffer buffer) {
        for (int i = 0; i < consumer; i++) {
            new Consumer(buffer).start();
        }
        for (int i = 0; i < producer; i++) {
            new Producer(buffer, i * 100).start();
        }
    }

    public static void main(String[] args) {
        // BoundedBuffer buffer = new BoundedBufferSemaphore(1);
        // BoundedBuffer buffer = new BoundedBufferMonitor(1);
        BoundedBuffer buffer = new BoundedBufferLock(1);
        multiThreads(3, 1, buffer);
    }

}
