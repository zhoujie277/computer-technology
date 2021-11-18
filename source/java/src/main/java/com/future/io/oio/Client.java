package com.future.io.oio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public void start() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(OIOConfig.getServerIP(), OIOConfig.getServerPort()));
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();
            OutputStream outStream = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(outStream);
            System.out.println(nextLine);
            pw.println(nextLine);
            pw.flush();
        }
        socket.close();
        scanner.close();
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.start();
    }
}
