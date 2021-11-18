package com.future.netty.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.StringUtil;

public class ByteBufPrint {

    public static void log(ByteBuf buf) {
        int length = buf.readableBytes();
        int rows = length / 16 + (length % 16 == 0 ? 0 : 1) + 4;
        StringBuilder builder = new StringBuilder(rows * 80 * 2);
        builder.append("read index: ").append(buf.readerIndex()).append(" write index: ").append(buf.writerIndex())
                .append(" capacity: ").append(buf.capacity()).append(StringUtil.NEWLINE);
        ByteBufUtil.appendPrettyHexDump(builder, buf);
        System.out.println(builder.toString());
    }

}
