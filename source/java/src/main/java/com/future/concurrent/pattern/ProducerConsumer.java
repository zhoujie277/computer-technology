package com.future.concurrent.pattern;

import com.future.concurrent.looper.Message;

import java.util.LinkedList;

/**
 * 生产者-消费者模式
 *
 * @author future
 */
class ProducerConsumer {

    private final LinkedList<Integer> queue = new LinkedList<>();
    private final int capacity;

    public ProducerConsumer(int capacity) {
        this.capacity = capacity;
    }

    public void offer(Integer value) {
        synchronized (this) {
            while (queue.size() >= capacity) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(value);
            this.notifyAll();
        }
    }

    public Integer take() {
        synchronized (this) {
            while (queue.isEmpty()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.notifyAll();;
            return queue.removeFirst();
        }
    }

    public Integer take(long timeout) {
        long begin = System.currentTimeMillis();
        long passedTime = 0, waitTime = 0;
        synchronized (this) {
            while (queue.isEmpty()) {
                waitTime = timeout - passedTime;
                if (waitTime <= 0) break;
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                passedTime = System.currentTimeMillis() - begin;
            }
            this.notifyAll();
            return queue.removeFirst();
        }
    }
}
