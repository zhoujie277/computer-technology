package com.future.netty.chat.server.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

public class SessionMemoryImpl implements Session {

    private final Map<String, Channel> userChannelMap = new ConcurrentHashMap<>();
    private final Map<Channel, String> channelUserMap = new ConcurrentHashMap<>();
    private final Map<Channel, Map<String, Object>> channelAttributeMap = new ConcurrentHashMap<>();

    @Override
    public void bind(Channel channel, String username) {
        userChannelMap.put(username, channel);
        channelUserMap.put(channel, username);
        channelAttributeMap.put(channel, new ConcurrentHashMap<String, Object>());

    }

    @Override
    public void unbind(Channel channel) {
        String name = channelUserMap.remove(channel);
        userChannelMap.remove(name);
        channelAttributeMap.remove(channel);
    }

    @Override
    public Object getAttibute(Channel channel, String name) {
        return channelAttributeMap.get(channel).get(name);
    }

    @Override
    public void setAttribute(Channel channel, String name, Object value) {
        channelAttributeMap.get(channel).put(name, value);
    }

    @Override
    public Channel getChannel(String name) {
        return userChannelMap.get(name);
    }

}
