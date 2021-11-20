package com.future.netty.chat.common.codec;

import com.future.netty.chat.common.ProtoInstant;
import com.future.netty.chat.proto.ProtoMsg;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class SimpleProtobufEncoder extends MessageToByteEncoder<ProtoMsg.Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoMsg.Message msg, ByteBuf out) throws Exception {
        out.writeShort(ProtoInstant.MAGIC_CODE);
        out.writeShort(ProtoInstant.VERSION_CODE);
        byte[] bytes = msg.toByteArray();
        int length = bytes.length;
        out.writeInt(length);
        out.writeBytes(bytes);
    }
}
