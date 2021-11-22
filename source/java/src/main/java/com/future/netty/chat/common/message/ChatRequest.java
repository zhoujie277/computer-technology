package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatRequest extends Request {
    private long msgId;
    private User from;
    private String toUser;
    private int contentType;
    private String content;
    private String property;
    private long time;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.CHAT_REQ;
    }

    public Message.ContentType contentType() {
        return Message.ContentType.values()[contentType];
    }

    public void setContentType(int type) {
        this.contentType = type;
    }

    public void setContentType(Message.ContentType type) {
        this.contentType = type.ordinal();
    }

}
