package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Pong;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PongAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        Pong msg = (Pong) pkg;
        log.info("recv pong msg: " + msg);
    }

}
