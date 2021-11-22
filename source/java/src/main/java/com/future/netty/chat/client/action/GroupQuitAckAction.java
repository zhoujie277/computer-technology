package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.GroupQuitResponse;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupQuitAckAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        GroupQuitResponse msg = (GroupQuitResponse) pkg;
        log.info("收到 ack code :" + msg.getCode() + ", message: " + msg.getMsg());
    }

}
