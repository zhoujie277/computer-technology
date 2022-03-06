package com.future.concurrent.benchmark;

import com.future.concurrent.history.javaold.Java7ConcurrentLinkedQueue;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 *
 */
@Slf4j
class ConcurrentLinkedQueueFAQ {

    /**
     * Java7 中存在内存泄露，因为其 remove 和 add 操作都没有移动 head 结点，
     * 且没有删除任何结点，因为 Java7 中判断删除节点条件其实为 item != null && pred != null && next != null.
     * Java8 修复了这个问题，在 remove 时会删除其 item == null && pred != null && next != null 的结点。
     */
    void testMemoryLeak() {
//        ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
        Java7ConcurrentLinkedQueue<Object> queue = new Java7ConcurrentLinkedQueue<>();
        queue.add(new Object());
        Object object = new Object();
        int loops = 0;
//        TimeUnit.SECONDS.sleep(10);

        Stopwatch watch = Stopwatch.createStarted();
        while (loops < 100000) {
            if (loops % 10000 == 0 && loops != 0) {
                long elapsed = watch.stop().elapsed(TimeUnit.MILLISECONDS);
                log.debug("loops={}  duration={} ms,size={}", loops, elapsed, queue.size());
                watch.reset().start();
            }
            queue.add(object);
            queue.remove(object);
            ++loops;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedQueueFAQ faq = new ConcurrentLinkedQueueFAQ();
        faq.testMemoryLeak();
    }
}
