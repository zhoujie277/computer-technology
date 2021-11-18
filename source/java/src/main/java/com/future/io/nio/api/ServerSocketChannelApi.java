package com.future.io.nio.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.future.io.nio.NIOConfig;

public class ServerSocketChannelApi implements Runnable {

    @Override
    public void run() {
        while (true) {
            ServerSocketChannel server;
            ByteBuffer buffer = ByteBuffer.allocate(16);
            try {
                server = ServerSocketChannel.open();
                // server.configureBlocking(false);
                server.bind(new InetSocketAddress(NIOConfig.getServerPort()));

                while (true) {
                    SocketChannel client = server.accept();
                    System.out.println("connected..." + client);
                    int read = client.read(buffer);
                    if (read > 0) {
                        buffer.flip();
                        System.out.println(new String(buffer.array(), 0, buffer.limit()));
                        buffer.clear();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        new ServerSocketChannelApi().run();
    }
}
