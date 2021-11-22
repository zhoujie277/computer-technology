package com.future.netty.chat.server.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 该管理器在高并发环境，需要注意线程安全
 */
public class GroupSessionManager {

    private GroupSessionManager() {
    }

    private static final GroupSessionManager sManager = new GroupSessionManager();

    public static GroupSessionManager getGroupSessionManager() {
        return sManager;
    }

    private final Map<String, GroupSession> groupMap = new ConcurrentHashMap<>();

    public GroupSession createGroupSession(String name, Set<Session> members) {
        GroupSession group = new GroupSession(name, members);
        groupMap.put(group.getGroupID(), group);
        return group;
    }

    public GroupSession joinGroup(String groupID, Session session) {
        return groupMap.computeIfPresent(groupID, (key, value) -> {
            value.getMembers().add(session);
            return value;
        });
    }

    public GroupSession removeMember(String groupID, Session member) {
        return groupMap.computeIfPresent(groupID, (key, value) -> {
            value.getMembers().remove(member);
            return value;
        });
    }

    public GroupSession getGroupSession(String groupID) {
        return groupMap.get(groupID);
    }

    public GroupSession removeGroup(String groupID) {
        return groupMap.remove(groupID);
    }

    public Set<Session> getMembers(String groupID) {
        return groupMap.getOrDefault(groupID, GroupSession.EMPTY_GROUP).getMembers();
    }

}
