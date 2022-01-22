package com.future.concurrent.history;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 测试相关锁的性能对比
 */
@Slf4j
public class LocksBenchmark {
    static abstract class TestRunnable implements Runnable {
        private int count = 0;

        @Override
        public void run() {
            for (int j = 0; j < 1000000; j++) {
                lock();
                count++;
                unlock();
            }
        }

        abstract void lock();

        abstract void unlock();

        public int getCount() {
            return count;
        }
    }

    /**
     * 20 个线程，每个线程跑 100_0000 次累加
     * [DEBUG] 2022-01-11 15:33:16.284 com.future.concurrent.history.TestAndSetLock test 67 [main] begin start
     * [DEBUG] 2022-01-11 15:33:25.948 com.future.concurrent.history.TestAndSetLock test 75 [main] result: true, 20000000
     */
    static class TASTester extends TestRunnable {
        TestAndSetLock lock = new TestAndSetLock();

        @Override
        void lock() {
            lock.lock();
        }

        @Override
        void unlock() {
            lock.unlock();
        }
    }

    /**
     * 20 个线程，每个线程跑 100_0000 次累加
     * [DEBUG] 2022-01-11 15:42:17.921 com.future.concurrent.history.LocksBenchmark test 68 [main] begin start
     * [DEBUG] 2022-01-11 15:42:25.835 com.future.concurrent.history.LocksBenchmark test 76 [main] result: true, 20000000
     */
    static class TTASTester extends TestRunnable {
        TestTASLock lock = new TestTASLock();

        @Override
        void lock() {
            lock.lock();
        }

        @Override
        void unlock() {
            lock.unlock();
        }
    }

    static void test(int core, TestRunnable task) {
        log.debug("begin start");
        ExecutorService executorService = Executors.newFixedThreadPool(core);
        for (int i = 0; i < core; i++) {
            executorService.execute(task);
        }
        try {
            executorService.shutdown();
            boolean b = executorService.awaitTermination(1, TimeUnit.MINUTES);
            log.debug("result: {}, {}", b, task.getCount());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int core = 20;
//        test(core, new TASTester());
        test(core, new TTASTester());
    }
}
