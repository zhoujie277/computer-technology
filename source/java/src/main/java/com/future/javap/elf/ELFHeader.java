package com.future.javap.elf;

import com.future.javap.IReader;

@SuppressWarnings("SpellCheckingInspection")
public class ELFHeader {

    public static final int EI_NIDENT = 16;
    private static final int EI_CLASS = 5;
    private static final int EI_DATA = 6;
    private static final int EI_VERSION = 7;
    private static final int ELFDATA2LSB = 1;
    private static final int ELFDATA2MSB = 2;

    private byte[] e_ident;

    Elf64_Half e_type = new Elf64_Half();
    Elf64_Half e_machine = new Elf64_Half();
    Elf64_Word e_version = new Elf64_Word();
    Elf64_Addr e_entry = new Elf64_Addr();
    Elf64_Off e_phoff = new Elf64_Off();
    Elf64_Off e_shoff = new Elf64_Off();
    Elf64_Word e_flags = new Elf64_Word();
    Elf64_Half e_ehsize = new Elf64_Half();
    Elf64_Half e_phentsize = new Elf64_Half();
    Elf64_Half e_phnum = new Elf64_Half();
    Elf64_Half e_shentsize = new Elf64_Half();
    Elf64_Half e_shnum = new Elf64_Half();
    Elf64_Half e_shstrndx = new Elf64_Half();

    @SuppressWarnings("DuplicatedCode")
    public void parse(IReader reader) {
        byte[] bytes = reader.readArray(EI_NIDENT);
        if (!isElfFile(bytes)) throw new ElfFormatError("Invalid File Magic");
        this.e_ident = bytes;
        if (e_ident[EI_DATA] == 0) throw new ElfFormatError(("Invalid Data Encoding"));
        reader.setByteOrder(e_ident[EI_DATA] == ELFDATA2LSB ? IReader.ORDER_LITTLE_INDIAN : IReader.ORDER_BIG_INDIAN);
        e_type.read(reader);
        e_machine.read(reader);
        e_version.read(reader);
        e_entry.read(reader);
        e_phoff.read(reader);
        e_shoff.read(reader);
        e_flags.read(reader);
        e_ehsize.read(reader);
        e_phentsize.read(reader);
        e_phnum.read(reader);
        e_shentsize.read(reader);
        e_shnum.read(reader);
        e_shstrndx.read(reader);
    }

    public boolean isElfFile(byte[] identity) {
        if (identity[0] != 0x7f || identity[1] != 'E' || identity[2] != 'L' || identity[3] != 'F') {
            return false;
        }
        return true;
    }

    public int getElClass() {
        return e_ident[EI_CLASS];
    }

    public int getEiVersion() {
        return e_ident[EI_VERSION];
    }

    public E_TYPE getEType() {
        return E_TYPE.getType(e_type.value);
    }

    @Override
    public String toString() {
        return "ELFHeader " +
                "\ne_type: " + getEType() +
                ", Machine: " + e_machine +
                ", Version: " + e_version +
                "\nEntry point address: 0x" + Long.toHexString(e_entry.value) +
                "\nStart of program headers: " + e_phoff +
                "\nStart of section headers: " + e_shoff +
                "\nFlags: " + e_flags +
                "\nSize of this header: " + e_ehsize + " (bytes)" +
                "\nSize of program headers: " + e_phentsize +
                "\nNumber of program headers: " + e_phnum +
                "\nSize of section headers: " + e_shentsize +
                "\nNumber of section headers: " + e_shnum +
                "\nSection header string table index: " + e_shstrndx;
    }
}
