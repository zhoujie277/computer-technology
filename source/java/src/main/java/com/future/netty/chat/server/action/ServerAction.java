package com.future.netty.chat.server.action;

import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.server.session.Session;

public interface ServerAction {

    void execute(Session session, Message msg);

}
