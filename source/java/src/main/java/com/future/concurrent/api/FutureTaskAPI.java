package com.future.concurrent.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class FutureTaskAPI {

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
            System.out.println("result=" + result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new FutureTaskAPI().run();
    }
}
