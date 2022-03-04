package com.future.concurrent.api;


import java.util.concurrent.Exchanger;

@SuppressWarnings("unused")
public class ExchangerAPI {

    static class DataBuffer {
        public boolean isFull() {
            return false;
        }

        public boolean isEmpty() {
            return false;
        }
    }

    Exchanger<DataBuffer> exchanger = new Exchanger<>();
    DataBuffer initialEmptyBuffer = new DataBuffer();
    DataBuffer initialFullBuffer = new DataBuffer();

    private void addToBuffer(DataBuffer buffer) {

    }

    void takeFromBuffer(DataBuffer buffer) {

    }

    class FillingLoop implements Runnable {
        @Override
        public void run() {
            DataBuffer currentBuffer = initialEmptyBuffer;
            try {
                while (currentBuffer != null) {
                    addToBuffer(currentBuffer);
                    if (currentBuffer.isFull()) {
                        currentBuffer = exchanger.exchange(currentBuffer);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class EmptyingLoop implements Runnable {
        @Override
        public void run() {
            DataBuffer currentBuffer = initialFullBuffer;
            try {
                while (currentBuffer != null) {
                    takeFromBuffer(currentBuffer);
                    if (currentBuffer.isEmpty())
                        currentBuffer = exchanger.exchange(currentBuffer);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void start() {
        new Thread(new FillingLoop()).start();
        new Thread(new EmptyingLoop()).start();
    }
}
