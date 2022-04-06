package com.future.javap.elf;

import com.future.javap.IReader;
import com.future.javap.Javap;

public class ByteArrayReader implements IReader {
    private int offset;

    private final byte[] byteArray;
    private int byteOrder = ORDER_LITTLE_INDIAN;

    public ByteArrayReader(byte[] buf) {
        this.byteArray = buf;
        offset = 0;
    }

    public ByteArrayReader(byte[] byteArray, int byteOrder) {
        this.byteArray = byteArray;
        this.byteOrder = byteOrder;
        this.offset = 0;
    }

    public ByteArrayReader(int length) {
        this.byteArray = new byte[length];
        offset = 0;
    }

    @Override
    public void setByteOrder(int byteOrder) {
        this.byteOrder = byteOrder;
    }

    @Override
    public int getByteOrder() {
        return byteOrder;
    }

    @Override
    public int readUnsignedShort() {
        int result;
        if (byteOrder == ORDER_LITTLE_INDIAN) {
            result = Javap.byteArrayToIntInLittleIndian(byteArray, offset, Javap.U2);
        } else {
            result = Javap.byteArrayToIntInBigIndian(byteArray, offset, Javap.U2);
        }
        offset += Javap.U2;
        return result;
    }

    @Override
    public int readUnsignedInt() {
        int result;
        if (byteOrder == ORDER_LITTLE_INDIAN) {
            result = Javap.byteArrayToIntInLittleIndian(byteArray, offset, Javap.U4);
        } else {
            result = Javap.byteArrayToIntInBigIndian(byteArray, offset, Javap.U4);
        }
        offset += Javap.U4;
        return result;
    }

    @Override
    public long readUnsignedLong() {
        long result;
        if (byteOrder == ORDER_LITTLE_INDIAN) {
            result = Javap.byteArrayToLongInLittleIndian(byteArray, offset, Javap.U8);
        } else {
            result = Javap.byteArrayToLongInBigIndian(byteArray, offset, Javap.U8);
        }
        offset += Javap.U8;
        return result;
    }

    @Override
    public int readUnsignedByte() {
        int b = byteArray[offset] & 0xFF;
        offset += Javap.U1;
        return b;
    }

    @Override
    public byte[] readArray(int length) {
        byte[] data = new byte[length];
        System.arraycopy(byteArray, offset, data, 0, data.length);
        offset += length;
        return data;
    }

    @Override
    public String readString(int len) {
        return null;
    }

    @Override
    public void seek(int offset) {
        this.offset = offset;
    }

    @Override
    public int mark() {
        return offset;
    }
}
