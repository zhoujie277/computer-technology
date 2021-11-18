package com.future.netty.api;

import java.util.concurrent.Callable;

import com.future.netty.utils.ByteBufPrint;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

public class ByteBufApi implements Callable<Object> {

    @Override
    public Object call() throws Exception {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' });
        buf.readByte();
        ByteBufPrint.log(buf);
        System.out.println();
        // 切片过程中，没有发生数据复制
        ByteBuf f1 = buf.slice(buf.readerIndex(), 5);
        ByteBuf f2 = buf.slice(5, 5);
        ByteBufPrint.log(f1);
        System.out.println();

        ByteBufPrint.log(f2);
        System.out.println();

        f1.setByte(0, 'b');
        ByteBufPrint.log(f1);
        System.out.println();
        ByteBufPrint.log(buf);

        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer();
        buf2.writeBytes(new byte[] { 1, 2, 3, 4, 5 });
        ByteBuf buf3 = ByteBufAllocator.DEFAULT.buffer();
        buf3.writeBytes(new byte[] { 6, 7, 8, 9, 10 });
        System.out.println();
        // ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        // buffer.writeBytes(buf2).writeBytes(buf3); // 两次复制
        // ByteBufPrint.log(buffer);
        ByteBufPrint.log(buf2);
        ByteBufPrint.log(buf3);
        CompositeByteBuf compositeBuffer = ByteBufAllocator.DEFAULT.compositeBuffer();
        compositeBuffer.addComponents(true, buf2, buf3); // 0 复制
        System.out.println();
        ByteBufPrint.log(compositeBuffer);
        return null;
    }

    public static void main(String[] args) throws Exception {
        new ByteBufApi().call();
    }
}
