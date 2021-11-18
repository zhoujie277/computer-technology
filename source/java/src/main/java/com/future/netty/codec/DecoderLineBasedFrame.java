package com.future.netty.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.future.util.NumberUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * 基于换行的数据帧解码器演示 <br/>
 * 该解码器以 \n 或者 \r\n 作为分隔符
 */
class DecoderLineBasedFrame {

    public void run() {
        String content = "Hello DecoderLengthFieldBasedFrame.";
        String spliter = "\r\n";
        EmbeddedChannel channel = new EmbeddedChannel(new LineBasedFrameDecoder(1024), new StringDecoder(),
                new HandlerStringProcess());
        for (int i = 0; i < 100; i++) {
            int random = NumberUtil.random(3);
            ByteBuf buf = Unpooled.buffer();
            for (int k = 0; k < random; k++) {
                buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));
            }
            buf.writeBytes(spliter.getBytes(StandardCharsets.UTF_8));
            channel.writeInbound(buf);
        }

    }

    public static void main(String[] args) throws IOException {
        new DecoderLineBasedFrame().run();
        System.in.read();
    }
}
