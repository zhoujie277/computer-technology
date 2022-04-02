package com.future.javap.dex;

import com.future.javap.IReader;

public class DexFile {

    private final int bytes;

    private final IReader reader;

    private final DexHeaderItem headerItem;

    public DexFile(IReader reader, int size) {
        this.reader = reader;
        this.bytes = size;
        headerItem = new DexHeaderItem(reader);
    }

    public void parseHeader() {
        headerItem.parseHeader();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DexFile ").append("bytes = ").append(bytes);
        builder.append(", headerItem = ").append(headerItem);
        return builder.toString();
    }
}
