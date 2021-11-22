
package com.future.netty.chat.server.session;

import java.util.Collections;
import java.util.Set;

import com.future.util.Utility;

import lombok.Getter;

@Getter
public class GroupSession {

    public static final GroupSession EMPTY_GROUP = new GroupSession("", Collections.emptySet());

    private String sessionID;
    private String groupID;
    private String groupName;
    private Set<Session> members;
    private Session leader;

    public GroupSession(String groupName, Set<Session> sessions) {
        this.groupName = groupName;
        this.members = sessions;
        this.groupID = Utility.UUID();
        this.sessionID = Utility.UUID();
    }

}
