package com.future.io.nio.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.future.io.nio.NIOConfig;

/**
 * 单线程 Reactor 简单模型
 */
public class EchoServerReactor implements Runnable {

    class AcceptorHandler implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel channel = serverSocketChannel.accept();
                channel.configureBlocking(false);
                SelectionKey skey = channel.register(selector, 0);
                skey.attach(new EchoHandler(channel, skey));
                skey.interestOps(SelectionKey.OP_READ);
                selector.wakeup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    Selector selector;
    protected ServerSocketChannel serverSocketChannel;

    public void init() {
        try {
            // 获取服务端通道，准备监听
            serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(NIOConfig.getServerPort());
            serverSocketChannel.socket().bind(address);
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            key.attach(new AcceptorHandler());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey key) {
        Runnable attachment = (Runnable) key.attachment();
        attachment.run();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    dispatch(next);
                }
                selectedKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        EchoServerReactor reactor = new EchoServerReactor();
        reactor.init();
        new Thread(reactor).start();
    }

}
