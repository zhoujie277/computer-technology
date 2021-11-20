package com.future.netty.chat.client.command;

import java.util.Scanner;


import lombok.Data;

@Data
public class ClientCommandMenu implements BaseCommand {
    public static final String KEY = "0";
    private String allCommandsShow;
    private String commandInput;

    @Override
    public void exec(Scanner scanner) {
        System.err.println("请输入某个操作指令：");
        System.err.println(allCommandsShow);
        commandInput = scanner.next();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTip() {
        return "show 所有命令";
    }
}
