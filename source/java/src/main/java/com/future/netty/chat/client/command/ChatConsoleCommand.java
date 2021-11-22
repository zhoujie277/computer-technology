package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.ChatRequest;
import com.future.netty.chat.common.message.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ChatConsoleCommand implements BaseCommand {
    private String toUserName;
    private String message;
    public static final String KEY = "2";

    @Override
    public void exec(Scanner scanner) {
        log.info("请输入聊天的消息(id:message)：");
        String[] info = null;
        while (true) {
            String input = scanner.next();
            info = input.split(":");
            if (info.length != 2) {
                log.info("请输入聊天的消息(id:message):");
            } else {
                break;
            }
        }
        toUserName = info[0];
        message = info[1];
    }

    @Override
    public Message buildMessage(Cookie cookie) {
        ChatRequest request = new ChatRequest();
        request.setContent(message);
        request.setToUser(toUserName);
        request.setContentType(Message.ContentType.TEXT);
        request.setFrom(cookie.getUser());
        request.setTime(System.currentTimeMillis());
        return request;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTip() {
        return "聊天";
    }

}
