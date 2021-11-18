package com.future.io.nio.api;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.future.io.nio.NIOConfig;

/**
 * 网络通信普遍存在的半包粘包问题
 * 此示例展示了用最基础的attachment + bytebuffer 解决消息边界问题
 */
public class AttachmentApi implements Runnable {

    @Override
    public void run() {
        ServerSocketChannel server;
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            // server.setOption(name, value)
            server.bind(new InetSocketAddress(NIOConfig.getServerPort()));
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    // 处理完之后需要手动移除，如果不移除，下一次selectionkeys 还会有它
                    iterator.remove();
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel client = channel.accept();
                        System.out.println(client);
                        client.configureBlocking(false);
                        SelectionKey aSelectionKey = client.register(selector, SelectionKey.OP_READ);
                        ByteBuffer buffer = ByteBuffer.allocate(4);
                        aSelectionKey.attach(buffer);
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        int rdcount = channel.read(buffer);
                        System.out.println("read..." + rdcount);
                        if (rdcount == -1) {
                            key.cancel();
                        } else if (rdcount > 0) {
                            if (buffer.capacity() == buffer.position()) {
                                // 简单的扩容策略，处理消息边界问题
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                buffer.flip();
                                newBuffer.put(buffer);
                                key.attach(newBuffer);
                            }
                            System.out.println(new String(buffer.array(), 0, buffer.limit()));
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new AttachmentApi().run();
    }

}
