package com.future.concurrent.history;

import com.future.concurrent.history.backoff.BackOff;
import com.future.concurrent.history.backoff.ExponentialBackOff;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 该算法演示指数退避策略的 TestTAS Lock
 */
@SuppressWarnings({"StatementWithEmptyBody", "unused"})
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

}
