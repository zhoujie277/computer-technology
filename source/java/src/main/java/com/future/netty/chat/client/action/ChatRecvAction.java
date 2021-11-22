package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.ChatRequest;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatRecvAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        ChatRequest msg = (ChatRequest) pkg;
        log.info("收到消息:" + msg.getContent() + ", 来自: " + msg.getFrom());
    }

}
