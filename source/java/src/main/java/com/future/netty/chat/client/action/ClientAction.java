package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.Message;

public interface ClientAction {

    void execute(Cookie cookie, Message pkg);
}
