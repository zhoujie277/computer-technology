package com.future.netty.chat.message;

import java.util.Set;

public class GroupMembersResponseMessage extends Message {
    private Set<String> members;

    public GroupMembersResponseMessage(Set<String> members) {
        this.members = members;
    }

    @Override
    public int getMessageType() {
        return GroupMembersResponseMessage;
    }

    @Override
    public String toString() {
        return "GroupMembersResponseMessage [members=" + members + "]";
    }
}
