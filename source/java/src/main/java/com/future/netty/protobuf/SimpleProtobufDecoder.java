package com.future.netty.protobuf;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SimpleProtobufDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();

        if (in.readableBytes() < 8) {
            return;
        }
        short magic = in.readShort();
        if (magic != ProtoBufConstant.MAGIC_CODE) {
            String error = "客户端口令不对:" + ctx.channel().remoteAddress();
            throw new IllegalArgumentException(error);
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
        ProtoBufMsgSample.Msg msg = ProtoBufMsgSample.Msg.parseFrom(array);
        if (msg != null) {
            out.add(msg);
        }
    }

}
