package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * Park 和 打断标记演示
 *
 * @author future
 */
@Slf4j
class ParkAndInterrupted {

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(()-> {
            log.debug("park...");
            LockSupport.park();
            log.debug("un park...");

//            log.debug("线程状态：{}", Thread.currentThread().isInterrupted());
            // 清除 interrupt 标记，park 才会生效
            log.debug("线程状态：{}", Thread.interrupted());

            LockSupport.park();
            log.debug("un park...");
        }, "park");
        t.start();

        Thread.sleep(1);
        t.interrupt();
    }
}
