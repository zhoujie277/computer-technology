package com.future.javap.dex;

import com.future.javap.IReader;
import com.future.javap.Javap;
import com.future.util.BitUtils;

import java.io.IOException;
import java.io.InputStream;

public class DexFileReader implements IReader {

    private int offset;
    private byte[] byteArray;

    public void loadDexFile(String path) throws IOException {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
        if (inputStream == null) throw new IllegalArgumentException("Dex file not found! path: " + path);
        this.byteArray = new byte[inputStream.available()];
        int dexSize = inputStream.read(byteArray);
        DexFile dex = new DexFile(this, dexSize);
        dex.parse();
        System.out.println(dex);
    }

    @Override
    public int readUnsignedShort() {
        int result = Javap.byteArrayToIntInLittleIndian(byteArray, offset, Javap.U2);
        offset += Javap.U2;
        return result;
    }

    @Override
    public int readUnsignedInt() {
        int result = Javap.byteArrayToIntInLittleIndian(byteArray, offset, Javap.U4);
        offset += Javap.U4;
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
        offset += data.length;
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

    @Override
    public int readUnsignedLEB128() {
        boolean hasNext;
        int shift = 0;
        int value = 0;
        do {
            int leb128 = readUnsignedByte();
            value += (BitUtils.lowSevenBit(leb128) << (7 * shift));
            int highBit = BitUtils.highBit(leb128);
            shift++;
            hasNext = highBit == 1;
        } while (hasNext);
        return value;
    }

    public static void main(String[] args) throws IOException {
        DexFileReader reader = new DexFileReader();
        reader.loadDexFile("classes3.dex");
    }
}
