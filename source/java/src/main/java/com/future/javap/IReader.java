package com.future.javap;

public interface IReader {

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
}
