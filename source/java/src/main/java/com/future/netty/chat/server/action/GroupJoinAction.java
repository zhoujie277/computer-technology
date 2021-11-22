package com.future.netty.chat.server.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.future.netty.chat.common.message.Group;
import com.future.netty.chat.common.message.GroupJoinRequest;
import com.future.netty.chat.common.message.GroupJoinResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.GroupSession;
import com.future.netty.chat.server.session.GroupSessionManager;
import com.future.netty.chat.server.session.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupJoinAction implements ServerAction {

    @Override
    public void execute(Session session, Message msg) {
        log.debug("{}", msg);
        GroupJoinRequest request = (GroupJoinRequest) msg;
        GroupSession joinGroup = GroupSessionManager.getGroupSessionManager().joinGroup(request.getGroupID(), session);
        GroupJoinResponse response = new GroupJoinResponse();
        if (joinGroup != null) {
            response.setResultCode(ResultCode.SUCCESS);
            Group group = convert(joinGroup);
            response.setGroup(group);
        } else {
            response.setResultCode(ResultCode.NOT_EXISTS_GROUP);
        }
        session.writeMessage(response);
    }

    private static Group convert(GroupSession session) {
        Group group = new Group();
        group.setGroupID(session.getGroupID());
        group.setName(session.getGroupName());
        group.setSessionID(session.getSessionID());
        group.setLeader(session.getLeader().getUser());
        List<User> users = new ArrayList<>();
        Set<Session> members = session.getMembers();
        for (Session s : members) {
            users.add(s.getUser());
        }
        group.setMembers(users);
        return group;
    }

}
