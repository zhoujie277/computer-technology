package com.future.netty.protobuf;

import com.future.io.nio.NIOConfig;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.logging.LoggingHandler;

/**
 * ProtoBuf 演示
 */
class ProtoBufServer {

    static class ProtobufBussinessDecoder extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ProtoBufMsgSample.Msg protoMsg = (ProtoBufMsgSample.Msg) msg;
            System.out.println("Received one protobuf message.");
            System.out.println("protoMsg.getId() = " + protoMsg.getId());
            System.out.println("protoMsg.getContent() = " + protoMsg.getContent());
        }
    }

    public void run(int port) {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            ChannelFuture future = bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
                    .localAddress(port).option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            // protobufDecoder仅仅负责编码，并不支持读半包，所以在之前，一定要有读半包的处理器。
                            // 有三种方式可以选择：
                            // 使用netty提供ProtobufVarint32FrameDecoder
                            // 继承netty提供的通用半包处理器 LengthFieldBasedFrameDecoder
                            // 继承ByteToMessageDecoder类，自己处理半包
                            ch.pipeline().addLast(new LoggingHandler()).addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new ProtobufDecoder(ProtoBufMsgSample.Msg.getDefaultInstance()))
                                    .addLast(new LoggingHandler()).addLast(new ProtobufBussinessDecoder());
                        }

                    }).bind().sync();
            System.out.println("server has started...");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        int port = NIOConfig.getServerPort();
        new ProtoBufServer().run(port);
    }
}
