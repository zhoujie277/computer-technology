package com.future.concurrent.api;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Java 中 sun.misc.Unsafe 类的 api 演示
 * <p>
 * （1）实例化一个类；
 * <p>
 * （2）修改私有字段的值；
 * <p>
 * （3）抛出checked异常；
 * <p>
 * （4）使用堆外内存；
 * <p>
 * （5）CAS操作；
 * <p>
 * （6）阻塞/唤醒线程；
 *
 * @author future
 */
@Slf4j
class UnsafeApi {

    @SuppressWarnings("unused")
    @ToString
    static class Clazz {
        private int age;

        public int getAge() {
            return age;
        }
    }

    static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object o = theUnsafe.get(null);
            return (Unsafe) o;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 实例化一个类: unsafe.allocateInstance(Class class);
     * 获取一个类中成员相对于类对象在内存中起始地址的偏移: unsafe.objectFieldOffset(Field field);
     * 修改对象私有字段的值: unsafe.putInt/putLong/putXXX
     */
    void allocateInstance() {
        Unsafe unsafe = getUnsafe();
        try {
            // 实例化类
            assert unsafe != null;
            Object o = unsafe.allocateInstance(Clazz.class);
            Field age = Clazz.class.getDeclaredField("age");
            // 修改变量的值
            unsafe.putInt(o, unsafe.objectFieldOffset(age), 18);
            log.debug("{}", o);
        } catch (InstantiationException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * 无须在方法上使用 throws 关键字抛出 checked 异常。
     * 也不需要在方法体内 try/catch 异常。
     */
    @SuppressWarnings("unused")
    void throwCheckException() {
        Unsafe unsafe = getUnsafe();
        Objects.requireNonNull(unsafe).throwException(new IOException());
    }

    static class DirectIntArray {
        private static final int INT_SIZE = 4;
        private final long address;
        private final int size;
        private final Unsafe unsafe;

        public DirectIntArray(int size) {
            this.size = size;
            this.unsafe = getUnsafe();
            this.address = Objects.requireNonNull(unsafe).allocateMemory((long) size * INT_SIZE);
        }

        public void checkRange(int index) {
            if (index >= 0 && index < size) return;
            throw new IllegalArgumentException("index is illegal: " + index);
        }

        public void set(int index, int value) {
            checkRange(index);
            unsafe.putInt(address + (long) index * INT_SIZE, value);
        }

        public int get(int index) {
            checkRange(index);
            return unsafe.getInt(address + (long) index * INT_SIZE);
        }

        public int length() {
            return size;
        }

        public void freeMemory() {
            unsafe.freeMemory(address);
        }
    }

    void testDirectArray() {
        DirectIntArray array = new DirectIntArray(5);
        for (int i = 0; i < array.length(); i++) {
            array.set(i, i * 10 + 1);
        }

        for (int i = 0; i < array.length(); i++) {
            log.debug("array[{}] value is {}", i, array.get(i));
        }
        // manual free memory
        array.freeMemory();
    }

    @SuppressWarnings("all")
    static class AtomicInteger {
        private volatile int value;

        private static long offset;
        private static Unsafe unsafe;

        public AtomicInteger(int value) {
            this.value = value;
            unsafe = getUnsafe();
            try {
                offset = Objects.requireNonNull(unsafe).objectFieldOffset(AtomicInteger.class.getDeclaredField("value"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        public int incrementAndGet() {
            int expect, newValue;
            do {
                expect = value;
                newValue = expect + 1;
            } while (!unsafe.compareAndSwapInt(this, offset, expect, newValue));
            return value;
        }

        public int getValue() {
            return value;
        }
    }

    void testAtomicInteger() {
        AtomicInteger integer = new AtomicInteger(4);
        int get = integer.incrementAndGet();
        log.debug("after increment: {}", get);
    }

    void testBatchAtomicInteger() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        IntStream.range(0, 10).forEach(i -> threadPool.submit(() -> IntStream.range(0, 10000).forEach(j -> counter.incrementAndGet())));
        threadPool.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("counter final value is {}", counter.getValue());
    }

    static class LockSupport {
        static Unsafe sUnsafe;
        static {
             sUnsafe = getUnsafe();
        }
        void park() {
            sUnsafe.park(false, 0L);
        }
        void unPark(Thread t) {
            sUnsafe.unpark(t);
        }
    }

    public static void main(String[] args) {
        UnsafeApi api = new UnsafeApi();
        api.allocateInstance();
        api.testDirectArray();
        api.testAtomicInteger();
        api.testBatchAtomicInteger();
    }

}
