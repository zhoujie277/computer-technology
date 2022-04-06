package com.future.javap.elf;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
enum E_TYPE {
    ET_NONE(0, "No file type"),
    ET_REL(1, "Relocatable file"),
    ET_EXEC(2, "Executable file"),
    ET_DYN(3, "Shared object file"),
    ET_CORE(4, "Core file"),
    ET_LOPROC(0xff00, "Processor-specific"),
    ET_HIPROC(0xffff, "Processor-specific");

    private final String desc;
    private final int code;

    E_TYPE(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static E_TYPE getType(int code) {
        return typeMap.get(code);
    }

    public String getDesc() {
        return desc;
    }

    private static final Map<Integer, E_TYPE> typeMap = new HashMap<>();

    static {
        for (E_TYPE value : values()) {
            typeMap.put(value.code, value);
        }
    }

    @Override
    public String toString() {
        return name() + " (" + desc + ')';
    }
}
