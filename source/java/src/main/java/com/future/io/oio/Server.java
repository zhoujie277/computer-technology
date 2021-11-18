package com.future.io.oio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 阻塞 I/O 线程池模型
 */
public class Server {

    private class Client implements Runnable {

        private Socket socket;

        public Client(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = null;
                InputStream inputStream = socket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String msg;
                System.out.println("server listenr...");
                while ((msg = reader.readLine()) != null) {
                    System.out.println(msg);
                }
                System.out.println("client exit...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(OIOConfig.getServerIP(), OIOConfig.getServerPort()));
        while (!Thread.interrupted()) {
            Socket client = socket.accept();
            System.out.println("client connect: " + client.getInetAddress());
            executorService.execute(new Client(client));
        }
        socket.close();
        System.out.println("server exit.......");
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.start();
    }
}
