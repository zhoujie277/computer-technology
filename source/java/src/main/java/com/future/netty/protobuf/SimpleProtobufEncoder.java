package com.future.netty.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

class SimpleProtobufEncoder extends MessageToByteEncoder<ProtoBufMsgSample.Msg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoBufMsgSample.Msg msg, ByteBuf out) throws Exception {
        out.writeShort(ProtoBufConstant.MAGIC_CODE);
        out.writeShort(ProtoBufConstant.VERSION_CODE);
        byte[] bytes = msg.toByteArray();
        int length = bytes.length;
        out.writeInt(length);
        out.writeBytes(bytes);
    }
}
