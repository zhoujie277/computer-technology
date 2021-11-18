package com.future.netty.chat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.future.netty.chat.message.LoginRequestMessage;
import com.future.netty.chat.message.Message;
import com.future.netty.utils.MyLogHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

@SuppressWarnings("unused")
/**
 * 继承自 ByteToMessageCodec 不能被标识为 @Sharable
 */
public class MessageCodec extends ByteToMessageCodec<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        System.out.println("encode.....");
        // 4 字节的魔数
        out.writeBytes(new byte[] { 1, 2, 3, 4 });
        // 1 字节的版本
        out.writeByte(1);
        // 1 字节的序列化方式 jdk 0， json 1
        out.writeByte(0);
        // 1 字节的指令类型
        out.writeByte(msg.getMessageType());
        // 4 个字节的请求序号
        out.writeInt(msg.getSequenceId());
        // 无意义，对齐填充
        out.writeByte(0xFF);

        // 获取内容的字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        byte[] bytes = baos.toByteArray();
        // 4 字节的长度
        out.writeInt(bytes.length);
        // 内容
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("decode...");
        int magicNum = in.readInt();
        byte version = in.readByte();
        byte serializeType = in.readByte();
        int msgType = in.readByte();
        int sequenceId = in.readInt();
        in.readByte();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        if (serializeType == 0) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Message msg = (Message) ois.readObject();
            System.out.println(msg);
            out.add(msg);
        }
    }

    public static void main(String[] args) {
        // 测试 encode 方法
        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
                new MyLogHandler(), new MessageCodec());
        LoginRequestMessage msg = new LoginRequestMessage("zhangsan", "123");
        channel.writeOutbound(msg);

        // 测试 decode 方法
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        try {
            new MessageCodec().encode(null, msg, buffer);
            channel.writeInbound(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
