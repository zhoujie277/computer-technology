package com.future.concurrent.history;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 该算法演示指数退避策略的 TestTAS Lock
 */
@SuppressWarnings({"StatementWithEmptyBody", "unused", "all"})
class BackoffTASLock {

    // minDelay, maxDelay 需要根据具体的机器结构、核心数量经过反复测试确定。
    private final BackOff backOff = new ExponentialBackOff(1, 1000);
    private final AtomicBoolean state = new AtomicBoolean();

    public void lock() throws InterruptedException {
        while (true) {
            while (state.get()) ;
            if (!state.getAndSet(true)) {
                return;
            }
            backOff.backOff();
        }
    }

    public void unlock() {
        state.set(false);
    }

    private interface BackOff {
        void backOff() throws InterruptedException;
    }

    static class ExponentialBackOff implements BackOff {
        private int limit;
        private final int minDelay;
        private final int maxDelay;
        private final Random random = new Random();

        public ExponentialBackOff(int min, int max) {
            this.minDelay = min;
            this.maxDelay = max;
            this.limit = minDelay;
        }

        public void backOff() throws InterruptedException {
            int delay = random.nextInt(limit);
            limit = Math.min(maxDelay, limit + limit);
            Thread.sleep(delay);
        }
    }
}
