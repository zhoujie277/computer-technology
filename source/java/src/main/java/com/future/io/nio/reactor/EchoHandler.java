package com.future.io.nio.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class EchoHandler implements Runnable {
    final SocketChannel channel;
    final SelectionKey sk;
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    static final int RECIEVING = 0, SENDING = 1;
    int state = RECIEVING;

    public EchoHandler(SocketChannel channel, SelectionKey sk) {
        this.channel = channel;
        this.sk = sk;
    }

    @Override
    public void run() {
        try {
            if (state == SENDING) {
                channel.write(buffer);
                // 写完后，准备开始从通道读，bytebuffer 切换成写模式
                buffer.clear();
                sk.interestOps(SelectionKey.OP_READ);
                state = RECIEVING;
            } else if (state == RECIEVING) {
                int length = 0;
                while ((length = channel.read(buffer)) > 0) {
                    System.out.println(new String(buffer.array(), 0, length));
                }
                buffer.flip();
                sk.interestOps(SelectionKey.OP_WRITE);
                // 读完后，进入发送的状态
                state = SENDING;
            }
        } catch (IOException e) {
            e.printStackTrace();
            sk.cancel();
            try {
                channel.finishConnect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}
