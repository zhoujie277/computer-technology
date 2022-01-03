package com.future.concurrent.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 饥饿问题演示
 */
@Slf4j
public class Starvation {

    /**
     * 该示例演示了饥饿的问题
     * 1. 向线程池中提交了两个任务，且两个个任务都因为等待而没有结束。
     * 2. 因为每个任务是往负荷已满的线程池中又提交了新任务，而这新任务只有在线程池中线程空闲时才会运行。
     * 3. 解决办法是不同类型的任务提交到不同的线程中。特别是在任务中又需要提交任务时，需要小心。
     */
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            log.debug("begin1...");
            Future<String> future = service.submit(() -> {
                log.debug("submit1...");
                return "future1";
            });
            try {
                log.debug("wait future1 result:{} ", future.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("over1 ...");
        });

        service.execute(() -> {
            log.debug("begin2...");
            Future<String> future = service.submit(() -> {
                log.debug("submit2...");
                return "future2";
            });
            try {
                log.debug("wait future2 result:{} ", future.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("over2 ...");
        });

        log.debug("main thread over");
    }
}
