package com.future.netty.chat.rpc.service;

public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "服务端ROBOT ：你好, " + name;
    }

}
