package com.future.netty.chat.server.action;

import java.util.Set;

import com.future.netty.chat.common.message.LoginRequest;
import com.future.netty.chat.common.message.LoginResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.service.UserServiceFactory;
import com.future.netty.chat.server.session.SessionManager;
import com.future.netty.chat.server.session.Session;

import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class LoginAction implements ServerAction {
    private Channel mChannel;

    public LoginAction(Channel channel) {
        this.mChannel = channel;
    }

    @Override
    public void execute(Session s, Message pkg) {
        log.debug("{}", pkg);
        LoginRequest msg = (LoginRequest) pkg;
        final User user = UserServiceFactory.getUserService().login(msg.getUsername(), msg.getPassword());
        LoginResponse response;
        if (user != null) {
            Session session = SessionManager.getInstance().bind(mChannel, user);
            response = new LoginResponse(ResultCode.SUCCESS);
            response.setUser(user);
            Set<User> onlineUsers = SessionManager.getInstance().getOnlineUsers();
            for (User onlineUser : onlineUsers) {
                response.addUser(onlineUser);
            }
            session.writeMessage(response);
        } else {
            // 暂时简单处理.
            // 应补充 重试次数，调节空闲读时间及其相关资源处理，以防浪费连接
            response = new LoginResponse(ResultCode.AUTH_FAILED);
            mChannel.writeAndFlush(response);
        }
    }
}
