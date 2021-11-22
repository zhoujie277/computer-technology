package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.ChatResponse;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatAckAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        ChatResponse msg = (ChatResponse) pkg;
        log.info("chat ACK:" + msg.getCode() + " -> " + msg.getMsg());
    }

}
