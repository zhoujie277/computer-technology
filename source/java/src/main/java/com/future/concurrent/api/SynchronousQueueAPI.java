package com.future.concurrent.api;


import java.util.concurrent.SynchronousQueue;

public class SynchronousQueueAPI {

    private void test() {
        SynchronousQueue<String> queue =  new SynchronousQueue<>();
        // offer will return immediately by cancel waiting
        boolean hello = queue.offer("hello");
        System.out.println(hello);
    }

    public static void main(String[] args) {
        SynchronousQueueAPI api = new SynchronousQueueAPI();
        api.test();
    }
}
