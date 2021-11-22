package com.future.netty.chat.server.action;

import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Pong;
import com.future.netty.chat.server.session.Session;

public class PingAction implements ServerAction {

    @Override
    public void execute(Session session, Message msg) {
        // 客户端只有登录后，才给客户端回心跳消息
        Pong message = new Pong();
        session.writeMessage(message);
    }

}
