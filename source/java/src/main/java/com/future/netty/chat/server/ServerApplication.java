package com.future.netty.chat.server;

public class ServerApplication {

    public static void main(String[] args) {
        NettyServer server = new NettyServer();
        server.run();
    }

}
