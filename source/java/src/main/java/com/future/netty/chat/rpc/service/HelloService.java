package com.future.netty.chat.rpc.service;

public interface HelloService extends RPCService {

    String sayHello(String name);
}
