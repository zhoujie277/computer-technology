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
 * Selector 产生的事件要么被处理，要么被cancel，否则下一次循环依旧会带上来。
 */
public class SelectorApi implements Runnable {

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
                        System.out.println("accept..." + key);
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel client = channel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        System.out.println(client);
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        int rdcount = channel.read(buffer);
                        System.out.println("read..." + rdcount);
                        if (rdcount == -1) {
                            key.cancel();
                        } else if (rdcount > 0) {
                            buffer.flip();
                            System.out.println(new String(buffer.array(), 0, buffer.limit()));
                            buffer.clear();
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SelectorApi().run();
    }
}
