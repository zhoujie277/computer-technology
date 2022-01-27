package com.future.concurrent.api;

import java.util.concurrent.Semaphore;

@SuppressWarnings("all")
class SemaphoreAPI {

    private void reentrant(Semaphore semaphore) throws InterruptedException {
        semaphore.acquire();
        System.out.println("reentrant");
        semaphore.release();
    }

    private void testReentrant() throws InterruptedException {
        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        System.out.println("before invoke reentrant");
        reentrant(semaphore);
        System.out.println("after invoke reentrant");
        semaphore.release();
    }

    public static void main(String[] args) throws InterruptedException {
        SemaphoreAPI api = new SemaphoreAPI();
        api.testReentrant();
    }

}
