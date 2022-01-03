package com.future.concurrent.jmm;

import lombok.extern.slf4j.Slf4j;

/**
 * Java 内存模型之可见性问题
 */
@Slf4j
class VisibilityProblem extends Thread {

    /*volatile*/ boolean running = true;

    public void run() {
        long start = System.currentTimeMillis();
        while (running) {
            // ...
        }
        log.debug("{} ms", System.currentTimeMillis() - start);
    }

    void terminate() {
        running = false;
    }

    public static void main(String[] args) throws InterruptedException {
        VisibilityProblem visibilityProblem = new VisibilityProblem();
        visibilityProblem.start();
        Thread.sleep(100);
        // 线程并不会停下来
        visibilityProblem.terminate();
    }
}
