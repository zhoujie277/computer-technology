package com.future.io.nio.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.future.io.nio.NIOConfig;

public class MultiThreadEchoReactor {

    class Reactor implements Runnable {
        final Selector selector;

        public Reactor(Selector selector) {
            this.selector = selector;
        }

        void dispatch(SelectionKey key) {
            Runnable runnable = (Runnable) key.attachment();
            runnable.run();
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    selector.select(1000);
                    Set<SelectionKey> keySet = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keySet.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        dispatch(key);
                    }
                    keySet.clear();
                }

            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    class AcceptorHandler implements Runnable {
        @Override
        public void run() {
            try {
                SocketChannel channel = serverSocketChannel.accept();

                channel.configureBlocking(false);

                Selector selector = workSelectors[next.get()];

                // 唤醒选择,防止register时 boss线程被阻塞，netty 处理方式比较优雅，会在同一个线程注册事件，避免阻塞boss
                selector.wakeup();

                SelectionKey key = channel.register(selector, 0);

                key.attach(new MultiThreadEchoHandler(channel, key));

                key.interestOps(SelectionKey.OP_READ);

                // 唤醒 select， 使得 OP_READ 生效
                selector.wakeup();

                System.out.println("新连接注册完成");

                if (next.incrementAndGet() == workSelectors.length) {
                    next.set(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ServerSocketChannel serverSocketChannel;
    AtomicInteger next = new AtomicInteger();

    Selector bossSelector = null;
    Reactor bossReactor;

    Selector[] workSelectors = new Selector[2];
    Reactor[] workReactors;

    public void init() {
        try {
            bossSelector = Selector.open();

            workSelectors[0] = Selector.open();
            workSelectors[1] = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(NIOConfig.getServerPort());
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(address);

            SelectionKey key = serverSocketChannel.register(bossSelector, SelectionKey.OP_ACCEPT);
            key.attach(new AcceptorHandler());

            // 处理新连接的反应器
            bossReactor = new Reactor(bossSelector);

            // 第一个子反应器，一个子反应器负责一个选择器
            Reactor subReactor1 = new Reactor(workSelectors[0]);
            Reactor subReactor2 = new Reactor(workSelectors[1]);
            workReactors = new Reactor[] { subReactor1, subReactor2 };

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startService() {
        new Thread(bossReactor).start();
        new Thread(workReactors[0]).start();
        new Thread(workReactors[1]).start();
    }

    public static void main(String[] args) {
        MultiThreadEchoReactor server = new MultiThreadEchoReactor();
        server.init();
        server.startService();
    }
}
