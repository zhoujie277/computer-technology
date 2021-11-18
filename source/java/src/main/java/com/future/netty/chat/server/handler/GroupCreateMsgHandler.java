package com.future.netty.chat.server.handler;

import java.util.List;
import java.util.Set;

import com.future.netty.chat.message.GroupChatResponseMessage;
import com.future.netty.chat.message.GroupCreateRequestMessage;
import com.future.netty.chat.message.GroupCreateResponseMessage;
import com.future.netty.chat.server.session.Group;
import com.future.netty.chat.server.session.GroupSession;
import com.future.netty.chat.server.session.GroupSessionFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class GroupCreateMsgHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        log.debug("{}", msg);
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();

        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, members);
        if (group == null) {
            // 创建成功
            ctx.writeAndFlush(new GroupChatResponseMessage(true, "成功创建群聊：" + groupName));
            List<Channel> channels = groupSession.getMemberChannels(groupName);
            for (Channel channel : channels) {
                channel.writeAndFlush(new GroupCreateResponseMessage(true, "你已被拉入群：" + groupName));
            }
        } else {
            ctx.writeAndFlush(new GroupChatResponseMessage(false, "已存在群： " + groupName));
        }
    }

}
