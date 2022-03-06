package com.future.concurrent;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

class PipedStreamAPI {

    void test() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);
        new Thread(new Task(in), "Task").start();
        int recv;
        try {
            while ((recv = System.in.read()) != -1) {
                out.write(recv);
            }
        } finally {
            out.close();
        }
    }

    static class Task implements Runnable {

        PipedInputStream in;

        public Task(PipedInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            int len;
            try {
                while ((len = in.read()) != 0) {
                    System.out.println((char) len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        PipedStreamAPI api = new PipedStreamAPI();
        api.test();
    }
}
