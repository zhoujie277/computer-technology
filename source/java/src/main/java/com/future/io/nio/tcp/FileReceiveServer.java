package com.future.io.nio.tcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.future.io.nio.NIOConfig;
import com.future.util.FormatUtil;
import com.future.util.IOUtil;

public class FileReceiveServer {

    private static class Client {
        static final int ACK = 200;
        static final int STATE_CONNECTED = 1; // 等传输文件名
        static final int STATE_WAIT_1 = 2; // 等文件长度
        static final int STATE_WAIT_2 = 3; // 等传输文件
        static final int STATE_CLOSE_WAIT = 4; // 传输完毕，等待关闭，等待客户端发送ack确认
        static final int STATE_CLOSED = 5;

        String fileName;
        long filesize;
        long readedFilesize;
        long startTime;
        InetSocketAddress address;
        FileChannel fileChannel;
        int state;
        ByteBuffer inBuffer = ByteBuffer.allocate(NIOConfig.getBufferSizeLarge());
        ByteBuffer outBuffer = ByteBuffer.allocate(NIOConfig.getBufferSizeSmall());

        void setState(int state) {
            this.state = state;
        }

        boolean isConnectedState() {
            return STATE_CONNECTED == state;
        }

        boolean isWait1State() {
            return state == STATE_WAIT_1;
        }

        boolean isWait2State() {
            return state == STATE_WAIT_2;
        }

        boolean isWaitCloseState() {
            return state == STATE_CLOSE_WAIT;
        }

        void ack(SocketChannel channel) throws IOException {
            outBuffer.clear();
            outBuffer.putInt(ACK);
            outBuffer.flip();
            channel.write(outBuffer);
            System.out.println("write ACK=" + ACK);
        }
    }

    private Charset charset = Charset.forName("UTF-8");

    private Map<SelectableChannel, Client> clientMap = new HashMap<>();

    @SuppressWarnings("all")
    private void processData(SelectionKey key) {
        Client client = clientMap.get(key.channel());
        SocketChannel clieChannel = (SocketChannel) key.channel();
        int num = 0;
        ByteBuffer buffer = client.inBuffer;
        try {
            buffer.clear();
            while ((num = clieChannel.read(buffer)) > 0) {
                if (client.isConnectedState()) {
                    System.out.println("processData buffer position=" + buffer.position());
                    // 文件名长度，如果字节数不够，继续读
                    buffer.flip();
                    String directory = NIOConfig.getServerReceivePath();
                    // 客户端发送过来的，首先是文件名长度 + 文件名，根据文件名，创建服务器端的文件，将文件通道保存到客户端
                    int len = buffer.getInt();
                    byte fileName[] = new byte[len];
                    // 文件名长度受缓冲区大小限制，可作为协议约定。文件名不能太长。
                    // 由于文件名不得超过缓冲区大小，否则缓冲区一次读取不完文件名，会导致 len > buffer.remain(), 将引发
                    // BufferUnderflow异常。
                    buffer.get(fileName);
                    // 客户端发送过来的，其次是文件长度
                    client.fileName = new String(fileName, charset);
                    File file = new File(directory, client.fileName);
                    System.out.println("NIO 接收目标文件：" + file.getAbsolutePath());
                    buffer.clear();
                    client.fileChannel = new FileOutputStream(file).getChannel();
                    // 给客户端回一个 ack
                    client.setState(Client.STATE_WAIT_1);
                    client.ack(clieChannel);
                    continue;
                }

                // 等待接收文件的状态，前8个字节为长度。
                // 这里作为两次传输，其实可以作为一次传输，可节省服务器一次回执。但状态不能省去。
                // 方法是，处理完了文件长度，立即修改状态，同时如果缓冲区还有剩余数据，则将剩余数据写入文件，然后清空缓冲区，重新读取下一次输入。
                if (client.isWait1State()) {
                    System.out.println("processData buffer position=" + buffer.position());
                    buffer.flip();
                    client.filesize = buffer.getLong();
                    client.startTime = System.currentTimeMillis();
                    // 给客户端回一个 ack
                    client.setState(Client.STATE_WAIT_2);
                    client.ack(clieChannel);
                    System.out.println("NIO 传输开始 filesize:" + client.filesize);
                    buffer.clear();
                    continue;
                }
                if (client.isWait2State()) {
                    // 客户端发送过来的 ，最后是文件内容，写入文件内容
                    buffer.flip();
                    client.readedFilesize += buffer.limit();
                    client.fileChannel.write(buffer);
                    buffer.clear();
                    if (client.readedFilesize >= client.filesize) {
                        long usedTime = System.currentTimeMillis() - client.startTime;
                        System.out.println("上传完毕");
                        System.out.printf("filename=%s, readsize=%s, filesize=%s, used time=%d \n", client.fileName,
                                FormatUtil.getFormatFileSize(client.readedFilesize),
                                FormatUtil.getFormatFileSize(client.filesize), usedTime);
                        client.setState(Client.STATE_CLOSE_WAIT);
                        client.ack(clieChannel); // 接收完毕
                    }
                    continue;
                }
                if (client.isWaitCloseState()) {
                    System.out.println("wait client closed....");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (num == -1) {
            key.cancel();
            client.setState(Client.STATE_CLOSED);
            IOUtil.close(client.fileChannel);
            IOUtil.close(clieChannel);
            System.out.println("client exited....");
        }
    }

    public void start() {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverChannel.socket();
            serverChannel.configureBlocking(false);
            InetSocketAddress address = new InetSocketAddress(NIOConfig.getServerPort());
            serverSocket.bind(address);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("serverChannel is listening...");
            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = server.accept();
                        if (clientChannel == null)
                            continue;
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        Client client = new Client();
                        client.setState(Client.STATE_CONNECTED);
                        client.address = (InetSocketAddress) clientChannel.getRemoteAddress();
                        clientMap.put(clientChannel, client);
                        System.out.println(client.address + " 连接成功.");
                    } else if (key.isReadable()) {
                        processData(key);
                    } else if (key.isWritable()) {
                        System.out.println("......writeable......................");
                        // writeACK(key);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    public static void main(String[] args) {
        FileReceiveServer server = new FileReceiveServer();
        server.start();
    }

}
