package com.future.netty.chat.server.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.future.netty.chat.common.message.Group;
import com.future.netty.chat.common.message.GroupCreateRequest;
import com.future.netty.chat.common.message.GroupCreateResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.GroupSession;
import com.future.netty.chat.server.session.GroupSessionManager;
import com.future.netty.chat.server.session.Session;
import com.future.netty.chat.server.session.SessionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupCreateAction implements ServerAction {

    @Override
    public void execute(Session session, Message msg) {
        log.debug("{}", msg);

        GroupCreateRequest request = (GroupCreateRequest) msg;
        Group group = request.getGroup();
        String groupName = group.getName();
        List<User> users = group.getMembers();

        Set<Session> sessions = new HashSet<>();
        SessionManager sessionMgr = SessionManager.getInstance();
        for (User user : users) {
            sessions.add(sessionMgr.getSession(user));
        }

        GroupSessionManager manager = GroupSessionManager.getGroupSessionManager();
        GroupSession groupSession = manager.createGroupSession(groupName, sessions);

        GroupCreateResponse response = new GroupCreateResponse(ResultCode.SUCCESS);
        group.setGroupID(groupSession.getGroupID());
        group.setSessionID(groupSession.getSessionID());
        response.setGroup(group);

        Set<Session> members = manager.getMembers(groupName);
        for (Session s : members) {
            s.writeMessage(response);
        }
    }

}
