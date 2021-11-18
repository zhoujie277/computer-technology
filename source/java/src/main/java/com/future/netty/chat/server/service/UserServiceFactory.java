package com.future.netty.chat.server.service;

public class UserServiceFactory {

    public static UserService getUserService() {
        return new UserServiceMemoryImpl();
    }
}
