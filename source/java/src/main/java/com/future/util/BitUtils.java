package com.future.util;

/**
 * 位操作工具类
 *
 * @author future
 */
public class BitUtils {

    public static int highBit(int value) {
        return (value & 0x80) >>> 7;
    }

    public static int lowSevenBit(int value) {
        return (value & 0x7F);
    }

    public static void main(String[] args) {
        System.out.println(lowSevenBit(255));
    }
}
