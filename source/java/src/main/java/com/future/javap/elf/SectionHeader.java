package com.future.javap.elf;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class SectionHeader {
    // typedef struct {
    //        Elf32_Word  sh_name;
    //        Elf32_Word  sh_type;
    //        Elf32_Word  sh_flags;
    //        Elf32_Addr  sh_addr;
    //        Elf32_Off  sh_offset;
    //        Elf32_Word  sh_size;
    //        Elf32_Word  sh_link;
    //        Elf32_Word  sh_info;
    //        Elf32_Word  sh_addralign;
    //        Elf32_Word  sh_entsize;
    //    } Elf32_Shdr;

    Elf64_Word sh_name = new Elf64_Word();
    Elf64_Word sh_type = new Elf64_Word();
    Elf64_XWord sh_flags = new Elf64_XWord();
    Elf64_Addr sh_addr = new Elf64_Addr();
    Elf64_Off sh_offset = new Elf64_Off();
    Elf64_XWord sh_size = new Elf64_XWord();
    Elf64_Word sh_link = new Elf64_Word();
    Elf64_Word sh_info = new Elf64_Word();
    Elf64_XWord sh_addralign = new Elf64_XWord();
    Elf64_XWord sh_entsize = new Elf64_XWord();

    String sectionName = "";

    @SuppressWarnings("DuplicatedCode")
    public void parse(byte[] bytes, int byteOrder) {
        ByteArrayReader reader = new ByteArrayReader(bytes);
        reader.setByteOrder(byteOrder);
        sh_name.read(reader);
        sh_type.read(reader);
        sh_flags.read(reader);
        sh_addr.read(reader);
        sh_offset.read(reader);
        sh_size.read(reader);
        sh_link.read(reader);
        sh_info.read(reader);
        sh_addralign.read(reader);
        sh_entsize.read(reader);
    }

    public SectionTypes getSectionTypes() {
        SectionTypes sectionTypes = SectionTypes.getSectionTypes(sh_type.value);
        if (sectionTypes == null) {
            System.out.println("--------------SectionTypes not found : " + sh_type.value);
            return SectionTypes.SHT_NULL;
        }
        return sectionTypes;
    }

    enum SectionTypes {
        SHT_NULL(0),
        SHT_PROGBITS(1),
        SHT_SYMTAB(2),
        SHT_STRTAB(3),
        SHT_RELA(4),
        SHT_HASH(5),
        SHT_DYNAMIC(6),
        SHT_NOTE(7),
        SHT_NOBITS(8),
        SHT_REL(9),
        SHT_SHLTB(10),
        SHT_DYNSYM(11),
        SHT_LOPROC(0x70000000),
        SHT_HIPROC(0x7fffffff),
        SHT_LOUSER(0x80000000),
        SHT_HIUSER(0xffffffff);

        private final int code;

        SectionTypes(int code) {
            this.code = code;
        }

        public static SectionTypes getSectionTypes(int code) {
            return typesMap.get(code);
        }

        final static Map<Integer, SectionTypes> typesMap = new HashMap<>();

        static {
            for (SectionTypes value : values()) {
                typesMap.put(value.code, value);
            }
        }
    }


    public String getFlagsString() {
        StringBuilder builder = new StringBuilder();
        if ((this.sh_flags.value & SectionAttributeFlags.SHF_WRITE.value) == SectionAttributeFlags.SHF_WRITE.value) {
            builder.append("W");
        }
        if ((this.sh_flags.value & SectionAttributeFlags.SHF_ALLOC.value) == SectionAttributeFlags.SHF_ALLOC.value) {
            builder.append("A");
        }
        if ((this.sh_flags.value & SectionAttributeFlags.SHF_EXECINSTR.value) == SectionAttributeFlags.SHF_EXECINSTR.value) {
            builder.append("X");
        }
        if ((this.sh_flags.value & SectionAttributeFlags.SHF_MASKPROC.value) == SectionAttributeFlags.SHF_MASKPROC.value) {
            builder.append("M");
        }
        if (builder.length() == 0) builder.append(this.sh_flags.value);
        return builder.toString();
    }

    enum SectionAttributeFlags {
        SHF_WRITE(0x1),
        SHF_ALLOC(0x2),
        SHF_EXECINSTR(0x4),
        SHF_MASKPROC(0xf0000000);

        int value;

        SectionAttributeFlags(int value) {
            this.value = value;
        }
    }
}
