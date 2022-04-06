package com.future.javap.elf;

public class ElfFormatError extends RuntimeException {

    public ElfFormatError(String reason) {
        super(reason);
    }
}
