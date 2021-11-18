package com.future.netty.chat.message;

public class GroupChatResponseMessage extends AbstractResponseMessage {
    private String from;
    private String content;

    public GroupChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    public GroupChatResponseMessage(String from, String content) {
        super(true, null);
        this.from = from;
        this.content = content;
    }

    @Override
    public int getMessageType() {
        return GroupChatResponseMessage;
    }

    @Override
    public String toString() {
        return "GroupChatResponseMessage [content=" + content + ", from=" + from + "]";
    }

}
