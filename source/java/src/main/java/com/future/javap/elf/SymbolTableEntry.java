package com.future.javap.elf;

import com.future.javap.IReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Symbol Table
 */
public class SymbolTableEntry {
    // typedef struct {
    //        Elf32_Word  st_name;
    //        Elf32_Addr  st_value;
    //        Elf32_Word  st_size;
    //        unsigned char st_info;
    //        unsigned char st_other;
    //        Elf32_Half  st_shndx;
    //    } Elf32_Sym;

    Elf64_Word st_name = new Elf64_Word();
    byte st_info;
    byte st_other;
    Elf64_Half st_shndx = new Elf64_Half();
    Elf64_Addr st_value = new Elf64_Addr();
    Elf64_XWord st_size = new Elf64_XWord();

    String symbolName = "";

    private int ELF64_ST_BIND() {
        return st_info >> 4;
    }

    private int ELF64_ST_TYPE() {
        return st_info & 0xf;
    }

    public static int ELF_ST_INFO(int bind, int type) {
        return (bind << 4) | (type & 0xf);
    }

    public void parse(IReader reader) {
        st_name.read(reader);
        st_info = (byte) reader.readUnsignedByte();
        st_other = (byte) reader.readUnsignedByte();
        st_shndx.read(reader);
        st_value.read(reader);
        st_size.read(reader);
    }

    public SymbolBind getSymbolBind() {
        return SymbolBind.getSymbolBind(ELF64_ST_BIND());
    }

    public SymbolType getSymbolType() {
        return SymbolType.getSymbolType(ELF64_ST_TYPE());
    }

    enum SymbolBind {
        STB_LOCAL(0),
        STB_GLOBAL(1),
        STB_WEAK(2),
        STB_LOPROC(13),
        STB_HIPROC(15);

        int value;

        SymbolBind(int value) {
            this.value = value;
        }

        public static SymbolBind getSymbolBind(int type) {
            return bindMap.get(type);
        }

        private static final Map<Integer, SymbolBind> bindMap = new HashMap<>();

        static {
            for (SymbolBind o : values()) {
                bindMap.put(o.value, o);
            }
        }
    }

    enum SymbolType {
        STT_NOTYPE(0),
        STT_OBJECT(1),
        STT_FUNC(2),
        STT_SECTION(3),
        STT_FILE(4),
        STT_LOPROC(13),
        STT_HIPROC(15);

        int value;

        SymbolType(int value) {
            this.value = value;
        }

        public static SymbolType getSymbolType(int type) {
            return typeMap.get(type);
        }

        private static final Map<Integer, SymbolType> typeMap = new HashMap<>();

        static {
            for (SymbolType o : values()) {
                typeMap.put(o.value, o);
            }
        }
    }
}
