package com.future.javap;

public class ExceptionsAttribute extends Attribute {
    private int numOfExceptions;
    private int[] exceptionIndexTable;

    public ExceptionsAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    @Override
    public void parse() {
        numOfExceptions = readUnsignedShort();
        exceptionIndexTable = new int[numOfExceptions];
        for (int i = 0; i < numOfExceptions; i++) {
            exceptionIndexTable[i] = readUnsignedShort();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("ExceptionsAttribute ")
                .append("numOfExceptions = ").append(numOfExceptions);
        for (int j : exceptionIndexTable) {
            builder.append("\n\t\t\t").append(classFile.toString(j));
        }
        return builder.toString();
    }
}
