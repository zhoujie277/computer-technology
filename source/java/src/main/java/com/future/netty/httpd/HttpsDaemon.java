package com.future.netty.httpd;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

public class HttpsDaemon {

    private static class SSLChannelInitializer extends ChannelInitializer<Channel> {
        private final SslContext sslContext;

        private SSLChannelInitializer() {
            String keyStoreFilePath = "/xxx/path";
            String keyStorePassword = "Password";
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new FileInputStream(keyStoreFilePath), keyStorePassword.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
                sslContext = SslContextBuilder.forServer(keyManagerFactory).build();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
            ch.pipeline().addLast(new SslHandler(sslEngine));
            ch.pipeline().addLast("decoder", new HttpRequestDecoder()).addLast("encoder", new HttpResponseDecoder())
                    .addLast("aggregator", new HttpObjectAggregator(512 * 1024)).addLast("handlers", new HttpHandler());
        }

    }

    public void run(int port) {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(boss, worker).channel(NioServerSocketChannel.class).localAddress(port)
                    .childHandler(new SSLChannelInitializer()).bind().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new HttpsDaemon().run(8081);
    }
}
