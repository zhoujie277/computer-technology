package com.future.concurrent.api;

import org.checkerframework.checker.units.qual.K;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapAPI {
    private static final String VALUE = "world";
    private static int RESIZE_STAMP_BITS = 16;

    private static void test(AbstractMap<Integer, String> map) {
        for (int i = 1; i <= 12; i++) {
            map.put(i, VALUE);
        }
    }

    static int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    public static void main(String[] args) {
        test(new ConcurrentHashMap<>());
//        int n = 16;
//        System.out.println(Integer.numberOfLeadingZeros(n));
//        int resizeStamp = resizeStamp(n);
//        System.out.println(resizeStamp);
//        System.out.println(resizeStamp << RESIZE_STAMP_BITS);
    }
}
