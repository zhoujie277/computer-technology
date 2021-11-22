package com.future.netty.chat.server.action;

import com.future.netty.chat.common.message.ChatRequest;
import com.future.netty.chat.common.message.ChatResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.Session;
import com.future.netty.chat.server.session.SessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 纯粹的转发消息后续补充
 * 
 * @author future
 */
@Slf4j
public class ChatAction implements ServerAction {

    @Override
    public void execute(Session session, Message pkg) {
        log.debug("{}", pkg);
        ChatRequest msg = (ChatRequest) pkg;
        String toUserName = msg.getToUser();
        User toUser = new User();
        toUser.setName(toUserName);
        Session toSession = SessionManager.getInstance().getSession(toUser);
        if (toSession != null) {
            // 消息应答
            ChatResponse response = new ChatResponse(ResultCode.SUCCESS);
            session.writeMessage(response);
            // 转发消息
            // TODO: 纯粹的转发消息，应当专门的解码处理器来做，定义一个转发类型。如果是转发消息，校验可靠后，即可马上转发，不需要服务器完全解码
            toSession.writeMessage(msg);
        } else {
            session.writeMessage(new ChatResponse(ResultCode.USER_OUTLINE));
        }
    }

}
