package com.future.netty.chat.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.future.netty.chat.client.command.BaseCommand;
import com.future.netty.chat.client.command.ChatConsoleCommand;
import com.future.netty.chat.client.command.ClientCommandMenu;
import com.future.netty.chat.client.command.LoginConsoleCommand;
import com.future.netty.chat.client.command.LogoutConsoleCommand;
import com.future.netty.chat.common.message.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandController {

    private final ClientCommandMenu clientCommandMenu = new ClientCommandMenu();
    private final LoginConsoleCommand loginConsoleCommand = new LoginConsoleCommand();
    private final ChatConsoleCommand chatConsoleCommand = new ChatConsoleCommand();
    private final LogoutConsoleCommand logoutConsoleCommand = new LogoutConsoleCommand();

    private final Map<String, BaseCommand> commandMap = new HashMap<>();;

    public CommandController() {
        initCommandMap();
    }

    public void initCommandMap() {
        commandMap.put(clientCommandMenu.getKey(), clientCommandMenu);
        commandMap.put(loginConsoleCommand.getKey(), loginConsoleCommand);
        commandMap.put(logoutConsoleCommand.getKey(), logoutConsoleCommand);
        commandMap.put(chatConsoleCommand.getKey(), chatConsoleCommand);

        Set<Map.Entry<String, BaseCommand>> entrys = commandMap.entrySet();
        Iterator<Map.Entry<String, BaseCommand>> iterator = entrys.iterator();
        StringBuilder menus = new StringBuilder();
        menus.append("[menu] ");
        while (iterator.hasNext()) {
            BaseCommand next = iterator.next().getValue();
            menus.append(next.getKey()).append("->").append(next.getTip()).append(" | ");
        }
        clientCommandMenu.setAllCommandsShow(menus.toString());
    }

    public void run() {
        Cookie cookie = Cookie.getCookie();
        BaseCommand command = null;
        while (!(command instanceof LogoutConsoleCommand)) {
            Scanner scanner = new Scanner(System.in);
            clientCommandMenu.exec(scanner);
            String key = clientCommandMenu.getCommandInput();
            command = commandMap.get(key);
            if (null == command) {
                log.info("无法识别[" + command + "]指令，请重新输入!");
                continue;
            }
            command.exec(scanner);
            Message message = command.buildMessage(cookie);
            if (message != null)
                cookie.writeMessage(message);
        }
    }

}
