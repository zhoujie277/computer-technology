package com.future.concurrent.jmm;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;

@SuppressWarnings("all")
public class FinalExample {

    static final int constant = 10;

    long l1, l2, l3, l4, l5, l6, l7, l8;
    //    int xField;
    //    volatile  int xField;
    final int xField;
    long l11, l12, l13, l14, l15, l16, l17, l18;

    static Unsafe unsafe = null;

    public FinalExample(int x) throws Exception {
        this.xField = x;
        unsafe = getUnsafe();
    }

    static void changeXField(FinalExample instance, int value) throws Exception {
        Field xField = FinalExample.class.getDeclaredField("xField");
        xField.setAccessible(true);
        xField.set(instance, value);
    }

    static void changeXFieldNormal(FinalExample instance, int value) {
//        instance.xField = value;
    }

    static void changeXFieldUnsafe(FinalExample instance, int value) throws Exception {
        long offset = unsafe.objectFieldOffset(FinalExample.class.getDeclaredField("xField"));
//        unsafe.putInt(instance, offset, value);
//        unsafe.putOrderedInt(instance, offset, value);
        unsafe.putIntVolatile(instance, offset, value);
    }

    static Unsafe getUnsafe() throws Exception {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);
        return unsafe;
    }

    static void changeConstant(int value) throws Exception {
        Field xField = FinalExample.class.getDeclaredField("constant");
        xField.setAccessible(true);
        // will throw Exception
//        xField.set(null, value);
        long offset = unsafe.staticFieldOffset(xField);
        unsafe.putInt(FinalExample.class, offset, value);
        System.out.println("offset:" + offset + " after modify static final:" + constant);
    }

    static Random random = new Random();

    static class ReadThread extends Thread {
        FinalExample finalExample;

        ReadThread(FinalExample finalExample) {
            super("reader1");
            this.finalExample = finalExample;
        }

        @Override
        public void run() {
            while (finalExample.xField == 8) ;
            System.out.println("ReadThread end....");
        }
    }

    public static int[] randomArray(int len) {
        int[] a = new int[len];
        for (int i = 0; i < len; i++) {
            a[i] = random.nextInt(len + 10000);
        }
        return a;
    }

    private static void testSort() {
        int[] array = randomArray(20000);
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[j - 1] > array[j]) {
                    int a = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = a;
                }
            }
        }
    }

    /**
     * 结论：
     * 1. final 字段可以被反射修改
     * 2. static final 是编译时常量，一般情况下，运行时不可修改, 除了 System.in 和 System.out。
     */
    public static void main(String[] args) throws Exception {
        FinalExample finalExample = new FinalExample(8);
        changeConstant(20);
        new ReadThread(finalExample).start();
        testSort();
        testSort();
        testSort();
        testSort();
        testSort();

        changeXField(finalExample, 42);
//        changeXFieldNormal(finalExample, 36);
//        changeXFieldUnsafe(finalExample, 36);
    }

}
