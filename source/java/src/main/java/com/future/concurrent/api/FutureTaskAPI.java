package com.future.concurrent.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class FutureTaskAPI {

    private class FutureCallbck implements Callable<String> {

        @Override
        public String call() throws Exception {
            return "called";
        }

    }

    private void run() {
        try {
            FutureTask<String> task = new FutureTask<>(new FutureCallbck());
            new Thread(task).start();
            String result = task.get();
            log.debug("result=" + result);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new FutureTaskAPI().run();
    }
}
