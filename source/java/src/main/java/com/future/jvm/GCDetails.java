package com.future.jvm;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 可用 java -XX:+PrintCommandLineFlags -version 打印 JVM 采用的自动优化参数。
 * <p>
 * 例如，笔者本机打印参数输出如下。
 * -XX:G1ConcRefinementThreads=13 -XX:GCDrainStackTargetSize=64 -XX:InitialHeapSize=536870912 -XX:MaxHeapSize=8589934592
 * -XX:+PrintCommandLineFlags -XX:ReservedCodeCacheSize=251658240 -XX:+SegmentedCodeCache -XX:+UseCompressedClassPointers
 * -XX:+UseCompressedOops -XX:+UseG1GC
 *
 * @author future
 */
@Slf4j
@SuppressWarnings("unused")
class GCDetails {

    private final static long APPLICATION_TIME = 240_000;
    private final static float DELAY_FACTOR = 2f;
    private final static float SIZE_FACTOR = 15f;
    private final static float LIVE_FACTOR = 10;

    private static final int _16KB = 1 << 14;
    private static final int _128KB = 1 << 17;
    private static final int _512KB = 1 << 19;
    private static final int _1MB = 1 << 20;
    private static final int _2MB = 1 << 21;
    private static final int _3MB = 3 << 20;
    private static final int _4MB = 4 << 20;
    private static final int _5MB = 5 << 20;
    private static final int _6MB = 6 << 20;
    private static final int _7MB = 7 << 20;
    private static final int _8MB = 8 << 20;

    private static final int SMALL_ALIVE = 10;
    private static final int SMALLER_ALIVE = 100;
    private static final int MIDDLE_ALIVE = 800;
    private static final int BIG_ALIVE = 1500;
    private static final int GREAT_ALIVE = 3000;

    /**
     * -Xms16m -Xmx16m -Xmn8m -XX:+UseSerialGC -XX:+PrintGCDetails -verbose:gc
     * <p>
     * UseSerialGC: 使用串行 GC 收集器
     * UseParallelGC: 使用 Throughput 收集器
     * UseConcMarkSweepGC: 使用 CMS 收集器
     * <p>
     * 初始打印信息
     * <p>
     * Heap
     * *  def new generation   total 9216K, used 3340K [0x00000007bf000000, 0x00000007bfa00000, 0x00000007bfa00000)
     * *   eden space 8192K,  40% used [0x00000007bf000000, 0x00000007bf3431b8, 0x00000007bf800000)
     * *   from space 1024K,   0% used [0x00000007bf800000, 0x00000007bf800000, 0x00000007bf900000)
     * *   to   space 1024K,   0% used [0x00000007bf900000, 0x00000007bf900000, 0x00000007bfa00000)
     * *  tenured generation   total 6144K, used 0K [0x00000007bfa00000, 0x00000007c0000000, 0x00000007c0000000)
     * *    the space 6144K,   0% used [0x00000007bfa00000, 0x00000007bfa00000, 0x00000007bfa00200, 0x00000007c0000000)
     * *  Metaspace       used 3065K, capacity 4496K, committed 4864K, reserved 1056768K
     * *   class space    used 335K, capacity 388K, committed 512K, reserved 1048576K
     */
    public static void main(String[] args) throws Exception {
        DelayQueue<Data> queue = new DelayQueue<>();
        AllocateMemory allocateMemory = new AllocateMemory(queue);
        RecycleMemory recycleMemory = new RecycleMemory(queue);
        allocateMemory.start();
        recycleMemory.start();

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.schedule(() -> {
            allocateMemory.interrupt();
            recycleMemory.interrupt();
        }, APPLICATION_TIME, TimeUnit.MILLISECONDS);
        allocateMemory.join();
        recycleMemory.join();
        service.schedule(() -> {
        }, 10, TimeUnit.SECONDS);
        service.shutdown();
        boolean b = service.awaitTermination(3, TimeUnit.SECONDS);
        log.debug("will exit application after 3000...{}", b);
    }

    /**
     * 线程内 OOM 并不会导致主线程结束。
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static void onThreadOOM() {
        new Thread(() -> {
            List<byte[]> list = new ArrayList<>();
            list.add(new byte[_8MB]);
        }).start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ToString(onlyExplicitlyIncluded = true)
    static class Data implements Delayed {
        final byte[] content;
        @ToString.Include
        final int keepAlive;

        public Data(int bytes, int keepAlive) {
            content = new byte[bytes];
            this.keepAlive = keepAlive;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.toMillis(keepAlive);
        }

        @Override
        public int compareTo(Delayed o) {
            return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    static class AllocateMemory extends Thread {
        final Random random = new Random();
        final DelayQueue<Data> queue;

        public AllocateMemory(DelayQueue<Data> queue) {
            super("AllocateMemory");
            this.queue = queue;
        }

        /**
         * 四种级别：
         * 1. 小对象，生成频率高，生存时间短。
         * 2. 中等对象，生成频率适中，生存时间适中。
         * 3. 大对象，生成频率低，生存时间较长。
         */
        private Data getObject() {
            int i = random.nextInt(100);
            int size, time;
            if (i < 40) {
                size = _16KB;
                time = SMALL_ALIVE;
            } else if (i < 60) {
                size = _128KB;
                time = SMALLER_ALIVE;
            } else if (i < 75) {
                size = _512KB;
                time = MIDDLE_ALIVE;
            } else if (i < 90) {
                size = _1MB;
                time = BIG_ALIVE;
            } else {
                size = _3MB;
                time = GREAT_ALIVE;
            }
            size *= SIZE_FACTOR;
            time *= LIVE_FACTOR;
            return new Data(size, time);
        }

        @Override
        public void run() {
            int maxSleep;
            while (!Thread.interrupted()) {
                Data data = getObject();
                queue.put(data);
                maxSleep = 40;
                if (data.keepAlive == SMALLER_ALIVE) {
                    maxSleep = 50;
                } else if (data.keepAlive == MIDDLE_ALIVE) {
                    maxSleep = 100;
                } else if (data.keepAlive == BIG_ALIVE) {
                    maxSleep = 300;
                }
                maxSleep *= DELAY_FACTOR;
                int sleepTime = random.nextInt(maxSleep);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static class RecycleMemory extends Thread {
        final DelayQueue<Data> queue;

        public RecycleMemory(DelayQueue<Data> queue) {
            super("RecycleMemory");
            this.queue = queue;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Data data = queue.take();
//                    if (data.keepAlive > BIG_ALIVE)
//                        log.debug("{}", data);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
