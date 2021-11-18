package com.future.io.nio.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import com.future.io.nio.NIOConfig;

public class UdpServer {

    public void run() {
        Selector selector = null;
        DatagramChannel datagramChannel = null;
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.bind(new InetSocketAddress(NIOConfig.getServerUdpPort()));

            System.out.println("udp server listening...");
            selector = Selector.open();
            datagramChannel.register(selector, SelectionKey.OP_READ);
            while (selector.select() > 0) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                ByteBuffer buffer = ByteBuffer.allocate(NIOConfig.getBufferSize());
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    if (next.isReadable()) {
                        SocketAddress client = datagramChannel.receive(buffer);
                        buffer.flip();
                        System.out.println("client :" + client);
                        System.out.println(new String(buffer.array(), 0, buffer.remaining()));
                        buffer.clear();
                    }
                }
                iterator.remove();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (selector != null)
                    selector.close();
                if (datagramChannel != null)
                    datagramChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new UdpServer().run();
    }
}
