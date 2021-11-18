package com.future.guava.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Google guava 异步回调演示 主任务可以不用阻塞，监听即可。
 */
public class FutureCallbackAPI {

    private class WorkThread1 implements Callable<String> {

        @Override
        public String call() throws Exception {
            System.out.println("workThread1 is running...." + Thread.currentThread());
            Thread.sleep(500);
            return "String work1";
        }
    }

    private class WorkThread2 implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("workThread2 is running...." + Thread.currentThread());
            Thread.sleep(500);
            return "workThread2";
        }
    }

    private static class MainJob implements Runnable {

        volatile boolean work1Finished = false;
        volatile boolean work2Finished = false;

        @Override
        public void run() {
            int gap = 50;
            while (true) {
                System.out.println("main job is running..." + Thread.currentThread());
                try {
                    Thread.sleep(gap);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (work1Finished && work2Finished) {
                    System.out.println("Two work both finished........");
                    work2Finished = false;
                    gap = 500;
                }
            }
        }
    }

    public void run() {
        System.out.println("currentThread...." + Thread.currentThread());
        final MainJob mainjob = new MainJob();
        Thread mainThread = new Thread(mainjob);
        mainThread.setName("mainJob");
        mainThread.start();

        Callable<String> work1 = new WorkThread1();
        Callable<String> work2 = new WorkThread2();

        ExecutorService jPool = Executors.newFixedThreadPool(3);

        // 包裹 Java 线程池，创建 guava 线程池
        ListeningExecutorService gPool = MoreExecutors.listeningDecorator(jPool);

        // 提交烧水的业务逻辑，取到异步任务
        ListenableFuture<String> work1Future = gPool.submit(work1);

        ListenableFuture<String> work2Future = gPool.submit(work2);

        Futures.addCallback(work1Future, new FutureCallback<String>() {
            public void onSuccess(String arg0) {
                System.out.println("FutureCallback work1 was called. the param arg0 is " + arg0 + ", current Thread is "
                        + Thread.currentThread());
                mainjob.work1Finished = true;
            };

            @Override
            public void onFailure(Throwable arg0) {
                System.out.println("onFailure...");
                arg0.printStackTrace();
            }
        }, gPool);

        Futures.addCallback(work2Future, new FutureCallback<String>() {
            public void onSuccess(String result) {
                System.out.println("FutureCallback work2 was called. the param arg0 is " + result
                        + ", current Thread is " + Thread.currentThread());
                mainjob.work2Finished = true;
            };

            @Override
            public void onFailure(Throwable t) {
                System.out.println("onFailure...");
                t.printStackTrace();
            }
        }, gPool);
    }

    public static void main(String[] args) {
        new FutureCallbackAPI().run();
    }
}
