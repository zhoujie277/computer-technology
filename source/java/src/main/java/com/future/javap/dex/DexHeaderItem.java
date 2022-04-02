package com.future.javap.dex;

import com.future.javap.IReader;
import com.future.util.PrintUtils;

public class DexHeaderItem {

    // "dex\n039\0"。 和 Java 的 CAFE BABE 不同，此处 dex\n 是 ASCII 码
    private static final byte[] DEX_FILE_MAGIC = {0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x39, 0x00};

    private final IReader reader;

    // ubyte[8]
    private int dexVersion;
    // uint
    private int checksum;
    // unsigned byte[20]
    private byte[] signature;
    // uint
    private int fileSize;
    // header_size	uint = 0x70
    private int headerSize;
    // endian_tag	uint = ENDIAN_CONSTANT
    private int endian_tag;

    public DexHeaderItem(IReader reader) {
        this.reader = reader;
    }

    public void parseHeader() {
        if (!isDexFile()) throw new DexFileFormatError("请检查文件头，魔数应为 dex\\n039\\0\"");
        this.checksum = reader.readUnsignedInt();
        this.signature = reader.readArray(20);
        this.fileSize = reader.readUnsignedInt();
        this.headerSize = reader.readUnsignedInt();
        this.endian_tag = reader.readUnsignedInt();
    }

    private boolean isDexFile() {
        int bb;
        int now = 0, prev = 0;
        for (byte b : DEX_FILE_MAGIC) {
            if ((bb = reader.readUnsignedByte()) != b && (bb < 0x35 || bb > 0x39)) {
                System.out.println("bb=" + PrintUtils.toHexString(bb));
                return false;
            }
            prev = now;
            now = b;
        }
        dexVersion = prev;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("DexHeaderItem ");
        builder.append(", dexVersion=").append(PrintUtils.toHexString(dexVersion));
        return builder.toString();
    }
}
