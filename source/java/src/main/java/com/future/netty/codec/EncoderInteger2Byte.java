package com.future.netty.codec;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToByteEncoder;

class EncoderInteger2Byte extends MessageToByteEncoder<Integer> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out) throws Exception {
        out.writeInt(msg);
        System.out.println("encoder Integer=" + msg);
    }

    public static void main(String[] args) throws IOException {
        EmbeddedChannel channel = new EmbeddedChannel(new EncoderInteger2Byte());

        for (int i = 0; i < 100; i++) {
            channel.write(i);
        }
        channel.flush();

        // 读取模拟的出站数据包
        ByteBuf buf = channel.readOutbound();
        while (buf != null) {
            System.out.println("o = " + buf.readInt());
            buf = channel.readOutbound();
        }

        System.in.read();
    }
}
