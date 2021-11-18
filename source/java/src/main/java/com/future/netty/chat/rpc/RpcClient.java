package com.future.netty.chat.rpc;

import com.future.netty.chat.rpc.service.HelloService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClient {

    private void run() {
        RpcClientSession session = new RpcClientSession();
        session.run();
        HelloService service = session.getService(HelloService.class);
        String value = service.sayHello("zhangsan");
        log.debug("zhangsan sayhello {}", value);
        String value1 = service.sayHello("lisi");
        log.debug("lisi sayhello {}", value1);
    }

    public static void main(String[] args) {
        try {
            new RpcClient().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
