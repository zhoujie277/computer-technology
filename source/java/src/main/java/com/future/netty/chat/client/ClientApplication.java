package com.future.netty.chat.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientApplication {

    public static void main(String[] args) {
        Thread.currentThread().setName("CommandLineThread");
        log.info("正在连接服务器....");
        Cookie.getCookie().connectServer();
        if (!Cookie.getCookie().isConnected()) {
            log.info("服务器连接失败，客户端退出");
            return;
        }
        log.info("服务器连接成功...");
        CommandController controller = new CommandController();
        controller.run();
    }

}
