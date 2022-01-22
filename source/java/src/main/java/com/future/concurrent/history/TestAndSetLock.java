package com.future.concurrent.history;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test-and-set lock 算法
 */
@SuppressWarnings({"StatementWithEmptyBody", "unused"})
class TestAndSetLock {
    private final AtomicBoolean state = new AtomicBoolean(false);

    void lock() {
        while (state.getAndSet(true)) ;
    }

    void unlock() {
        state.set(false);
    }
}
