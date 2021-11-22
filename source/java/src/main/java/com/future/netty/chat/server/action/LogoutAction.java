package com.future.netty.chat.server.action;

import com.future.netty.chat.common.message.LogoutResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.Session;
import com.future.netty.chat.server.session.SessionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogoutAction implements ServerAction {

    @Override
    public void execute(Session session, Message pkg) {
        log.debug("{}", pkg);
        // TODO: 释放资源，放入半连接队列
        session.release();
        SessionManager.getInstance().unbind(session.getChannel());
        session.writeMessage(new LogoutResponse(ResultCode.SUCCESS));
    }

}
