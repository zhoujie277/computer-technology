package com.future.io.nio.file;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.future.util.IOUtil;

/**
 * 分散聚集 I/O 操作的演示
 */
public class AggregateIO {

    public void copy() throws IOException {
        FileInputStream fis = new FileInputStream(IOUtil.getClassLoaderResourcePath("nio.properties"));
        FileChannel iChannel = fis.getChannel();
        FileOutputStream fos = new FileOutputStream(IOUtil.getClassLoaderResourcePath("tmp.txt"));
        FileChannel oChannel = fos.getChannel();

        ByteBuffer buffer1 = ByteBuffer.allocate(68);
        ByteBuffer buffer2 = ByteBuffer.allocate(1024);
        // 从通道中读取数据分散到各个缓冲区
        ByteBuffer[] buffers = { buffer1, buffer2 };
        iChannel.read(buffers);

        // 查看分散读取的数据
        for (ByteBuffer buffer : buffers) {
            buffer.flip();
            System.out.println(new String(buffer.array(), 0, buffer.remaining()));
        }

        // 聚集写入到通道
        oChannel.write(buffers);
        iChannel.close();
        oChannel.close();
        fis.close();
        fos.close();
        System.out.println("文件复制");
    }

    public static void main(String[] args) throws IOException {
        AggregateIO aggregateIO = new AggregateIO();
        aggregateIO.copy();
    }
}