package com.future.netty.protobuf;

import com.future.io.nio.NIOConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class ProtoBufClient {
    private static String sContent = "Hello ProtoBufClient";

    private ProtoBufMsgSample.Msg build(int id, String content) {
        ProtoBufMsgSample.Msg.Builder msg = ProtoBufMsgSample.Msg.newBuilder();
        msg.setId(id);
        msg.setContent(content);
        return msg.build();
    }

    public void run(String ip, int port) {
        EventLoopGroup worker = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        try {
            ChannelFuture future = bootstrap.group(worker).channel(NioSocketChannel.class).remoteAddress(ip, port)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(new ProtobufEncoder());
                        }

                    }).connect().sync();
            Channel channel = future.channel();
            for (int i = 0; i < 100; i++) {
                ProtoBufMsgSample.Msg msg = build(i, i + "->" + sContent);
                channel.writeAndFlush(msg);
                System.out.println("send data pack:" + i);
            }
            channel.flush();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
        new ProtoBufClient().run(NIOConfig.getServerIP(), NIOConfig.getServerPort());
    }
}
