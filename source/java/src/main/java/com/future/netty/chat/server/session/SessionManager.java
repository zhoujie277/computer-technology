package com.future.netty.chat.server.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.future.netty.chat.common.message.User;

import io.netty.channel.Channel;

/**
 * 该管理器在高并发环境，需要注意线程安全
 */
public class SessionManager {
    private static SessionManager sManager = new SessionManager();

    public static SessionManager getInstance() {
        return sManager;
    }

    private Map<Channel, Session> mSessions = new ConcurrentHashMap<>();
    private Map<User, Session> mUsers = new ConcurrentHashMap<>();

    public Session bind(Channel channel, User user) {
        Session session = new Session(channel, user);
        mSessions.put(channel, session);
        mUsers.put(user, session);
        return session;
    }

    public void unbind(Channel channel) {
        Session session = mSessions.remove(channel);
        session.close();
    }

    public Object getAttibute(Channel channel, String key) {
        return mSessions.get(channel).getAttibute(key);
    }

    public void setAttribute(Channel channel, String key, Object value) {
        mSessions.get(channel).addAttribute(key, value);
    }

    public Session getSession(Channel channel) {
        return mSessions.get(channel);
    }

    public Session getSession(User user) {
        return mUsers.get(user);
    }

    public Set<Channel> getOnlineChannels() {
        return mSessions.keySet();
    }

    public Set<User> getOnlineUsers() {
        return mUsers.keySet();
    }
}
