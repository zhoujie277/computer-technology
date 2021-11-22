package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.GroupChatRequest;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupChatRecvAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        GroupChatRequest msg = (GroupChatRequest) pkg;
        log.info("收到消息:" + msg.getContent() + ", 来自群: " + msg.getGroupID() + ", 来自用户: " + msg.getFromUser());
    }

}
