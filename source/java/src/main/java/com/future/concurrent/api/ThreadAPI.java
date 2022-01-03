package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
class ThreadAPI {

    static class ThreadUnsafe {
        ArrayList<String> list = new ArrayList<>();

        public void method1(int loopNumber) {
            for (int i = 0; i < loopNumber; i++) { // { 临界区, 会产生竞态条件
                method2();
                method3();
            }
        }

        private void method2() {
            list.add("1");
        }

        private void method3() {
            list.remove(0);
        }
    }
    static final int THREAD_NUMBER = 1;
    static final int LOOP_NUMBER = 200;

    public static void main(String[] args) {
        ThreadUnsafe test = new ThreadUnsafe(); for (int i = 0; i < THREAD_NUMBER; i++) {
            new Thread(() -> test.method1(LOOP_NUMBER), "Thread" + i).start(); }
    }
}