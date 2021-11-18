package com.future.netty.chat.server.session;

public class SessionFactory {

    public static Session getSession() {
        return new SessionMemoryImpl();
    }
}
