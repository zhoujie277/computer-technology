package com.future.netty.chat.message;

import lombok.ToString;

@ToString(callSuper = true)
public class ChatResponseMessage extends AbstractResponseMessage {
    private String from;
    private String content;

    public ChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    public ChatResponseMessage(String from, String content) {
        super(true, null);
        this.from = from;
        this.content = content;
    }

    @Override
    public int getMessageType() {
        return ChatResponseMessage;
    }
}
