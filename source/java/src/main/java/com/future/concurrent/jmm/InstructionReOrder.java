package com.future.concurrent.jmm;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 指令重排序
 *
 * @author future
 */
@Slf4j
@ToString
class InstructionReOrder {

    int num;
    boolean ready = false;

    void actor1() {
        int result = 0;
        if (ready) {
            result = num + num;
        } else {
            result = 1;
        }
        if (result != 4) {
            // 输出1 是可见性问题，输出 0 是重排序问题。
            log.debug("result is {}", result);
        }
    }

    void actor2() {
        // 该处代码既可能出现重排序问题，也有可能出现可见性问题。
        num = 2;
        ready = true;
    }

    void startT1() {
        new Thread(this::actor2).start();
        new Thread(this::actor1).start();
    }

    public static void main(String[] args) {
        InstructionReOrder reOrder = null;
        int i = 0;
        for (; i < 100000; i++) {
            reOrder = new InstructionReOrder();
            reOrder.startT1();
        }
    }
}
