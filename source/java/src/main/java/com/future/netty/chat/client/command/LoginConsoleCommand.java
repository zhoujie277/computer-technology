package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.LoginRequest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class LoginConsoleCommand implements BaseCommand {

    public static final String KEY = "1";

    private String userName;
    private String password;

    @Override
    public void exec(Scanner scanner) {
        log.info("请输入用户信息(id:password)  ");
        String[] info = null;
        while (true) {
            String input = scanner.next();
            info = input.split(":");
            if (info.length != 2) {
                log.info("请按照格式输入(id:password):");
            } else {
                break;
            }
        }
        userName = info[0];
        password = info[1];
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTip() {
        return "登录";
    }

    @Override
    public LoginRequest buildMessage(Cookie cookie) {
        LoginRequest request = new LoginRequest();
        request.setUsername(userName);
        request.setPassword(password);
        return request;
    }

}
