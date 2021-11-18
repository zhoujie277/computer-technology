package com.future.netty.chat.server.handler;

import com.future.netty.chat.message.LoginRequestMessage;
import com.future.netty.chat.message.LoginResponseMessage;
import com.future.netty.chat.server.service.UserServiceFactory;
import com.future.netty.chat.server.session.SessionFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class LoginMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        log.debug("{}", msg);
        final String username = msg.getUsername();
        final String password = msg.getPassword();
        final boolean login = UserServiceFactory.getUserService().login(username, password);
        LoginResponseMessage responseMessage;
        if (login) {
            SessionFactory.getSession().bind(ctx.channel(), username); // 用户 、channel 简历关系
            responseMessage = new LoginResponseMessage(true, "登录成功！");
        } else {
            responseMessage = new LoginResponseMessage(false, "用户名或密码错误！");
        }
        // 登录结果 返回
        // 【 【当前节点】 开始向上 找 "出站Handler" (ch.writeAndFlush 和 ctx.channel().write(msg)
        // 从尾部向上查找)】
        ctx.writeAndFlush(responseMessage);

    }

}
