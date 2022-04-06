package com.future.javap;

public interface IReader {

    int ORDER_LITTLE_INDIAN = 1;
    int ORDER_BIG_INDIAN = 2;

    int readUnsignedShort();

    int readUnsignedInt();

    int readUnsignedByte();

    byte[] readArray(int length);

    String readString(int len);

    default void seek(int offset) {

    }

    default int mark() {
        return 0;
    }

    default int readUnsignedLEB128() {
        return 0;
    }

    default void setByteOrder(int byteOrder) {

    }

    default int getByteOrder() {
        return ORDER_LITTLE_INDIAN;
    }

    default long readUnsignedLong() {
        return 0L;
    }
}
