package com.future.netty.chat.common.message;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class LoginResponse extends Response {

    public LoginResponse(ResultCode code) {
        super(code);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.LOGIN_ACK;
    }

    private User user;

    private List<User> onlines = new ArrayList<>();

    public void addUser(User user) {
        onlines.add(user);
    }

}
