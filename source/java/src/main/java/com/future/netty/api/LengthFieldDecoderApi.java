package com.future.netty.api;

import com.future.netty.utils.ByteBufPrint;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class LengthFieldDecoderApi {

    private static void send(ByteBuf buf, String content) {
        byte[] bytes = content.getBytes();
        int length = bytes.length;
        buf.writeInt(length);
        buf.writeBytes(bytes);
    }

    public void run() {
        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4),
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        ByteBufPrint.log((ByteBuf) msg);
                        // System.out.println(msg);
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        cause.printStackTrace();
                        super.exceptionCaught(ctx, cause);
                    }
                });

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        send(buf, "Hello world");
        send(buf, "Hi ");
        channel.writeInbound(buf);
    }

    public static void main(String[] args) {
        new LengthFieldDecoderApi().run();
    }

}
