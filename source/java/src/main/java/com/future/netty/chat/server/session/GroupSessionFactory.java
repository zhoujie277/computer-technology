package com.future.netty.chat.server.session;

public class GroupSessionFactory {

    public static GroupSession getGroupSession() {
        return new GroupSessionMemoryImpl();
    }
}
