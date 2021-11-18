package com.future.netty.codec;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

class DecoderByte2Integer extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 4) {
            int i = in.readInt();
            System.out.println("decode an integer: " + i);
            out.add(i);
        }
    }

    public static void main(String[] args) throws IOException {
        EmbeddedChannel channel = new EmbeddedChannel(new DecoderByte2Integer(), new HandlerIntegerProcess());
        for (int i = 0; i < 100; i++) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(i);
            channel.writeInbound(buf);
        }
        System.in.read();
    }
}