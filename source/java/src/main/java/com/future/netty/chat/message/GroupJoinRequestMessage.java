package com.future.netty.chat.message;

public class GroupJoinRequestMessage extends Message {
    private String groupName;

    private String username;

    public GroupJoinRequestMessage(String username, String groupName) {
        this.groupName = groupName;
        this.username = username;
    }

    @Override
    public int getMessageType() {
        return GroupJoinRequestMessage;
    }

    @Override
    public String toString() {
        return "GroupJoinRequestMessage [groupName=" + groupName + ", username=" + username + "]";
    }
}
