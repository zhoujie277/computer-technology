package com.future.netty.chat.client.action;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.LoginResponse;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginAckAction implements ClientAction {

    @Override
    public void execute(Cookie cookie, Message pkg) {
        // 判断返回是否成功
        LoginResponse msg = (LoginResponse) pkg;
        if (msg.isSuccess()) {
            // 登录成功
            cookie.createSession(msg.getSequence(), msg.getSessionId(), msg.getUser(), msg.getOnlines());
            log.info("登录成功");
        } else {
            // send ui show msg...
            // 登录失败
            log.info(msg.getMsg());
        }
    }
}
