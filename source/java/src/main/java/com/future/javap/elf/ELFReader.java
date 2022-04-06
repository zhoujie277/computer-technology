package com.future.javap.elf;

import java.io.IOException;
import java.io.InputStream;

public class ELFReader {
    
    public void loadFile(String path) throws IOException {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
        if (inputStream == null) throw new IllegalArgumentException("Elf file not found! path: " + path);
        byte[] buf = new byte[inputStream.available()];
        int dexSize = inputStream.read(buf);
        ByteArrayReader reader = new ByteArrayReader(buf);
        ELFFile elf = new ELFFile(reader, dexSize);
        elf.parse();
        System.out.println(elf);
    }

    public static void main(String[] args) throws IOException {
        ELFReader reader = new ELFReader();
//        reader.loadFile("part.o");
//        reader.loadFile("main.o");
        reader.loadFile("main_s");
    }
}
