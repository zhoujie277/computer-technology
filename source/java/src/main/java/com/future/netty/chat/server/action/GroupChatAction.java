package com.future.netty.chat.server.action;

import java.util.Set;

import com.future.netty.chat.common.message.GroupChatRequest;
import com.future.netty.chat.common.message.GroupChatResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.GroupSessionManager;
import com.future.netty.chat.server.session.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupChatAction implements ServerAction {

    @Override
    public void execute(Session session, Message msg) {
        log.debug("{}", msg);
        GroupChatRequest request = (GroupChatRequest) msg;

        GroupSessionManager manager = GroupSessionManager.getGroupSessionManager();
        Set<Session> members = manager.getMembers(request.getGroupID());
        session.writeMessage(new GroupChatResponse(ResultCode.SUCCESS));
        for (Session s : members) {
            s.writeMessage(msg);
        }
    }

}
