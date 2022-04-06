package com.future.javap.elf;

import com.future.javap.IReader;

import java.util.*;

public class ELFFile {

    private final int fileBytes;
    private final IReader reader;

    private final ELFHeader header = new ELFHeader();
    private SectionHeader[] sectionHeaders;
    private ProgramHeader[] programHeaders;
    private SectionHeader symbolSectionHeader;
    private SymbolTableEntry[] symbolTable;
    private final List<SectionHeader> relocationHeaderTable = new ArrayList<>();
    private final Map<SectionHeader, RelocationEntry[]> relocationHeaderMap = new HashMap<>();

    public ELFFile(IReader reader, int fileBytes) {
        this.reader = reader;
        this.fileBytes = fileBytes;
    }

    public void parse() {
        header.parse(reader);
        parseSectionHeaderTable();
        parseProgramHeaderTable();
        parseSectionNames();
        parseSymbolTable();
        parseSymbolNames();
        parseRelocations();
    }

    private void parseSectionHeaderTable() {
        short sectionHeaderNumber = header.e_shnum.value;
        short sectionHeaderEntrySize = header.e_shentsize.value;
        sectionHeaders = new SectionHeader[sectionHeaderNumber];
        int sectionHeaderOffset = (int) header.e_shoff.value;
        reader.seek(sectionHeaderOffset);
        for (int i = 0; i < sectionHeaderNumber; i++) {
            SectionHeader sectionHeader = new SectionHeader();
            byte[] bytes = reader.readArray(sectionHeaderEntrySize);
            sectionHeader.parse(bytes, reader.getByteOrder());
            switch (sectionHeader.getSectionTypes()) {
                case SHT_SYMTAB:
                    symbolSectionHeader = sectionHeader;
                    break;
                case SHT_REL:
                case SHT_RELA:
                    this.relocationHeaderTable.add(sectionHeader);
                    break;
                default:
                    break;
            }
            sectionHeaders[i] = sectionHeader;
        }
    }

    private void parseProgramHeaderTable() {
        short programHeaderNumber = header.e_phnum.value;
        short programHeaderEntrySize = header.e_phentsize.value;
        programHeaders = new ProgramHeader[programHeaderNumber];
        int programHeaderOffset = (int) header.e_phoff.value;
        reader.seek(programHeaderOffset);
        for (int i = 0; i < programHeaderNumber; i++) {
            ProgramHeader programHeader = new ProgramHeader();
            byte[] bytes = reader.readArray(programHeaderEntrySize);
            ByteArrayReader in = new ByteArrayReader(bytes, reader.getByteOrder());
            programHeader.parse(in);
//            switch (sectionHeader.getSectionTypes()) {
//                case SHT_SYMTAB:
//                    symbolSectionHeader = sectionHeader;
//                    break;
//                case SHT_REL:
//                case SHT_RELA:
//                    this.relocationHeaderTable.add(sectionHeader);
//                    break;
//                default:
//                    break;
//            }
            programHeaders[i] = programHeader;
        }
    }

    private void parseSectionNames() {
        SectionHeader sectionHeaderString = sectionHeaders[header.e_shstrndx.value];
        int sh_offset = (int) sectionHeaderString.sh_offset.value;
        for (SectionHeader sectionHeader : sectionHeaders) {
            if (sectionHeader.sh_name.value == 0) continue;
            reader.seek(sh_offset + sectionHeader.sh_name.value);
            sectionHeader.sectionName = readString(reader);
        }
    }

    private void parseSymbolTable() {
        if (symbolSectionHeader == null) return;
        Elf64_Off symbolSectionOffset = symbolSectionHeader.sh_offset;
        Elf64_XWord symbolSectionEntrySize = symbolSectionHeader.sh_entsize;
        Elf64_XWord symbolSectionSize = symbolSectionHeader.sh_size;
        int entries = (int) (symbolSectionSize.value / symbolSectionEntrySize.value);
        symbolTable = new SymbolTableEntry[entries];
        reader.seek((int) symbolSectionOffset.value);
        for (int i = 0; i < entries; i++) {
            byte[] bytes = reader.readArray((int) symbolSectionEntrySize.value);
            ByteArrayReader r = new ByteArrayReader(bytes);
            r.setByteOrder(reader.getByteOrder());
            symbolTable[i] = new SymbolTableEntry();
            symbolTable[i].parse(r);
        }
    }

    public void parseSymbolNames() {
        // symbolSectionHeader.sh_link 是符号相关联的字符串表。
        SectionHeader strSectionHeader = sectionHeaders[symbolSectionHeader.sh_link.value];
        int strSectionOffset = (int) strSectionHeader.sh_offset.value;
        for (SymbolTableEntry entry : symbolTable) {
            if (entry.st_name.value == 0) continue;
            reader.seek(strSectionOffset + entry.st_name.value);
            entry.symbolName = readString(reader);
        }
    }

    public void parseRelocations() {
        for (SectionHeader relocationHeader : relocationHeaderTable) {
            int offset = (int) relocationHeader.sh_offset.value;
            int sh_entsize = (int) relocationHeader.sh_entsize.value;
            int entries = (int) (relocationHeader.sh_size.value / sh_entsize);
            RelocationEntry[] relocationArray = new RelocationEntry[entries];
            reader.seek(offset);
            for (int j = 0; j < entries; j++) {
                byte[] bytes = reader.readArray(sh_entsize);
                ByteArrayReader in = new ByteArrayReader(bytes);
                in.setByteOrder(reader.getByteOrder());
                relocationArray[j] = new RelocationEntry();
                relocationArray[j].parse(in);
            }
            relocationHeaderMap.put(relocationHeader, relocationArray);
        }
    }

