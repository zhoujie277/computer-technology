package com.future.concurrent.classic;

class Test {

    private volatile int loop = 1;

    public void put(int product) {
        while (loop == 1) {
            System.out.println("loop...");
        }
    }

    public void get() {
        loop = 0;
    }

}
