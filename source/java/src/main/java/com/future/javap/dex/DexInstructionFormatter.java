package com.future.javap.dex;

public enum DexInstructionFormatter {

    K10x(1, 0, 'x'),
    K11x(1, 1, 'x'),
    K12x(1, 2, 'x'),
    K21c(2, 1, 'c'),
    K22x(2, 2, 'x'),
    K31i(3, 1, 'i'),
    K32x(3, 2, 'x'),
    K35c(3, 5, 'c');

    int shortCount;
    int registers;
    char symbol;

    DexInstructionFormatter(int shortCount, int registers, char symbol) {
        this.shortCount = shortCount;
        this.registers = registers;
        this.symbol = symbol;
    }
}
