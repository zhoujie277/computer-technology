package com.future.netty.chat.protocol;

import java.util.List;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.message.LoginRequestMessage;
import com.future.netty.chat.message.Message;
import com.future.netty.chat.protocol.Serializer.Algorithm;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.logging.LoggingHandler;

@Sharable
@SuppressWarnings("unused")
public class MessageCodecSharable extends MessageToMessageCodec<ByteBuf, Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        System.out.println("encode.....");
        // 4 字节的魔数
        out.writeBytes(new byte[] { 1, 2, 3, 4 });
        // 1 字节的版本
        out.writeByte(1);
        // 1 字节的序列化方式 jdk 0， json 1
        Serializer.Algorithm algorithm = getCodecType();
        out.writeByte(algorithm.ordinal());
        // 1 字节的指令类型
        out.writeByte(msg.getMessageType());
        // 4 个字节的请求序号
        out.writeInt(msg.getSequenceId());
        // 无意义，对齐填充
        out.writeByte(0xFF);

        byte[] bytes = algorithm.serialize(msg);
        // 4 字节的长度
        out.writeInt(bytes.length);
        // 内容
        out.writeBytes(bytes);

        outList.add(out);
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

        Class<? extends Message> messageClass = Message.getMessageClass(msgType);
        Message msg = Serializer.Algorithm.values()[serializeType].deserialize(messageClass, bytes);
        System.out.println(msg);
        out.add(msg);
    }

    private static Serializer.Algorithm getCodecType() {
        String string = NIOConfig.get(NIOConfig.KEY_CODEC_TYPE);
        return Serializer.Algorithm.valueOf(string);
    }

    public static void main(String[] args) {
        MessageCodecSharable sharable = new MessageCodecSharable();
        // 测试 encode 方法
        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
                new LoggingHandler(), sharable);
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
