package com.future.netty.chat.server.session;

import java.util.HashMap;
import java.util.Map;

import com.future.netty.chat.common.message.User;
import com.future.util.Utility;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "sessionID" })
public class Session {

    private String sessionID;
    private User user;
    private Channel channel;
    private Map<String, Object> attrs = new HashMap<>();

    public Session(Channel channel, User user) {
        sessionID = Utility.UUID();
        this.channel = channel;
        this.user = user;
    }

    public void addAttribute(String key, Object object) {
        this.attrs.put(key, object);
    }

    public Object getAttibute(String key) {
        return this.attrs.get(key);
    }

    public <T> void writeMessage(T message) {
        channel.writeAndFlush(message);
    }

    public void release() {
        // 释放资源
        attrs.clear();
    }

    public void close() {
        channel.close();
    }
}
