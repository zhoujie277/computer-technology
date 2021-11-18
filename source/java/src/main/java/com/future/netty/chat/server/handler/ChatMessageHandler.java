package com.future.netty.chat.server.handler;

import com.future.netty.chat.message.ChatRequestMessage;
import com.future.netty.chat.message.ChatResponseMessage;
import com.future.netty.chat.server.session.SessionFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class ChatMessageHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        log.debug("{}", msg);
        String toUser = msg.getTo();
        Channel toChannel = SessionFactory.getSession().getChannel(toUser);
        if (toChannel != null) {
            ChatResponseMessage responseMessage = new ChatResponseMessage(msg.getFrom(), msg.getContent());
            toChannel.writeAndFlush(responseMessage);
        } else {
            ctx.writeAndFlush(new ChatResponseMessage(false, "用户不在线"));
        }
    }

}
