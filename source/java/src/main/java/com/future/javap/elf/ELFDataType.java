package com.future.javap.elf;

import com.future.javap.IReader;

public interface ELFDataType {
    void read(IReader reader);
}

// typedef uint64_t	Elf64_Addr;
// typedef uint16_t	Elf64_Half;
// typedef uint64_t	Elf64_Off;
// typedef int32_t		Elf64_Sword;
// typedef int64_t		Elf64_Sxword;
// typedef uint32_t	Elf64_Word;
// typedef uint64_t	Elf64_Lword;
// typedef uint64_t	Elf64_Xword;
class Elf64_Off implements ELFDataType {

    long value;

    @Override
    public void read(IReader reader) {
        value = reader.readUnsignedLong();
    }

    @Override
    public String toString() {
        return value + "";
    }
}

class Elf64_Word implements ELFDataType {

    int value;

    @Override
    public void read(IReader reader) {
        value = reader.readUnsignedInt();
    }

    @Override
    public String toString() {
        return value + "";
    }
}

class Elf64_Half implements ELFDataType {

    short value;

    @Override
    public void read(IReader reader) {
        value = (short) reader.readUnsignedShort();
    }

    @Override
    public String toString() {
        return value + "";
    }
}

class Elf64_Addr implements ELFDataType {
    long value;

    @Override
    public void read(IReader reader) {
        value = reader.readUnsignedLong();
    }

    @Override
    public String toString() {
        return value + "";
    }
}

class Elf64_XWord implements ELFDataType {
    long value;

    @Override
    public void read(IReader reader) {
        value = reader.readUnsignedLong();
    }

    @Override
    public String toString() {
        return value + "";
    }
}

class Elf64_SXWord implements ELFDataType {
    long value;

    @Override
    public void read(IReader reader) {
        value = reader.readUnsignedLong();
    }

    public String toString() {
        return value + "";
    }
}