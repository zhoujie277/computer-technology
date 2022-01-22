package com.future.concurrent.lock;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 自旋锁 演示
 *
 * 自旋锁的性能仍受困于一个根本难题，即，自旋本身就有很大的开销：
 * 每一个尝试获取锁的操作都需要将包含锁的 cache line 传输到执行操作的CPU上。
 * 锁竞争严重的情形下，这个 cache line 会在自旋的 CPU 之间不断弹跳，导致性能严重下降。
 * 鉴于此，内核开发者为降低 cache line 弹跳所做的大量努力也就不足为奇了。
 *
 * 无论是简单的非公平自旋锁还是公平的基于排队的自旋锁，
 * 由于执行线程均在同一个 <b>共享变量</b>上自旋，申请和释放锁的时候必须对该共享变量进行修改，
 * 这将导致所有参与排队自旋锁操作的处理器的缓存变得无效。
 * 如果排队自旋锁竞争比较激烈的话，频繁的缓存同步操作会导致繁重的系统总线和内存的流量，
 * 从而大大降低了系统整体的性能。
 */
@SuppressWarnings("all")
public class SpinLock {

    static class NonFairSpinLock {
        AtomicReference<Thread> owner = new AtomicReference<>();

        void lock() {
            while (!owner.compareAndSet(null, Thread.currentThread())) {
                // busy waiting
            }
        }

        void unlock() {
            owner.compareAndSet(Thread.currentThread(), null);
        }
    }
}
