package com.future.concurrent.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicIntegerArray;

@Slf4j
@SuppressWarnings("unused")
class FakePool {

    /**
     * 池化对象。
     * 例如数据库连接、大对象...
     */
    static class Pooled {

    }

    /**
     * 对象池设计几大因素
     * 1. 连接动态增长与收缩
     * 2. 连接保活
     * 3. 等待超时处理
     * 4. 分布式 hash
     * 5. 拒绝策略
     * 6. ...
     *
     * 可参照 Java 线程池设计
     *
     * 该示例是个简单的应用示例，展示高并发应用。
     */
    static class Pool<T> {
        private final int size;
        private final Pooled[] objects;

        private final AtomicIntegerArray states;

        Pool(int size) {
            this.size = size;
            this.objects = new Pooled[size];
            this.states = new AtomicIntegerArray(size);

            for (int i = 0; i < size; i++) {
                objects[i] = new Pooled();
                states.set(i, 0);
            }
        }

        public Pooled get() {
            for (; ; ) {
                for (int i = 0; i < size; i++) {
                    if (states.get(i) == 0 && states.compareAndSet(i, 0, 1)) {
                        return objects[i];
                    }
                }
                synchronized (this) {
                    try {
                        log.debug("wait...");
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void free(Pooled pooled) {
            for (int i = 0; i < size; i++) {
                if (objects[i] == pooled) {
                    states.set(i, 0);
                    synchronized (this) {
                        log.debug("all");
                        notifyAll();
                    }
                    break;
                }
            }
        }
    }
}
