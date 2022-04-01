package com.future.javap;

public interface IReader {

    int readUnsignedShort();

    int readInt();

    int readByte();

    byte[] readArray(int length);

    String readString(int len);
}
