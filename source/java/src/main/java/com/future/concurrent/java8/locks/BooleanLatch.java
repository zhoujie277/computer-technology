package com.future.concurrent.java8.locks;


import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 这里有一个类似 CountDownLatch 的类，只是它只需要一个信号就可以触发。
 * 因为 latch 锁是非独占的，所以它使用共享的获取和释放方法。
 * <p>
 * 源自 AQS 注释文档
 */
@SuppressWarnings("unused")
public class BooleanLatch {

    private static class Sync extends AbstractQueuedSynchronizer {
        boolean isSignalled() {
            return getState() != 0;
        }

        @Override
        protected int tryAcquireShared(int arg) {
            return isSignalled() ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            setState(1);
            return true;
        }
    }

    private final Sync sync = new Sync();

    public boolean isSignalled() {
        return sync.isSignalled();
    }

    public void signal() {
        sync.releaseShared(1);
    }

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
}
