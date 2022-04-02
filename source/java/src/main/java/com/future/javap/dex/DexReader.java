package com.future.javap.dex;

import com.future.javap.IReader;
import com.future.javap.Javap;
import com.future.util.PrintUtils;

import java.io.IOException;
import java.io.InputStream;

public class DexReader implements IReader {

    private int offset;
    private byte[] byteArray;

    public void loadDexFile(String path) throws IOException {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
        if (inputStream == null) throw new IllegalArgumentException("Dex file not found! path: " + path);
        this.byteArray = new byte[inputStream.available()];
        int dexSize = inputStream.read(byteArray);
        PrintUtils.printHex(byteArray);
        DexFile dex = new DexFile(this, dexSize);
        dex.parseHeader();
        System.out.println(dex);
    }

    public static void main(String[] args) throws IOException {
        DexReader reader = new DexReader();
        reader.loadDexFile("classes3.dex");
    }

    @Override
    public int readUnsignedShort() {
        return 0;
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
}