    private String readString(IReader reader) {
        int ch;
        StringBuilder builder = new StringBuilder();
        do {
            ch = reader.readUnsignedByte();
            if (ch == '\0') break;
            builder.append((char) ch);
        } while (true);
        return builder.toString();
    }

    private void sectionHeaderTableToString(StringBuilder builder) {
        if (sectionHeaders == null) return;
        builder.append("Sections Headers: ");
        Formatter formatter = new Formatter();
        String format = "\n\t[%2s]\t%20s\t%15s\t%12s\t%12s\t%6s\t%4s\t%4s\t%4s\t%4s\t%4s";
        formatter.format(format, "Nr", "Name", "Type", "Address", "Off", "Size", "ES", "Flag", "Link", "Info", "Align");
        for (int i = 0; i < sectionHeaders.length; i++) {
            SectionHeader s = sectionHeaders[i];
            formatter.format(format, i + "",
                    s.sectionName,
                    s.getSectionTypes(),
                    "0x" + Long.toHexString(s.sh_addr.value),
                    "0x" + Long.toHexString(s.sh_offset.value),
                    "0x" + Long.toHexString(s.sh_size.value),
                    s.sh_entsize,
                    s.getFlagsString(),
                    s.sh_link, s.sh_info, s.sh_addralign);
        }
        builder.append(formatter);
    }

    private void programHeaderTableToString(StringBuilder builder) {
        if (programHeaders == null) return;
        builder.append("Program Headers: ");
        Formatter formatter = new Formatter();
        String format = "\n\t[%2s]\t%13s\t%10s\t%15s\t%15s\t%10s\t%10s\t%4s\t%8s";
        formatter.format(format, "[Nr]", "Type", "Offset", "VirtAddr", "PhysAddr", "FileSiz", "MemSiz", "Flg", "Align");
        for (int i = 0; i < programHeaders.length; i++) {
            ProgramHeader p = programHeaders[i];
            formatter.format(format, i + "",
                    p.getSegmentType(),
                    "0x" + Long.toHexString(p.p_offset.value),
                    "0x" + Long.toHexString(p.p_vaddr.value),
                    "0x" + Long.toHexString(p.p_paddr.value),
                    "0x" + Long.toHexString(p.p_filesz.value),
                    "0x" + Long.toHexString(p.p_memsz.value),
                    p.getFlagsString(),
                    "0x" + Long.toHexString(p.p_align.value));
        }
        builder.append(formatter);
    }

    private void symbolTableToString(StringBuilder builder) {
        Formatter formatter = new Formatter();
        formatter.format("Symbol table '%s' contains %d entries: ", symbolSectionHeader.sectionName, symbolTable.length);
        String format = "\n\t[%2s]\t%10s\t%10s\t%12s\t%12s\t%8s\t%8s\t%20s";
        formatter.format(format, "Num", "Value", "Size", "Type", "Bind", "Vis", "Ndx", "Name");
        for (int i = 0; i < symbolTable.length; i++) {
            SymbolTableEntry entry = symbolTable[i];
            formatter.format(format, i + "",
                    "0x" + Long.toHexString(entry.st_value.value),
                    entry.st_size,
                    entry.getSymbolType(),
                    entry.getSymbolBind(),
                    entry.st_other,
                    entry.st_shndx,
                    entry.symbolName);
        }
        builder.append(formatter);
    }

    private void relocationToString(StringBuilder builder) {
        Formatter formatter = new Formatter();
        for (SectionHeader relocationHeader : relocationHeaderTable) {
            RelocationEntry[] relocationTable = this.relocationHeaderMap.get(relocationHeader);
            formatter.format("\n\nRelocation section '%s at offset 0x%s contains %s entries: ", relocationHeader.sectionName, Long.toHexString(relocationHeader.sh_offset.value), relocationTable.length);
            String format = "\n%12s\t%12s\t%15s\t%15s\t%15s\t%8s";
            formatter.format(format, "Offset", "Info", "Type", "Symbol's Value", "Symbol's Name", "+Append");
            for (RelocationEntry entry : relocationTable) {
                long symIndex = entry.ELF64_R_SYM();
                String symbolName = symbolTable[(int) symIndex].symbolName;
                //TODO: 这个 Symbol Name 暂时不知道计算得到。
                formatter.format(format, "0x" + Long.toHexString(entry.r_offset.value),
                        "0x" + Long.toHexString(entry.r_info.value),
                        "0x" + Long.toHexString(entry.ELF64_R_TYPE()),
                        "0",
                        symbolName,
                        entry.addendString());
            }
        }
        builder.append(formatter);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ELFFile ").append("fileBytes:").append(fileBytes);
        builder.append("\n").append(header);
        builder.append("\n\n");
        sectionHeaderTableToString(builder);
        builder.append("\n\n");
        programHeaderTableToString(builder);
        builder.append("\n\n");
        symbolTableToString(builder);
        builder.append("\n");
        relocationToString(builder);
        return builder.toString();
    }


}
