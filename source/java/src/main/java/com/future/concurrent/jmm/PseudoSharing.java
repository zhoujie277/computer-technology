package com.future.concurrent.jmm;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存行伪共享问题演示
 * <p>
 * CPU缓存中的数据是以缓存行为单位处理的；
 * 缓存行普遍大小是 64 字节。
 * <p>
 * Java8 中可使用 @sun.misc.Contended 注解 消除伪共享问题
 * <p>
 * （1）CPU具有多级缓存，越接近CPU的缓存越小也越快；
 * <p>
 * （2）CPU缓存中的数据是以缓存行为单位处理的；
 * <p>
 * （3）CPU缓存行能带来免费加载数据的好处，所以处理数组性能非常高；
 * <p>
 * （4）CPU缓存行也带来了弊端，多线程处理不相干的变量时会相互影响，也就是伪共享；
 * <p>
 * （5）避免伪共享的主要思路就是让不相干的变量不要出现在同一个缓存行中；
 * <p>
 * （6）一是每两个变量之间加七个 long 类型；
 * <p>
 * （7）二是创建自己的 long 类型，而不是用原生的；
 * <p>
 * （8）三是使用 java8 提供的注解；
 *
 * @author future
 */
@Slf4j
class PseudoSharing {

    interface SharingStruct {
        long incrementX();

        long incrementY();

        long getX();

        long getY();
    }

    /**
     * 默认共享结构体，连续的两个变量很有可能会被加载到同一个缓存行中，
     * 若该结构体的两个变量是在不同线程中更新的，则不应该被共享缓存。
     */
    static class DefaultStruct implements SharingStruct {
        volatile long x;
        volatile long y;

        @Override
        public long incrementX() {
            return ++x;
        }

        @Override
        public long incrementY() {
            return ++y;
        }

        @Override
        public long getX() {
            return x;
        }

        @Override
        public long getY() {
            return y;
        }
    }

    /**
     * 以下结构体使用了多个变量来分离 x，y，使其不共享一个缓存行。
     * 缓存行普遍大小是 64 字节；相当于 8 个 long 类型变量。
     */
    static class AloneStruct implements SharingStruct {
        volatile long x;
        long a, b, c, d, e, f, g;
        volatile long y;

        @Override
        public long incrementY() {
            return ++y;
        }

        @Override
        public long incrementX() {
            return ++x;
        }

        @Override
        public long getX() {
            return x;
        }

        @Override
        public long getY() {
            return y;
        }
    }

    /**
     * 使用 Java 伪共享注解，使其提供变量独立共享的能力。
     * 默认使用这个注解是无效的，需要在 JVM 启动参数加上 -XX:-RestrictContended
     */
    static class JavaStruct implements SharingStruct {
        @sun.misc.Contended
        volatile long x;
        @sun.misc.Contended
        volatile long y;

        @Override
        public long incrementX() {
            return ++x;
        }

        @Override
        public long incrementY() {
            return ++y;
        }

        @Override
        public long getX() {
            return x;
        }

        @Override
        public long getY() {
            return y;
        }
    }

    @sun.misc.Contended
    static class MyLong {
        volatile long value;
    }

    static class MyLongStruct implements SharingStruct {
        MyLong x = new MyLong();
        MyLong y = new MyLong();

        @Override
        public long incrementY() {
            return ++y.value;
        }

        @Override
        public long incrementX() {
            return ++x.value;
        }

        @Override
        public long getX() {
            return x.value;
        }

        @Override
        public long getY() {
            return y.value;
        }
    }

    void update(int loop, SharingStruct struct) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < loop; i++) {
                struct.incrementX();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            for (int y = 0; y < loop; y++) {
                struct.incrementY();
            }
        }, "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long end = System.currentTimeMillis();
        log.debug("cost time is {} ms", end - start);
        log.debug("struct.x = {}, struct.y = {} ", struct.getX(), struct.getY());
    }

    public static void main(String[] args) throws InterruptedException {
        PseudoSharing pseudoSharing = new PseudoSharing();
        // 耗时 4656 ms
        pseudoSharing.update(100000000, new DefaultStruct());
        // 耗时 656 ms
        pseudoSharing.update(100000000, new AloneStruct());
        pseudoSharing.update(100000000, new JavaStruct());
        pseudoSharing.update(100000000, new MyLongStruct());
    }
}
