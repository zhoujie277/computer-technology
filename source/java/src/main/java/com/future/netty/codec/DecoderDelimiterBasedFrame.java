package com.future.netty.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.future.util.NumberUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * 基于自定义分隔符的帧解码器演示 <br/>
 */
class DecoderDelimiterBasedFrame {

    public void run() {
        String content = "Hello DecoderDelimiterBasedFrame!";
        String spliter2 = "\t";
        final ByteBuf delimiter = Unpooled.copiedBuffer(spliter2.getBytes(StandardCharsets.UTF_8));
        DelimiterBasedFrameDecoder delimiterDecoder = new DelimiterBasedFrameDecoder(1024, delimiter);
        EmbeddedChannel channel = new EmbeddedChannel(delimiterDecoder, new StringDecoder(),
                new HandlerStringProcess());
        for (int j = 0; j < 100; j++) {
            // 1-3之间的随机数
            int random = NumberUtil.random(3);
            ByteBuf buf = Unpooled.buffer();
            for (int k = 0; k < random; k++) {
                buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));
            }
            buf.writeBytes(spliter2.getBytes(StandardCharsets.UTF_8));
            channel.writeInbound(buf);
        }
    }

    public static void main(String[] args) throws IOException {
        new DecoderDelimiterBasedFrame().run();
        System.in.read();
    }

}
