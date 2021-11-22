package com.future.util;

import java.util.concurrent.ThreadLocalRandom;

public class NumberUtil {

    private NumberUtil() {
    }

    /**
     * Generated [1, end]
     */
    public static int random(int end) {
        return ThreadLocalRandom.current().nextInt(end) + 1;
    }

    /**
     * Generated [start, end)
     */
    public static int random(int start, int end) {
        return ThreadLocalRandom.current().nextInt(end - start) + start;
    }
}
