package com.future.javap;

import java.util.ArrayList;
import java.util.List;

public class ConstantPool {

    private static final PlaceInfo placeInfo = new PlaceInfo(null);

    private int constantPoolCount;
    private final List<ConstantPoolInfo> constantPoolInfos = new ArrayList<>();

    public String getUTF8Value(int nameIndex) throws ConstantPoolException {
        ConstantPoolInfo poolInfo = constantPoolInfos.get(nameIndex);
        if (poolInfo instanceof Utf8Info) {
            return ((Utf8Info) poolInfo).content;
        }
        throw new ConstantPoolException("nameIndex:" + nameIndex);
    }

    public String toString(int nameIndex) {
        ConstantPoolInfo poolInfo = constantPoolInfos.get(nameIndex);
        if (poolInfo instanceof Utf8Info) {
            return "#" + nameIndex + " (" + ((Utf8Info) poolInfo).content + ")";
        }
        return poolInfo.toString();
    }

    public void parseConstantPool(SimpleClassReader reader) {
        // 常量池项从 1 开始计数。
        this.constantPoolCount = reader.readUnsignedShort();
        this.constantPoolInfos.add(placeInfo);
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = reader.readUnsignedByte();
            switch (tag) {
                case Utf8Info.tag:
                    this.constantPoolInfos.add(new Utf8Info(reader));
                    break;
                case LongInfo.tag:
                    parseLongInfo(reader);
                    // 由于历史原因，8 字节常量均占用两个表成员的空间。
                    this.constantPoolInfos.add(placeInfo);
                    i++;
                    break;
                case ClassInfo.tag:
                    this.constantPoolInfos.add(new ClassInfo(reader));
                    break;
                case StringInfo.tag:
                    this.constantPoolInfos.add(new StringInfo(reader));
                    break;
                case FieldRefInfo.tag:
                    this.constantPoolInfos.add(new FieldRefInfo(reader));
                    break;
                case MethodRefInfo.tag:
                    this.constantPoolInfos.add(new MethodRefInfo(reader));
                    break;
                case NameAndTypeInfo.tag:
                    this.constantPoolInfos.add(new NameAndTypeInfo(reader));
                    break;
                default:
                    System.out.println("un handle tag: " + tag);
                    break;
            }
        }
    }

    private void parseLongInfo(IReader reader) {
        this.constantPoolInfos.add(new LongInfo(reader));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("constantPoolCount=").append(constantPoolCount)
                .append(" (即事实上只有 ").append(constantPoolCount - 1).append(" 项常量)").append("\n");
        builder.append("ConstantPool: [").append("\n");
        for (int item = 0; item < constantPoolInfos.size(); item++) {
            ConstantPoolInfo constantPoolInfo = constantPoolInfos.get(item);
            // 常量池项索引从 1 开始
            builder.append("\t#").append(item).append(" = ").append(constantPoolInfo).append("\n");
        }
        builder.append("]").append("\n");
        return builder.toString();
    }

    static class ConstantPoolInfo {

        public ConstantPoolInfo(IReader reader) {
        }
    }

    static class PlaceInfo extends ConstantPoolInfo {
        public PlaceInfo(IReader reader) {
            super(reader);
        }

        @Override
        public String toString() {
            return "占位符";
        }
    }

    class ClassInfo extends ConstantPoolInfo {
        public static final int tag = 7;
        public final int nameIndex;

        public ClassInfo(IReader reader) {
            super(reader);
            this.nameIndex = reader.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "ClassInfo {" + ConstantPool.this.toString(nameIndex) + "}";
        }
    }

    class FieldRefInfo extends ConstantPoolInfo {
        public static final int tag = 9;
        public final int classInfoIndex;
        public final int nameAndTypeIndex;

        public FieldRefInfo(IReader reader) {
            super(reader);
            this.classInfoIndex = reader.readUnsignedShort();
            this.nameAndTypeIndex = reader.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "FieldRefInfo {" + ConstantPool.this.toString(classInfoIndex) +
                    ", " + ConstantPool.this.toString(nameAndTypeIndex) + "}";
        }
    }

    class MethodRefInfo extends ConstantPoolInfo {
        public static final int tag = 10;
        public final int classInfoIndex;
        public final int nameAndTypeIndex;

        public MethodRefInfo(IReader reader) {
            super(reader);
            this.classInfoIndex = reader.readUnsignedShort();
            this.nameAndTypeIndex = reader.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "MethodRefInfo {" + ConstantPool.this.toString(classInfoIndex) +
                    ", " + ConstantPool.this.toString(nameAndTypeIndex) + "}";
        }
    }

    class NameAndTypeInfo extends ConstantPoolInfo {
        public static final int tag = 12;
        public final int nameIndex;
        public final int descriptorIndex;

        public NameAndTypeInfo(IReader reader) {
            super(reader);
            this.nameIndex = reader.readUnsignedShort();
            this.descriptorIndex = reader.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "NameAndTypeInfo {" +
                    "nameIndex=" + ConstantPool.this.toString(nameIndex) +
                    ", descriptorIndex=" + ConstantPool.this.toString(descriptorIndex) +
                    '}';
        }
    }

    static class Utf8Info extends ConstantPoolInfo {
        public static final int tag = 1;
        public final int length;
        public final String content;

        public Utf8Info(IReader reader) {
            super(reader);
            this.length = reader.readUnsignedShort();
            this.content = reader.readString(length);
        }

        @Override
        public String toString() {
            return "Utf8Info {" +
                    "length=" + length +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    class StringInfo extends ConstantPoolInfo {
        public static final int tag = 8;

        private final int stringIndex;

        public StringInfo(IReader reader) {
            super(reader);
            this.stringIndex = reader.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "StringInfo {" +
                    ConstantPool.this.toString(stringIndex) +
                    "}";
        }
    }

    static class LongInfo extends ConstantPoolInfo {
        public static final int tag = 5;

        public final int highBytes;
        public final int lowBytes;

        public LongInfo(IReader reader) {
            super(reader);
            this.highBytes = reader.readUnsignedInt();
            this.lowBytes = reader.readUnsignedInt();
        }

        @Override
        public String toString() {
            return "LongInfo {" +
                    "highBytes=" + highBytes +
                    ", lowBytes=" + lowBytes +
                    '}';
        }
    }


}
