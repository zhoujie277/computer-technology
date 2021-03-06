package com.future.javap;

public class Javap {

    public static final int U8 = 8;
    public static final int U4 = 4;
    public static final int U2 = 2;
    public static final int U1 = 1;

    // 大端序。低地址高字节。先读到的数据在高位。
    public static int byteArrayToIntInBigIndian(byte[] buf, int start, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result += ((buf[start + i] & 0xFF) << ((len - 1 - i) * 8));
        }
        return result;
    }

    // 小端序。低地址低字节。后面读到的数据在高位
    public static int byteArrayToIntInLittleIndian(byte[] buf, int start, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result += ((buf[start + i] & 0xFF) << (i * 8));
        }
        return result;
    }

    // 小端序。低地址低字节。后面读到的数据在高位
    public static long byteArrayToLongInLittleIndian(byte[] buf, int start, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            result += ((long) (buf[start + i] & 0xFF) << (i * 8));
        }
        return result;
    }

    // 大端序。低地址高字节。先读到的数据在高位。
    public static long byteArrayToLongInBigIndian(byte[] buf, int start, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            result += ((long) (buf[start + i] & 0xFF) << ((len - 1 - i) * 8));
        }
        return result;
    }
}
