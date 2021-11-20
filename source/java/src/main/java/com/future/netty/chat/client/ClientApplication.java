package com.future.netty.chat.client;

public class ClientApplication {

    public static void main(String[] args) {
        Thread.currentThread().setName("CommandLineThread");
        System.out.println("正在连接服务器....");
        ClientSession.getSession().connectServer();
        if (ClientSession.getSession().isConnected()) {
            System.out.println("服务器连接失败，客户端退出");
            return;
        }
        System.out.println("服务器连接成功...");
        CommandController controller = new CommandController();
        controller.run();
    }

}
