package com.future.netty.chat.client.command;

import java.util.Scanner;

import com.future.netty.chat.proto.ProtoMsg;

public interface BaseCommand {

    void exec(Scanner scanner);

    String getKey();

    String getTip();

    default ProtoMsg.Message buildMessage(ProtoMsg.Message.Builder builder) {
        return null;
    }

}
