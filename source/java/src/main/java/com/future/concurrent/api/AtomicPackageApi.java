package com.future.concurrent.api;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * java.util.concurrent.atomic 包下面的 Api 使用
 *
 * @author future
 */
@SuppressWarnings("unused")
@Slf4j
class AtomicPackageApi {

    static <T> void arrayDemo(Supplier<T> supplier, Function<T, Integer> lengthFunction,
                              BiConsumer<T, Integer> putConsumer, Consumer<T> printConsumer) {
        List<Thread> ts = new ArrayList<>();
        T array = supplier.get();
        Integer length = lengthFunction.apply(array);
        for (int i = 0; i < length; i++) {
            ts.add(new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    putConsumer.accept(array, j % length);
                }
            }));
        }

        ts.forEach(Thread::start);
        ts.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        printConsumer.accept(array);
    }

    static void testAtomicArray() {
        arrayDemo(() -> new int[10], array -> array.length, (array, index) -> array[index]++, array -> System.out.print(Arrays.toString(array)));
        System.out.println();
        arrayDemo(() -> new AtomicIntegerArray(10), AtomicIntegerArray::length, AtomicIntegerArray::getAndIncrement, System.out::print);
    }


    @ToString
    static class Student {
        volatile String name;
    }

    static void testFieldUpdater() {
        AtomicReferenceFieldUpdater<Student, String> updater = AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");
        Student student = new Student();
        boolean jack = updater.compareAndSet(student, null, "jack");
        log.debug("{}, update {}", student, jack);
    }

    /**
     * 测试累加器性能
     */
    static <T> void testAdderPerformance(Supplier<T> supplier, Consumer<T> consumer) {
        T adder = supplier.get();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ts.add(new Thread(() -> {
                for (int j = 0; j < 500000; j++) {
                    consumer.accept(adder);
                }
            }));
        }
        long start = System.currentTimeMillis();
        ts.forEach(Thread::start);
        ts.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.currentTimeMillis();
        log.debug("{} cost time {} ms", adder, end - start);
    }

    static void testAdder() {
        testAdderPerformance(AtomicLong::new, AtomicLong::getAndIncrement);
        testAdderPerformance(LongAdder::new, LongAdder::increment);
    }

    public static void main(String[] args) {
//        testAtomicArray();
//        testFieldUpdater();
        testAdder();
    }
}
