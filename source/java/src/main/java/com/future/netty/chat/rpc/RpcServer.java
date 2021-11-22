package com.future.netty.chat.rpc;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.common.codec.CodecFrameDecoder;
import com.future.netty.chat.common.codec.MessageCodec;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcServer {

    private void run() {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
        MessageCodec codec = new MessageCodec();
        RpcMessageHandler rpcHandler = new RpcMessageHandler();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(worker, worker).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new CodecFrameDecoder()).addLast(loggingHandler).addLast(codec);
                            ch.pipeline().addLast(rpcHandler);
                        };
                    });
            ChannelFuture future = bootstrap.bind(NIOConfig.getServerPort()).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new RpcServer().run();
    }
}
