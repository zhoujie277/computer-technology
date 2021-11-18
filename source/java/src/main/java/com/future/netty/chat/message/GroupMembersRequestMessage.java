package com.future.netty.chat.message;

public class GroupMembersRequestMessage extends Message {
    private String groupName;

    public GroupMembersRequestMessage(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public int getMessageType() {
        return GroupMembersRequestMessage;
    }

    @Override
    public String toString() {
        return "GroupMembersRequestMessage [groupName=" + groupName + "]";
    }
}
