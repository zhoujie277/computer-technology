package com.future.netty.codec;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

class DecoderStringReplay extends ReplayingDecoder<Status> {

    private int length;
    private byte[] content;

    DecoderStringReplay() {
        super(Status.Parse1);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
        case Parse1:
            length = in.readInt();
            content = new byte[length];
            checkpoint(Status.Parse2);
            break;
        case Parse2:
            in.readBytes(content, 0, length);
            out.add(new String(content, StandardCharsets.UTF_8));
            checkpoint(Status.Parse1);
            break;
        }
    }

}
