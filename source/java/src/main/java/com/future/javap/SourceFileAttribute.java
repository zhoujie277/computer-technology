package com.future.javap;


public class SourceFileAttribute extends Attribute {

    private int sourceFileIndex;

    public SourceFileAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    @Override
    public void parse() {
        this.sourceFileIndex = readUnsignedShort();
    }

    @Override
    public String toString() {
        return "SourceFileAttribute {" + classFile.toString(sourceFileIndex) +
                '}';
    }
}
