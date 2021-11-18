package com.future.netty.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageEncoder;

public class EncoderString2Integer extends MessageToMessageEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        char[] charArray = msg.toCharArray();
        for (char c : charArray) {
            // 0 - 9
            if (c >= 48 && c <= 57) {
                out.add(Character.getNumericValue(c));
            }
        }
    }

    public static void main(String[] args) {
        EmbeddedChannel channel = new EmbeddedChannel(new EncoderInteger2Byte(), new EncoderString2Integer());
        for (int j = 0; j < 100; j++) {
            String s = "i am " + j;
            channel.write(s);
        }
        channel.flush();
        ByteBuf buf = (ByteBuf) channel.readOutbound();
        while (null != buf) {
            System.out.println("o = " + buf.readInt());
            buf = (ByteBuf) channel.readOutbound();
        }
    }

}
