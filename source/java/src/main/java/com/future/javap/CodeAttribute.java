package com.future.javap;


import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CodeAttribute extends Attribute {
    public CodeAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    private int maxStack;
    private int maxLocals;
    private byte[] codes;
    private ExceptionTable[] exceptionTable;
    private Attributes attributes;

    @Override
    public void parse() {
        this.maxStack = readUnsignedShort();
        this.maxLocals = readUnsignedShort();
        int codeLength = readUnsignedInt();
        this.codes = readArray(codeLength);
        int exceptionTableLength = readUnsignedShort();
        this.exceptionTable = new ExceptionTable[exceptionTableLength];
        for (int i = 0; i < exceptionTableLength; i++) {
            exceptionTable[i] = new ExceptionTable();
        }
        attributes = Attributes.create(classFile, this);
    }

    /**
     * 异常表数据
     */
    public class ExceptionTable {

        public final int startPc;
        public final int endPc;
        public final int handlerPc;
        public final int catchType;

        public ExceptionTable() {
            this.startPc = readUnsignedShort();
            this.endPc = readUnsignedShort();
            this.handlerPc = readUnsignedShort();
            this.catchType = readUnsignedShort();
        }

        @Override
        public String toString() {
            return "{" +
                    "startPc=" + startPc +
                    ", endPc=" + endPc +
                    ", handlerPc=" + handlerPc +
                    ", catchType=" + catchType +
                    '}';
        }
    }

    public Iterator<Instruction> getInstructions() {
        return new InstructionIterator();
    }

    private class InstructionIterator implements Iterator<Instruction> {
        private int pc = 0;
        private Instruction next = new Instruction(codes, pc);

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Instruction next() {
            if (next == null)
                throw new NoSuchElementException();
            Instruction current = next;
            pc += current.length();
            next = (pc < codes.length ? new Instruction(codes, pc) : null);
            return current;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CodeAttribute ").append(super.toString())
                .append(", maxStack = ").append(maxStack)
                .append(", maxLocals = ").append(maxLocals)
                .append(", exceptionTableCount = ").append(exceptionTable.length)
                .append(", exceptionTable = ").append(Arrays.toString(exceptionTable))
                .append(", attributesCount = ").append(attributes.size())
                .append(", attributes: [");
        for (int i = 0; i < attributes.size(); i++) {
            builder.append("\n\t\t\t").append(attributes.get(i));
        }
        builder.append("\n\t\t\tcodes: [");
        Iterator<Instruction> instructions = getInstructions();
        while (instructions.hasNext()) {
            Instruction instruction = instructions.next();
            builder.append("\n\t\t\t\t").append(instruction.getPc()).append("\t")
                    .append(instruction.toString(classFile));
        }
        builder.append("\n\t\t\t]");
        builder.append("\n\t\t]");
        return builder.toString();
    }

}
