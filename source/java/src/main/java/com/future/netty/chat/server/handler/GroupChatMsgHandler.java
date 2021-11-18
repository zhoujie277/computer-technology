package com.future.netty.chat.server.handler;

import java.util.List;

import com.future.netty.chat.message.GroupChatRequestMessage;
import com.future.netty.chat.message.GroupChatResponseMessage;
import com.future.netty.chat.server.session.GroupSession;
import com.future.netty.chat.server.session.GroupSessionFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class GroupChatMsgHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        log.debug("{}", msg);
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        List<Channel> memberChannels = groupSession.getMemberChannels(msg.getGroupName());
        for (Channel channel : memberChannels) {
            channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
    }

}
