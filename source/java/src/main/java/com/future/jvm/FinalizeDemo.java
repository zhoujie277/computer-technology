package com.future.jvm;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * -XX:+PrintGCDetails
 * <p>
 * 当对象被标记为垃圾，即将被回收之前，JVM 会将重写了 finalize() 且没有被调用过的对象送入 Finalizer 的引用队列，
 * 由 Finalizer 线程从该队列获取该对象，并调用该对象的 finalize()，如果这次 finalize() 方法执行过后，
 * 该对象依然被判定为垃圾，则该对象会被真正回收。
 *
 * @author future
 */
@Slf4j
public class FinalizeDemo {

    static class Finalize {
        @Override
        protected void finalize() throws Throwable {
            // [DEBUG] 2022-03-13 21:42:38.721 com.future.jvm.FinalizeDemo finalize 16 [Finalizer] finalize() invoke.
            // 说明 finalize() 方法是在 Finalizer 线程调用的。
            log.debug("finalize() invoke.");
            super.finalize();
        }
    }

    void run() {
        Finalize obj = new Finalize();
        obj = null;
        System.gc();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FinalizeDemo demo = new FinalizeDemo();
        demo.run();
    }
}
