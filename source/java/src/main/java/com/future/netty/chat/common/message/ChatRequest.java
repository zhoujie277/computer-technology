package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRequest extends Request {
    private long msgId;
    private User from;
    private String toUser;
    private Message.ContentType contentType;
    private String content;
    private String property;
    private long time;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.CHAT_REQ;
    }

}
