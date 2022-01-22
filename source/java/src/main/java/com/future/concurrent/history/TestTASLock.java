package com.future.concurrent.history;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test-Test-And-Set 算法
 */
@SuppressWarnings({"StatementWithEmptyBody", "unused"})
public class TestTASLock {
    private final AtomicBoolean state = new AtomicBoolean(false);

    void lock() {
        while (true) {
            while (state.get()) ;
            if (!state.getAndSet(true))
                return;
        }
    }

    void unlock() {
        state.set(false);
    }
}
