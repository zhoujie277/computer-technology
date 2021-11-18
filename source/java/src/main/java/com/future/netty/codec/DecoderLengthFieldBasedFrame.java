package com.future.netty.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.naming.ldap.LdapContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

class DecoderLengthFieldBasedFrame {

    public void run() {
        String content = "Hello LengthFieldBasedFrameDecoder";
        LengthFieldBasedFrameDecoder lDecoder = new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4);
        EmbeddedChannel channel = new EmbeddedChannel(lDecoder, new StringDecoder(), new HandlerStringProcess());
        for (int j = 1; j <= 100; j++) {
            ByteBuf buf = Unpooled.buffer();
            String s = j + "次发送->" + content;
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            System.out.println("bytes length = " + bytes.length);
            buf.writeBytes(bytes);
            channel.writeInbound(buf);
        }
    }

    public static void main(String[] args) throws IOException {
        new DecoderLengthFieldBasedFrame().run();
        System.in.read();
    }
}
