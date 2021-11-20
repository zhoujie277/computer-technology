package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.client.ClientSession;
import com.future.netty.chat.common.bean.ChatMsg;
import com.future.netty.chat.common.bean.ChatUser;
import com.future.netty.chat.proto.ProtoMsg;

import lombok.Data;

@Data
public class ChatConsoleCommand implements BaseCommand {
    private String toUserId;
    private String message;
    public static final String KEY = "2";

    @Override
    public void exec(Scanner scanner) {
        System.out.print("请输入聊天的消息(id:message)：");
        String[] info = null;
        while (true) {
            String input = scanner.next();
            info = input.split(":");
            if (info.length != 2) {
                System.out.println("请输入聊天的消息(id:message):");
            } else {
                break;
            }
        }
        toUserId = info[0];
        message = info[1];
    }

    public ProtoMsg.Message buildMessage(ProtoMsg.Message.Builder builder) {
        ChatUser user = ClientSession.getUser();
        ChatMsg msg = new ChatMsg(user);
        msg.setContent(message);
        msg.setMsgType(ChatMsg.MSGTYPE.TEXT);
        msg.setTo(toUserId);
        msg.setMsgId(System.currentTimeMillis());
        ProtoMsg.MessageRequest.Builder lb = ProtoMsg.MessageRequest.newBuilder();
        msg.fillMsg(lb);
        builder.setType(ProtoMsg.HeadType.MESSAGE_REQUEST);
        builder.setMsgRequest(lb);
        return builder.build();
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
