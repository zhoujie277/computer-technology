package com.future.netty.chat.common.codec;

import java.util.List;

import com.future.netty.chat.common.ProtoInstant;
import com.future.netty.chat.proto.ProtoMsg;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleProtobufDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();

        if (in.readableBytes() < 8) {
            return;
        }
        short magic = in.readShort();
        if (magic != ProtoInstant.MAGIC_CODE) {
            String error = "客户端口令不对:" + ctx.channel().remoteAddress();
            throw new Exception(error);
        }
        short version = in.readShort();
        log.debug("version {}", version);
        
        int length = in.readInt();
        if (length < 0) {
            // 非法数据，关闭连接
            ctx.close();
        }
        if (length > in.readableBytes()) {
            // 如果接收到的数据小于消息体本应该有的长度
            // 重置读取位置
            in.resetReaderIndex();
            return;
        }
        byte[] array;
        if (in.hasArray()) {
            // 堆缓冲，零拷贝
            ByteBuf slice = in.slice();
            array = slice.array();
        } else {
            // 直接缓冲，则拷贝
            array = new byte[length];
            in.readBytes(array, 0, length);
        }
        ProtoMsg.Message msg = ProtoMsg.Message.parseFrom(array);
        if (msg != null) {
            out.add(msg);
        }
    }

}
