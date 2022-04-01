package com.future.javap;

public class ConstantValueAttribute extends Attribute {

    public ConstantValueAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    private int constantValueIndex;

    @Override
    public void parse() {
        constantValueIndex = readUnsignedShort();
    }

    @Override
    public String toString() {
        return "ConstantValueAttribute {" + super.toString() +
                ", constantValueIndex = " + classFile.toString(constantValueIndex) +
                '}';
    }
}
