package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class LogoutRequest extends Request {

    @Override
    public MsgType getMessageType() {
        return MsgType.LOGOUT_REQ;
    }

    @Getter
    @Setter
    private User user;

}
