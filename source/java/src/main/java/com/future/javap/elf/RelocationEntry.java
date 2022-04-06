package com.future.javap.elf;

import com.future.javap.IReader;

@SuppressWarnings("SpellCheckingInspection")
public class RelocationEntry {

    // typedef struct {
    //      Elf64_Addr r_offset;
    //      Elf64_XWord r_info;
    // } Elf64_Rel;

    //    typedef struct {
    Elf64_Addr r_offset = new Elf64_Addr();
    Elf64_XWord r_info = new Elf64_XWord();
    Elf64_SXWord r_addend = new Elf64_SXWord();
    // } Elf64_Rela;

    public void parse(IReader reader) {
        r_offset.read(reader);
        r_info.read(reader);
        r_addend.read(reader);
    }

    public long ELF64_R_SYM() {
        return r_info.value >> 32;
    }

    public int ELF64_R_TYPE() {
        return (int) (r_info.value);
    }

    public static long ELF64_R_INFO(long sym, long type) {
        return sym << 32 + type;
    }

    public String addendString() {
        if (r_addend.value >= 0) {
            return "+ 0x" + Long.toHexString(r_addend.value);
        }
        return r_addend.toString();
    }

    // Figure 1-22: Relocation Type
    // Name	Value	Field	Calculation
    // R_386_NONE	0	none	none
    //R_386_32	1	word32	S + A
    //R_386_PC32	2	word32	S + A - P
    //R_386_GOT32	3	word32	G + A - P
    //R_386_PLT32	4	word32	L + A - P
    //R_386_COPY	5	none	none
    //R_386_GLOB_DAT	6	word32	S
    //R_386_JMP_SLOT	7	word32	S
    //R_386_RELATIVE	8	word32	S + A
    //R_386_GOTOFF	9	word32	S + A - GOT
    //R_386_GOTPC	10	word32	GOT + A - P
}
