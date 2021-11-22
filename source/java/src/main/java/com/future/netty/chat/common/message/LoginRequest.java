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
public class LoginRequest extends Request {
    private static final long serialVersionUID = 1905122041950251217L;

    private String username;
    private String password;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.LOGIN_REQ;
    }
}
