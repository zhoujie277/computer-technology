package com.future.util;

public class PrintUtils {

    public static void printHex(byte[] bytes) {
        for (int i = 0; i < bytes.length; ) {
            System.out.print(toHexString(bytes[i] & 0xFF));
            System.out.print(" ");
            i++;
            if (i % 8 == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }

    public static String toHexString(int i) {
        String s = Integer.toHexString(i).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }
}
