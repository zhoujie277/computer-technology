package com.future.javap;

public interface IReader {

    int readUnsignedShort();

    int readUnsignedInt();

    int readUnsignedByte();

    byte[] readArray(int length);

    String readString(int len);
}
