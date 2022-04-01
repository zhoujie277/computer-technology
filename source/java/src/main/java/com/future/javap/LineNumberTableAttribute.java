package com.future.javap;

public class LineNumberTableAttribute extends Attribute {

    private Entry[] lineNumberTable;

    public LineNumberTableAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    @Override
    public void parse() {
        int lineNumberTableLength = readUnsignedShort();
        lineNumberTable = new Entry[lineNumberTableLength];
        for (int i = 0; i < lineNumberTableLength; i++) {
            lineNumberTable[i] = new Entry();
        }
    }

    private class Entry {
        int startPc;
        int lineNumber;

        public Entry() {
            this.startPc = readUnsignedShort();
            this.lineNumber = readUnsignedShort();
        }

        @Override
        public String toString() {
            return "line: " +
                    "startPc = " + startPc +
                    ", lineNumber = " + lineNumber;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("LineNumberTableAttribute ");
        builder.append(super.toString()).append(", Count: ").append(lineNumberTable.length);
        for (Entry entry : lineNumberTable) {
            builder.append("\n\t\t\t\t").append(entry);
        }
        return builder.toString();
    }
}
