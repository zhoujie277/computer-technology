package com.future.netty.chat.message;

public class LoginRequestMessage extends Message {
    private String username;
    private String password;

    @Override
    public int getMessageType() {
        return LoginRequestMessage;
    }

    public LoginRequestMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "{" + " username='" + username + "'" + ", password='" + password + "'" + "}";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
