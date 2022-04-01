package com.future.javap;

public class LocalVariableTableAttribute extends Attribute {

    private Entry[] localVariableTable;

    public LocalVariableTableAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    @Override
    public void parse() {
        int localVariableTableLength = readUnsignedShort();
        localVariableTable = new Entry[localVariableTableLength];
        for (int i = 0; i < localVariableTableLength; i++) {
            localVariableTable[i] = new Entry();
        }
    }

    private class Entry {
        int startPc;
        int length;
        int nameIndex;
        int descriptorIndex;
        int index;

        public Entry() {
            this.startPc = readUnsignedShort();
            this.length = readUnsignedShort();
            this.nameIndex = readUnsignedShort();
            this.descriptorIndex = readUnsignedShort();
            this.index = readUnsignedShort();
        }

        @Override
        public String toString() {
            return "localVariable: " +
                    "startPc = " + startPc +
                    ", length = " + length +
                    ", index = " + index +
                    ", nameIndex = " + classFile.toString(nameIndex) +
                    ", descriptorIndex = " + classFile.toString(descriptorIndex);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("LocalVariableTableAttribute ");
        builder.append(super.toString()).append(", Count: ").append(localVariableTable.length);
        for (Entry entry : localVariableTable) {
            builder.append("\n\t\t\t\t").append(entry);
        }
        return builder.toString();
    }
}
