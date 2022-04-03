package com.future.javap.dex;

import com.future.javap.IReader;
import com.future.util.PrintUtils;
import lombok.Getter;

@Getter
public class DexFileHeader {

    // "dex\n039\0"。 和 Java 的 CAFE BABE 不同，此处 dex\n 是 ASCII 码
    public static final byte[] DEX_FILE_MAGIC = {0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x39, 0x00};

    // 小端模式标记
    public static final int ENDIAN_CONSTANT = 0x12345678;

    // 大端模式标记
    public static final int REVERSE_ENDIAN_CONSTANT = 0x78563412;

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

    // link_size	uint
    private int linkSize;

    // link_off	uint
    private int linkOff;

    // map_off	uint
    private int mapOff;

    // string_ids_size	uint
    private int stringIdsSize;

    // string_ids_off	uint
    private int stringIdsOff;

    // type_ids_size	uint
    private int typeIdsSize;

    // type_ids_off	uint
    private int typeIdsOff;

    // proto_ids_size	uint
    private int protoIdsSize;

    // proto_ids_off	uint
    private int protoIdsOff;

    // field_ids_size	uint
    private int fieldIdsSize;

    // field_ids_off	uint
    private int fieldIdsOff;

    // method_ids_size	uint
    private int methodIdsSize;

    // method_ids_off	uint
    private int methodIdsOff;

    // class_defs_size	uint
    private int classDefsSize;

    // class_defs_off	uint
    private int classDefsOff;

    // data_size	uint
    private int dataSize;

    // data_off	uint
    private int dataOff;

    public DexFileHeader(IReader reader) {
        this.reader = reader;
    }

    public void parseHeader() {
        if (!isDexFile()) throw new DexFileFormatError("请检查文件头，魔数应为 dex\\n039\\0\"");
        this.checksum = reader.readUnsignedInt();
        this.signature = reader.readArray(20);
        this.fileSize = reader.readUnsignedInt();
        this.headerSize = reader.readUnsignedInt();
        this.endian_tag = reader.readUnsignedInt();
        this.linkSize = reader.readUnsignedInt();
        this.linkOff = reader.readUnsignedInt();
        this.mapOff = reader.readUnsignedInt();
        this.stringIdsSize = reader.readUnsignedInt();
        this.stringIdsOff = reader.readUnsignedInt();
        this.typeIdsSize = reader.readUnsignedInt();
        this.typeIdsOff = reader.readUnsignedInt();
        this.protoIdsSize = reader.readUnsignedInt();
        this.protoIdsOff = reader.readUnsignedInt();
        this.fieldIdsSize = reader.readUnsignedInt();
        this.fieldIdsOff = reader.readUnsignedInt();
        this.methodIdsSize = reader.readUnsignedInt();
        this.methodIdsOff = reader.readUnsignedInt();
        this.classDefsSize = reader.readUnsignedInt();
        this.classDefsOff = reader.readUnsignedInt();
        this.dataSize = reader.readUnsignedInt();
        this.dataOff = reader.readUnsignedInt();
    }

    public boolean isLittleEndian() {
        return ENDIAN_CONSTANT == this.endian_tag;
    }

    public boolean isBigEndian() {
        return REVERSE_ENDIAN_CONSTANT == this.endian_tag;
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
        return "dexVersion = 0x" + PrintUtils.toHexString(dexVersion) +
                ", checksum = " + checksum +
//                ", signature=" + Arrays.toString(signature) +
                ", fileSize = " + fileSize +
                ", headerSize = " + headerSize +
                ", endian_tag = 0x" + PrintUtils.toHexString(endian_tag) + "\n\t" +
                "linkSize = " + linkSize +
                ", linkOff = " + linkOff +
                ", mapOff = " + mapOff +
                ", stringIdsSize = " + stringIdsSize +
                ", stringIdsOff = " + stringIdsOff + "\n\t" +
                "typeIdsSize = " + typeIdsSize +
                ", typeIdsOff = " + typeIdsOff +
                ", protoIdsSize = " + protoIdsSize +
                ", protoIdsOff = " + protoIdsOff + "\n\t" +
                "fieldIdsSize = " + fieldIdsSize +
                ", fieldIdsOff = " + fieldIdsOff +
                ", methodIdsSize = " + methodIdsSize +
                ", methodIdsOff = " + methodIdsOff +
                ", classDefsSize = " + classDefsSize +
                ", classDefsOff = " + classDefsOff + "\n\t" +
                "dataSize = " + dataSize +
                ", dataOff = " + dataOff + "\n";
    }
}
