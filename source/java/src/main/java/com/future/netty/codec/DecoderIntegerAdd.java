package com.future.netty.codec;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ReplayingDecoder;

/**
 * 分阶段解析的整数相加解码器 <br/>
 * 演示从Replaying中读取bytebuf 数据 并转换从想要的对象
 */
class DecoderIntegerAdd extends ReplayingDecoder<Status> {

    private Integer first;
    private Integer second;

    DecoderIntegerAdd() {
        super(Status.Parse1);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
        case Parse1:
            first = in.readInt();
            checkpoint(Status.Parse2);
            break;
        case Parse2:
            second = in.readInt();
            int sum = first + second;
            out.add(sum);
            checkpoint(Status.Parse1);
            break;
        }
    }

    public static void main(String[] args) throws IOException {
        EmbeddedChannel channel = new EmbeddedChannel(new DecoderIntegerAdd(), new HandlerIntegerProcess());
        for (int i = 0; i < 100; i++) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(i);
            channel.writeInbound(buf);
        }
        System.in.read();
    }

}
