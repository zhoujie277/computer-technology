package com.future.jvm.instrumentation;

import java.io.IOException;

public class InstrumentationMain {

    private int count;

    public void run() {
        count++;
    }

    public static void main(String[] args) throws IOException {
        System.in.read();
        InstrumentationMain main = new InstrumentationMain();
        main.run();
    }
}
