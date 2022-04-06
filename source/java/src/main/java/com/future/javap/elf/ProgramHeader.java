package com.future.javap.elf;

import com.future.javap.IReader;

import java.util.HashMap;
import java.util.Map;

/**
 * 程序头结构体
 *
 * @author future
 */
@SuppressWarnings("SpellCheckingInspection")
public class ProgramHeader {

    // typedef struct {
    //        Elf32_Word  p_type;
    //        Elf32_Off   p_offset;
    //        Elf32_Addr  p_vaddr;
    //        Elf32_Addr  p_paddr;
    //        Elf32_Word  p_filesz;
    //        Elf32_Word  p_memsz;
    //        Elf32_Word  p_flags;
    //        Elf32_Word  p_align;
    //    } Elf32_Phdr;

    Elf64_Word p_type = new Elf64_Word();
    Elf64_Word p_flags = new Elf64_Word();
    Elf64_Off p_offset = new Elf64_Off();
    Elf64_Addr p_vaddr = new Elf64_Addr();
    Elf64_Addr p_paddr = new Elf64_Addr();
    Elf64_XWord p_filesz = new Elf64_XWord();
    Elf64_XWord p_memsz = new Elf64_XWord();
    Elf64_XWord p_align = new Elf64_XWord();

    public void parse(IReader reader) {
        p_type.read(reader);
        p_flags.read(reader);
        p_offset.read(reader);
        p_vaddr.read(reader);
        p_paddr.read(reader);
        p_filesz.read(reader);
        p_memsz.read(reader);
        p_align.read(reader);
    }

    public SegmentType getSegmentType() {
        SegmentType type = SegmentType.getType(this.p_type.value);
        if (type == null) {
            System.out.println("--------unknown segment type: 0x" + Long.toHexString(this.p_type.value));
            return SegmentType.PT_NULL;
        }
        return type;
    }

    private static final int PF_E = 1;
    private static final int PF_W = 2;
    private static final int PF_R = 4;

    /**
     * PF_E = 1
     * PF_W = 2
     * PF_R = 4
     */
    public String getFlagsString() {
        StringBuilder builder = new StringBuilder();
        if ((this.p_flags.value & PF_R) == PF_R) {
            builder.append("R");
        }
        if ((this.p_flags.value & PF_W) == PF_W) {
            builder.append("W");
        }
        if ((this.p_flags.value & PF_E) == PF_E) {
            builder.append("E");
        }
        if (builder.length() == 0) builder.append(this.p_flags.value);
        return builder.toString();
    }


    enum SegmentType {
        PT_NULL(0),
        PT_LOAD(1),
        PT_DYNAMIC(2),
        PT_INTERP(3),
        PT_NOTE(4),
        PT_SHLIB(5),
        PT_PHDR(6),
        GNU_PROPERTY(0x6474e553),
        GNU_EH_FRAME(0x6474e550),
        GNU_STACK(0x6474e551),
        GNU_RELRO(0x6474e552);

        int value;

        SegmentType(int v) {
            this.value = v;
        }

        public static SegmentType getType(int code) {
            return typeMap.get(code);
        }

        private static final Map<Integer, SegmentType> typeMap = new HashMap<>();

        static {
            for (SegmentType o : values()) {
                typeMap.put(o.value, o);
            }
        }
    }
}
