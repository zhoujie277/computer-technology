package com.future.netty.chat.server.action;

import com.future.netty.chat.common.message.GroupQuitRequest;
import com.future.netty.chat.common.message.GroupQuitResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.netty.chat.server.session.GroupSession;
import com.future.netty.chat.server.session.GroupSessionManager;
import com.future.netty.chat.server.session.Session;

public class GroupQuitAction implements ServerAction {

    @Override
    public void execute(Session session, Message msg) {
        GroupQuitRequest req = (GroupQuitRequest) msg;
        String groupID = req.getGroupID();
        GroupSession groupSession = GroupSessionManager.getGroupSessionManager().removeMember(groupID, session);
        if (groupSession != null) {
            session.writeMessage(new GroupQuitResponse(ResultCode.SUCCESS));
        } else {
            session.writeMessage(new GroupQuitResponse(ResultCode.NOT_EXISTS_GROUP));
        }
    }

}
