package com.future.javap;

public class Javap {

    public static final int U4 = 4;
    public static final int U2 = 2;
    public static final int U1 = 1;

    // 大端序
    public static int byteArrayToIntInBigIndian(byte[] buf, int start, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result += ((buf[start + i] & 0xFF) << ((len - 1 - i) * 8));
        }
        return result;
    }

    // 小端序
    public static int byteArrayToIntInLittleIndian(byte[] buf, int start, int len) {
        int result = 0;
        for (int i = start; i < start + len; i++) {
            result += ((buf[i] & 0xFF) << (i * 8));
        }
        return result;
    }

}
