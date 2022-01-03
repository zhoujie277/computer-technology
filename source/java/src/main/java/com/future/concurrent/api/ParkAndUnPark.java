package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

@Slf4j
public class ParkAndUnPark {

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("will park");
            LockSupport.park();
            log.debug("resume");
        }, "t1");
        t1.start();
        Thread.sleep(3000);
        LockSupport.unpark(t1);
        log.debug("un park");
    }
}
