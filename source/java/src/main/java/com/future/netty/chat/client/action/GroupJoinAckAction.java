package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.GroupJoinResponse;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupJoinAckAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        GroupJoinResponse msg = (GroupJoinResponse) pkg;
        log.info("收到 ack code :" + msg.getCode() + ", message: " + msg.getMsg());
        log.info("群组加入成功: " + msg.getGroup());
    }

}
