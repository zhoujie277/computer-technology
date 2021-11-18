package com.future.netty;

import com.future.io.nio.NIOConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;

/**
 * Netty 回显服务器示例
 */
public class DiscardServer {

    private static class DiscardHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            ByteBuf buf = (ByteBuf) msg;
            try {
                while (buf.isReadable()) {
                    System.out.print((char) buf.readByte());
                }
                System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private ServerBootstrap bootstrap = new ServerBootstrap();
    private final int serverPort;

    public DiscardServer(int port) {
        this.serverPort = port;
    }

    public void run() {
        EventLoopGroup bossLoopGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerLoopGroup = new NioEventLoopGroup();

        try {
            // 1设置 reactor 线程组
            bootstrap.group(bossLoopGroup, workerLoopGroup);

            // 2 设置 nio 类型的 channel
            bootstrap.channel(NioServerSocketChannel.class);

            // 3 设置监听端口
            bootstrap.localAddress(serverPort);

            // 4 设置通道的参数
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

            // 5 装配子通道流水线
            bootstrap.childHandler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new DiscardHandler());
                }
            });

            // 6 开始绑定 server
            // 通过调用 sync 同步方法阻塞直到绑定成功
            ChannelFuture channelFuture = bootstrap.bind().sync();
            System.out.println(" 服务器启动成功，监听端口：" + channelFuture.channel().localAddress());

            // 7 等待通道关闭的异步任务结束
            // 服务监听通道会一直等待通道关闭的异步任务结束
            ChannelFuture closeFuture = channelFuture.channel().closeFuture();
            closeFuture.sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 8 优雅关闭 EventLoopGroup
            // 释放所有资源包括所创建的线程
            workerLoopGroup.shutdownGracefully();
            bossLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new DiscardServer(NIOConfig.getServerPort()).run();
    }

}
