
package com.future.netty.practice;

import java.net.InetSocketAddress;
import java.util.Random;

import com.future.io.nio.NIOConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 采用定长消息报解决粘包半包问题
 */
@Slf4j
public class StickyClient implements Runnable {
    private Random random = new Random();

    private static byte[] fill10Bytes(char c, int len) {
        byte[] bytes = new byte[10];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) c;
        }
        for (int i = len; i < bytes.length; i++) {
            bytes[i] = '_';
        }
        return bytes;
    }

    @Override
    public void run() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(worker);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            log.debug("---ChannelInboundHandlerAdapter----");
                            ByteBuf buffer = ctx.alloc().buffer();
                            char c = '0';
                            for (int i = 0; i < 10; i++) {
                                byte[] bytes = fill10Bytes(c, random.nextInt(10) + 1);
                                c++;
                                buffer.writeBytes(bytes);
                            }
                            ctx.writeAndFlush(buffer);
                        }
                    });
                };
            });
            ChannelFuture channelFuture = bootstrap
                    .connect(new InetSocketAddress(NIOConfig.getServerIP(), NIOConfig.getServerPort())).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new StickyClient().run();
    }

}
