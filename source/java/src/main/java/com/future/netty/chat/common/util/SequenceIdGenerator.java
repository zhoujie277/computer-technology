package com.future.netty.chat.common.util;

import java.util.concurrent.atomic.AtomicLong;

public abstract class SequenceIdGenerator {
    private static final AtomicLong id = new AtomicLong();

    private SequenceIdGenerator() {
    }

    // 递增 和 获取
    public static long nextId() {
        return id.incrementAndGet();
    }
}
