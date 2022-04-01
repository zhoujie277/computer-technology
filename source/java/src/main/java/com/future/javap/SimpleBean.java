package com.future.javap;


import java.io.IOException;
import java.io.InputStream;

public class SimpleBean implements AutoCloseable, Runnable {

    public final static long CONS = 64L;

    private String name;
    private int age;

    private InputStream inputStream;

    @Override
    public void close() throws Exception {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Hello world!");
    }
}
