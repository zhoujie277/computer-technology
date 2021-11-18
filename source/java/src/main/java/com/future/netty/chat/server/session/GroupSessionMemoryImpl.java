package com.future.netty.chat.server.session;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.netty.channel.Channel;

public class GroupSessionMemoryImpl implements GroupSession {

    private final Map<String, Group> groupMap = new ConcurrentHashMap<>();

    @Override
    public Group createGroup(String name, Set<String> members) {
        Group g = new Group(name, members);
        return groupMap.putIfAbsent(name, g);
    }

    @Override
    public Group joinGroup(String name, String member) {
        return groupMap.computeIfPresent(name, (key, value) -> {
            value.getMembers().add(member);
            return value;
        });
    }

    @Override
    public Group removeMember(String name, String member) {
        return groupMap.computeIfPresent(name, (key, value) -> {
            value.getMembers().remove(member);
            return value;
        });
    }

    @Override
    public Group removeGroup(String name) {
        return groupMap.remove(name);
    }

    @Override
    public Set<String> getMembers(String name) {
        return groupMap.getOrDefault(name, Group.EMPTY_GROUP).getMembers();
    }

    @Override
    public List<Channel> getMemberChannels(String name) {
        return getMembers(name).stream().map(member -> SessionFactory.getSession().getChannel(member))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

}
