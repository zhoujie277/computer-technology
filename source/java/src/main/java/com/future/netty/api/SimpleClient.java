package com.future.netty.api;

import java.net.InetSocketAddress;

import com.future.io.nio.NIOConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

public class SimpleClient implements Runnable {

    @Override
    public void run() {
        try {
            new Bootstrap().group(new NioEventLoopGroup()).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {

                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringEncoder());
                        }

                    }).connect(new InetSocketAddress(NIOConfig.getServerIP(), NIOConfig.getServerPort())).sync()
                    .channel().writeAndFlush("hello,world");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SimpleClient().run();
    }
}
