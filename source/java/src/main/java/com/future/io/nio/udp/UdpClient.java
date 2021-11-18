package com.future.io.nio.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;

import com.future.io.nio.NIOConfig;

public class UdpClient {

    public void run() {
        DatagramChannel channel = null;
        Scanner scanner = null;
        try {
            channel = DatagramChannel.open();
            ByteBuffer buffer = ByteBuffer.allocate(NIOConfig.getBufferSize());

            scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                buffer.put(scanner.next().getBytes());
                buffer.flip();
                channel.send(buffer, new InetSocketAddress(NIOConfig.getServerIP(), NIOConfig.getServerUdpPort()));
                buffer.clear();
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null)
                    channel.close();
                if (scanner != null)
                    scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new UdpClient().run();
    }

}
