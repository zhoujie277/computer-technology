package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.LogoutRequest;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogoutConsoleCommand implements BaseCommand {
    public static final String KEY = "10";

    @Override
    public void exec(Scanner scanner) {
        log.info("client will logout!");
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTip() {
        return "退出";
    }

    @Override
    public Message buildMessage(Cookie cookie) {
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setUser(cookie.getUser());
        return logoutRequest;
    }

}
