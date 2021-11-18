package com.future.netty.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.future.util.NumberUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * 基于 Head-Content 协议的字符串分包解码器
 */
class HandlerStringProcess extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("process string: " + msg);
    }

    public static void main(String[] args) throws IOException {
        String content = "Hello world";
        EmbeddedChannel channel = new EmbeddedChannel(new DecoderStringReplay(), new HandlerStringProcess());
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        for (int j = 0; j < 100; j++) {
            // 1-3之间的随机数
            int random = NumberUtil.random(3);
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(bytes.length * random);
            for (int k = 0; k < random; k++) {
                buf.writeBytes(bytes);
            }
            channel.writeInbound(buf);
        }
        System.in.read();
    }
}
