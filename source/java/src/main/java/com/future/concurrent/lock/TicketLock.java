package com.future.concurrent.lock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于票据的自旋锁
 * 虽然解决了公平性的问题。
 * 但是多处理器系统上，每个进程/线程占用的处理器都在读写同一个变量serviceNum ，
 * 每次读写操作都必须在多个处理器缓存之间进行缓存同步，
 * 这会导致繁重的系统总线和内存的流量，大大降低系统整体的性能
 *
 *
 * @author future
 */
public class TicketLock {

    private AtomicInteger serviceNo = new AtomicInteger();
    private AtomicInteger ticketNo = new AtomicInteger();

    /**
     * 先进入 lock 的取票号。
     * 释放的时候，将票号回收，同时将服务号累加。
     */
    public int lock() {
        int acquireTicket = ticketNo.getAndIncrement();
        //noinspection StatementWithEmptyBody
        while (serviceNo.get() != acquireTicket) {

        }
        return acquireTicket;
    }

    public void unlock(int tickNo) {
        int nextServiceNo = serviceNo.get() + 1;
        serviceNo.compareAndSet(tickNo, nextServiceNo);
    }
}
