package com.future.concurrent.history;

import java.util.concurrent.atomic.AtomicInteger;

class ThreadID {
    private static final AtomicInteger sequencer = new AtomicInteger();
    private static final ThreadLocal<Integer> threadId = ThreadLocal.withInitial(sequencer::getAndIncrement);

    public static int get() {
        return threadId.get();
    }

    public static int getCluster() {
        // 线程所在集群 id
        return 0;
    }
}
