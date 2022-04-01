package com.future.javap;

public class Instruction {
    public enum Kind {
        /**
         * Opcode is not followed by any operands.
         */
        NO_OPERANDS(1),
        /**
         * Opcode is followed by a byte indicating a type.
         */
        ATYPE(2),
        /**
         * Opcode is followed by a 2-byte branch offset.
         */
        BRANCH(3),
        /**
         * Opcode is followed by a 4-byte branch offset.
         */
        BRANCH_W(5),
        /**
         * Opcode is followed by a signed byte value.
         */
        BYTE(2),
        /**
         * Opcode is followed by a 1-byte index into the constant pool.
         */
        CPREF(2),
        /**
         * Opcode is followed by a 2-byte index into the constant pool.
         */
        CPREF_W(3),
        /**
         * Opcode is followed by a 2-byte index into the constant pool,
         * an unsigned byte value.
         */
        CPREF_W_UBYTE(4),
        /**
         * Opcode is followed by a 2-byte index into the constant pool.,
         * an unsigned byte value, and a zero byte.
         */
        CPREF_W_UBYTE_ZERO(5),
        /**
         * Opcode is followed by variable number of operands, depending
         * on the instruction.
         */
        DYNAMIC(-1),
        /**
         * Opcode is followed by a 1-byte reference to a local variable.
         */
        LOCAL(2),
        /**
         * Opcode is followed by a 1-byte reference to a local variable,
         * and a signed byte value.
         */
        LOCAL_BYTE(3),
        /**
         * Opcode is followed by a signed short value.
         */
        SHORT(3),
        /**
         * Wide opcode is not followed by any operands.
         */
        WIDE_NO_OPERANDS(2),
        /**
         * Wide opcode is followed by a 2-byte index into the local variables array.
         */
        WIDE_LOCAL(4),
        /**
         * Wide opcode is followed by a 2-byte index into the constant pool.
         */
        WIDE_CPREF_W(4),
        /**
         * Wide opcode is followed by a 2-byte index into the constant pool,
         * and a signed short value.
         */
        WIDE_CPREF_W_SHORT(6),
        /**
         * Wide opcode is followed by a 2-byte reference to a local variable,
         * and a signed short value.
         */
        WIDE_LOCAL_SHORT(6),
        /**
         * Opcode was not recognized.
         */
        UNKNOWN(1);

        Kind(int length) {
            this.length = length;
        }

        /**
         * The length, in bytes, of this kind of instruction, or -1 is the
         * length depends on the specific instruction.
         */
        public final int length;
    }

    /**
     * Get a 2-byte value, relative to the start of this instruction.
     */
    public int getShort(int offset) {
        return (getByte(offset) << 8) | getUnsignedByte(offset + 1);
    }

    /**
     * Get a unsigned 2-byte value, relative to the start of this instruction.
     */
    public int getUnsignedShort(int offset) {
        return getShort(offset) & 0xFFFF;
    }

    /**
     * Get a 4-byte value, relative to the start of this instruction.
     */
    public int getInt(int offset) {
        return (getShort(offset) << 16) | (getUnsignedShort(offset + 2));
    }

    /**
     * Get a byte value, relative to the start of this instruction.
     */
    public int getByte(int offset) {
        return bytes[pc + offset];
    }

    /**
     * Get an unsigned byte value, relative to the start of this instruction.
     */
    public int getUnsignedByte(int offset) {
        return getByte(offset) & 0xff;
    }

    private static int align(int n) {
        return (n + 3) & ~3;
    }

    public Opcode getOpcode() {
        int b = getUnsignedByte(0);
        return Opcode.get(b);
    }

    /**
     * Get the mnemonic for this instruction, or a default string if the
     * instruction is unrecognized.
     */
    public String getMnemonic() {
        Opcode opcode = getOpcode();
        if (opcode == null)
            return "bytecode " + getUnsignedByte(0);
        else
            return opcode.toString().toLowerCase();
    }

    public int length() {
        Opcode opcode = getOpcode();
        if (opcode == null)
            return 1;

        switch (opcode) {
            case TABLESWITCH: {
                int pad = align(pc + 1) - pc;
                int low = getInt(pad + 4);
                int high = getInt(pad + 8);
                return pad + 12 + 4 * (high - low + 1);
            }
            case LOOKUPSWITCH: {
                int pad = align(pc + 1) - pc;
                int npairs = getInt(pad + 4);
                return pad + 8 + 8 * npairs;
            }
            default:
                return opcode.kind.length;
        }
    }

    private final byte[] bytes;
    private final int pc;

    public Instruction(byte[] bytes, int pc) {
        this.bytes = bytes;
        this.pc = pc;
    }

    public int getPc() {
        return pc;
    }

    /**
     * Get the {@link Kind} of this instruction.
     */
    public Kind getKind() {
        Opcode opcode = getOpcode();
        return (opcode != null ? opcode.kind : Kind.UNKNOWN);
    }

    public String toString(ClassFile classFile) {
        StringBuilder builder = new StringBuilder();
        builder.append(getMnemonic()).append("\t");
        int index;
        switch (getKind()) {
            case CPREF:
                index = getUnsignedByte(1);
                break;
            case CPREF_W:
                index = getUnsignedShort(1);
                break;
            case BRANCH:
                index = getShort(1);
                break;
            case BRANCH_W:
                index = getInt(1);
                break;
            case NO_OPERANDS:
                index = 0;
                break;
            default:
                index = 0;
                builder.append(getKind());
        }
        if (index != 0) {
            builder.append("#").append(index).append("\t // ");
            builder.append(classFile.toString(index));
        }
        return builder.toString();
    }
}
