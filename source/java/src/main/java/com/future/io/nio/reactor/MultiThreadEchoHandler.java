package com.future.io.nio.reactor;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadEchoHandler implements Runnable {

    final SelectionKey key;
    final SocketChannel channel;
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    static final int RECIEVING = 0, SENDING = 1;
    int state = RECIEVING;

    static ExecutorService pool = Executors.newFixedThreadPool(4);

    public MultiThreadEchoHandler(SocketChannel channel, SelectionKey sKey) {
        this.channel = channel;
        this.key = sKey;
    }

    @Override
    public void run() {
        pool.execute(new AsyncTask());
    }

    class AsyncTask implements Runnable {
        @Override
        public void run() {
            asyncRun();
        }
    }

    public synchronized void asyncRun() {
        try {
            if (state == SENDING) {
                channel.write(buffer);
                buffer.clear();
                state = RECIEVING;
                key.interestOps(SelectionKey.OP_READ);
            } else if (state == RECIEVING) {
                int length = 0;
                while ((length = channel.read(buffer)) > 0) {
                    System.out.println(new String(buffer.array(), 0, length));
                }
                buffer.flip();
                state = SENDING;
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
