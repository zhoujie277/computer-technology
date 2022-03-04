package com.future.concurrent.history.backoff;

import java.util.Random;

/**
 * 指数退避策略
 */
public class ExponentialBackOff implements BackOff {
    private int limit;
    private final int minDelay;
    private final int maxDelay;
    private final Random random = new Random();

    public ExponentialBackOff(int min, int max) {
        this.minDelay = min;
        this.maxDelay = max;
        this.limit = minDelay;
    }

    public void backOff() {
        int delay = random.nextInt(limit);
        limit = Math.min(maxDelay, limit + limit);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}