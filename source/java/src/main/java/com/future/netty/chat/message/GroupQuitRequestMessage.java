package com.future.netty.chat.message;

public class GroupQuitRequestMessage extends Message {

    private String groupName;

    private String username;

    public GroupQuitRequestMessage(String username, String groupName) {
        this.groupName = groupName;
        this.username = username;
    }

    @Override
    public int getMessageType() {
        return GroupQuitRequestMessage;
    }

    @Override
    public String toString() {
        return "GroupQuitRequestMessage [groupName=" + groupName + ", username=" + username + "]";
    }

}
