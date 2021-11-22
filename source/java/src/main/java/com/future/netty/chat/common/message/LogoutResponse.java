package com.future.netty.chat.common.message;

import lombok.ToString;

@ToString
public class LogoutResponse extends Response {

    public LogoutResponse(ResultCode resultCode) {
        super(resultCode);
    }

    @Override
    public MsgType getMessageType() {
        return MsgType.LOGOUT_ACK;
    }

}
