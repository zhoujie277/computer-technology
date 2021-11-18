package com.future.io.nio.api;

import java.nio.ByteBuffer;

public class BufferApi {

    // 粘包半包演示
    public static void splitPacket() {
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("hello,world\nI'm zhangsan\nHo".getBytes());
        split(source);
        source.put("w are you?\n".getBytes());
        split(source);
    }

    public static void split(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 获取数据，但不读
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                System.out.println("length=" + length + ", position=" + source.position());
                ByteBuffer target = ByteBuffer.allocate(length);
                for (int j = 0; j < target.limit(); j++) {
                    // 从缓冲区读取数据
                    target.put(source.get());
                }
                System.out.println(new String(target.array(), 0, target.capacity()));
            }
        }
        // 压缩，将已经读过的数据丢弃，没有读过的数据往前挪
        source.compact();
    }

    public static void api() {
        // 分配一个缓冲区，容量设置为 10
        ByteBuffer buffer = ByteBuffer.allocate(12);
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // put 往缓冲区中添加数据
        String name = "hello world";
        buffer.put(name.getBytes());
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // buffer.flip
        buffer.flip();
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // get 数据的读取
        char ch = (char) buffer.get(); // 此处读一个字节
        System.out.println(ch);
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // clear 清除缓冲区的数据
        buffer.clear();
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // 直接缓冲区和非直接缓冲区
        ByteBuffer dfBuffer = ByteBuffer.allocateDirect(10);
        ByteBuffer nBuffer = ByteBuffer.allocate(10);
        System.out.println(dfBuffer.isDirect());
        System.out.println(nBuffer.isDirect());
    }

    public static void main(String[] args) {
        // api();
        splitPacket();
    }

}
