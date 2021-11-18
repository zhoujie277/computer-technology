package com.future.io.aio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class AIOFileChannel {

    public void run() {

        try {
            // 相对路径：相对于 程序运行的目录，此处是 computer_technology
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get("source/java/src/main/resources/nio.properties"),
                    StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate(256);
            channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {

                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    System.out.println(new String(attachment.array(), 0, attachment.limit()));
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    exc.printStackTrace();
                }

            });
            System.out.println("read invoked..");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new AIOFileChannel().run();
    }

}
