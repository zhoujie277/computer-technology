package com.future.io.nio.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.future.io.nio.NIOConfig;

public class SocketChannelApi implements Runnable {

    @Override
    public void run() {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(NIOConfig.getServerIP(), NIOConfig.getServerPort()));
            System.out.println("waiting...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SocketChannelApi().run();
    }

}
