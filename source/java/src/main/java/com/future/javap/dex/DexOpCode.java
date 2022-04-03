package com.future.javap.dex;

public enum DexOpCode {

    NOP(0x00, DexInstructionFormatter.K10x),
    MOVE(0x01, DexInstructionFormatter.K12x),
    MOVE_FROM16(0x02, DexInstructionFormatter.K22x),
    MOVE_16(0x03, DexInstructionFormatter.K32x),
    MOVE_WIDE(0x04, DexInstructionFormatter.K12x),
    MOVE_WIDE_FROM16(0x05, DexInstructionFormatter.K22x),
    MOVE_WIDE_16(0x06, DexInstructionFormatter.K32x),

    MOVE_RESULT(0x0a, DexInstructionFormatter.K11x),
    MOVE_RESULT_WIDE(0x0b, DexInstructionFormatter.K11x),
    RETURN_VOID(0x0e, DexInstructionFormatter.K10x),

    CONST(0x14, DexInstructionFormatter.K31i),

    CONST_CLASS(0x1c, DexInstructionFormatter.K21c),

    // sstaticop vAA, field@BBBB
    SGET(0x60, DexInstructionFormatter.K21c),

    SGET_WIDE(0x61, DexInstructionFormatter.K21c),

    SGET_OBJECT(0x62, DexInstructionFormatter.K21c),

    SGET_BOOLEAN(0x63, DexInstructionFormatter.K21c),

    SGET_BYTE(0x64, DexInstructionFormatter.K21c),

    SGET_CHAR(0x65, DexInstructionFormatter.K21c),

    SGET_SHORT(0x66, DexInstructionFormatter.K21c),

    SPUT(0x67, DexInstructionFormatter.K21c),

    SPUT_WIDE(0x68, DexInstructionFormatter.K21c),

    SPUT_OBJECT(0x69, DexInstructionFormatter.K21c),

    SPUT_BOOLEAN(0x6a, DexInstructionFormatter.K21c),

    SPUT_BYTE(0x6b, DexInstructionFormatter.K21c),

    SPUT_CHAR(0x6c, DexInstructionFormatter.K21c),

    SPUT_SHORT(0x6d, DexInstructionFormatter.K21c),


    //invoke-kind {vC, vD, vE, vF, vG}, meth@BBBB
    INVOKE_VIRTUAL(0x6e, DexInstructionFormatter.K35c),

    INVOKE_SUPER(0x6f, DexInstructionFormatter.K35c),

    INVOKE_DIRECT(0x70, DexInstructionFormatter.K35c),

    INVOKE_STATIC(0x71, DexInstructionFormatter.K35c),

    INVOKE_INTERFACE(0x72, DexInstructionFormatter.K35c),

    // 1a 21c	const-string vAA, string@BBBB
    CONST_STRING(0x1a, DexInstructionFormatter.K21c),

    //ff 21c	const-method-type vAA, proto@BBBB
    CONST_METHOD_TYPE(0xFF, DexInstructionFormatter.K21c);


    public final int opcode;
    public final DexInstructionFormatter format;

    DexOpCode(int opcode, DexInstructionFormatter format) {
        this.opcode = opcode;
        this.format = format;
    }

    public static DexOpCode get(int opcode) {
        if (stdOpcodes[opcode] == null) {
            System.out.println("Unknown opcode: 0x" + Integer.toHexString(opcode));
            return NOP;
        }
        return stdOpcodes[opcode];
    }

    private static final DexOpCode[] stdOpcodes = new DexOpCode[256];

    static {
        for (DexOpCode o : values()) {
            stdOpcodes[o.opcode] = o;
        }
    }

    // 依照 Android 官方文档，短整型的低 8 位是操作码。
    public static DexOpCode getOpcodeFromShort(int unsignedShort) {
        return get(unsignedShort & 0xFF);
    }
}
