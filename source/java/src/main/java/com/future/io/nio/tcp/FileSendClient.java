package com.future.io.nio.tcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import com.future.io.nio.NIOConfig;
import com.future.util.IOUtil;

public class FileSendClient {

    private static final int STATE_INITED = 1;
    private static final int STATE_WAIT_1 = 2; // 已发送完文件名长度+文件名，等待 第一个 ACK
    private static final int STATE_WAIT_1_ACK = 3; // 已发送完文件名长度+文件名，等待 第一个 ACK
    private static final int STATE_WAIT_2 = 4; // 已发送文件长度，等待第二个 ACK
    private static final int STATE_WAIT_2_ACK = 5; // 已发送文件长度，等待第二个 ACK
    private static final int STATE_WAIT_3 = 6; // 已发送文件，等待第三个 ACK
    private static final int STATE_WAIT_3_ACK = 7; // 已发送文件，等待第三个 ACK
    private static final int STATE_CLOSED = 9; // 关闭状态

    private int state = 0;

    public synchronized void setFTPState(int state) {
        this.state = state;
    }

    public synchronized void ackFTPState() {
        this.state++;
    }

    public synchronized int getFTPState() {
        return state;
    }

    private class ReaderThread extends Thread {
        private ByteBuffer buffer = ByteBuffer.allocate(NIOConfig.getBufferSizeSmall());
        private SocketChannel channel;

        ReaderThread(SocketChannel channel) {
            super("ClientReaderThread");
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                int ack = 0;
                int count = 0;
                buffer.clear();
                while (getFTPState() <= STATE_WAIT_3) {
                    System.out.println("等待读取服务器数据");
                    channel.read(buffer);
                    buffer.flip();
                    ack = buffer.getInt();
                    buffer.clear();
                    ackFTPState();
                    count++;
                    System.out.println("收到服务器发送的第 " + count + " 次 ACK = " + ack + ", ftpState=" + getFTPState());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SenderThread extends Thread {
        ByteBuffer buffer = ByteBuffer.allocate(NIOConfig.getBufferSize());
        private Charset charset = Charset.forName("UTF-8");
        private File file;
        private SocketChannel channel;

        SenderThread(SocketChannel channel, File file) {
            super("ClientSenderThread");
            this.file = file;
            this.channel = channel;
        }

        @Override
        public void run() {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                FileChannel fileChannel = fis.getChannel();
                // 将文件名编码
                ByteBuffer fileName = charset.encode(file.getName());

                // 发送文件名称
                buffer.clear();
                buffer.putInt(fileName.limit());
                buffer.put(fileName);
                buffer.flip();
                channel.write(buffer);
                setFTPState(FileSendClient.STATE_WAIT_1);
                System.out.println("已发送文件名，等待服务器 ACK");
                while (getFTPState() != STATE_WAIT_1_ACK)
                    ;

                // 发送文件长度
                buffer.clear();
                buffer.putLong(file.length());
                buffer.flip();
                channel.write(buffer);
                System.out.println("发送文件长度：limit=" + buffer.limit() + ", pos=" + buffer.position());
                setFTPState(FileSendClient.STATE_WAIT_2);
                while (getFTPState() != STATE_WAIT_2_ACK)
                    ;

                // 发送文件内容
                System.out.println("开始传输文件");
                System.out.println(file.getAbsolutePath());
                int length = 0;
                long progress = 0;
                buffer.clear();
                int count = 0;
                while ((length = fileChannel.read(buffer)) > 0) {
                    buffer.flip();
                    channel.write(buffer);
                    buffer.clear();
                    progress += length;
                    count++;
                    if (count == 6000 || length < buffer.capacity()) {
                        System.out.println("| " + (100 * progress / file.length()) + "% |");
                        count = 0;
                    }
                }
                if (length == -1) {
                    setFTPState(STATE_WAIT_3);
                    while (getFTPState() != STATE_WAIT_3_ACK)
                        ;
                    setFTPState(STATE_CLOSED);
                    // 关闭文件通道
                    IOUtil.close(fileChannel);
                    IOUtil.close(fis);
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFile() {
        try {
            String ip = NIOConfig.getServerIP();
            int port = NIOConfig.getServerPort();
            String srcPath = NIOConfig.getSocketSendFile();
            System.out.println("send file " + srcPath);

            File file = new File(srcPath);
            if (!file.exists()) {
                System.out.println("file not exists.");
                return;
            }
            setFTPState(FileSendClient.STATE_INITED);
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.socket().connect(new InetSocketAddress(ip, port));
            socketChannel.configureBlocking(true);
            while (!socketChannel.finishConnect())
                ;
            System.out.println("Client 成功连接服务器.");
            ReaderThread reader = new ReaderThread(socketChannel);
            SenderThread sender = new SenderThread(socketChannel, file);

            reader.start();
            sender.start();

            reader.join();
            sender.join();
            // 在socketchannel
            socketChannel.shutdownOutput();
            socketChannel.shutdownInput();
            IOUtil.close(socketChannel);
            System.out.println("--文件传输成功--");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FileSendClient client = new FileSendClient();
        client.sendFile();
    }

}
