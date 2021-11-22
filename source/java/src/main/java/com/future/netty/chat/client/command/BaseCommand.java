package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.Message;

public interface BaseCommand {

    void exec(Scanner scanner);

    String getKey();

    String getTip();

    default Message buildMessage(Cookie cookie) {
        return null;
    }

}
