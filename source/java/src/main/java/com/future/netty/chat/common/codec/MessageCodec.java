package com.future.netty.chat.common.codec;

import java.util.List;

import com.future.netty.chat.common.message.Message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageCodec;

@Sharable
public class MessageCodec extends MessageToMessageCodec<ByteBuf, Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        // 2 字节的魔数
        out.writeShort(Message.MAGIC_CODE);
        // 2 字节的版本
        out.writeShort(Message.VERSION_CODE);
        // 2 字节的消息类型
        out.writeShort(msg.getMessageType().getValue());
        // 序列化
        byte[] bytes = Serializer.getSerializer().serialize(msg);
        // 4 字节的长度
        out.writeInt(bytes.length);
        // 内容
        out.writeBytes(bytes);

        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 读取魔数
        in.readShort();
        // 读取版本
        in.readShort();
        int msgType = in.readShort();
        // 读取长度
        int length = in.readInt();
        byte[] bytes;
        if (in.hasArray()) {
            // 堆缓冲，零拷贝
            ByteBuf slice = in.slice();
            bytes = slice.array();
        } else {
            // 直接缓冲，则拷贝
            bytes = new byte[length];
            in.readBytes(bytes, 0, length);
        }
        // 反序列化
        Message msg = Serializer.getSerializer().deserialize(msgType, bytes);
        out.add(msg);
    }

}
