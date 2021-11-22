package com.future.netty.chat.client.command;

import java.util.Scanner;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class ClientCommandMenu implements BaseCommand {
    public static final String KEY = "0";
    private String allCommandsShow;
    private String commandInput;

    @Override
    public void exec(Scanner scanner) {
        log.info("请输入某个操作指令：");
        log.info(allCommandsShow);
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
