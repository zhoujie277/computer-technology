package com.future.netty.chat.server.session;

import io.netty.channel.Channel;

public interface Session {

    void bind(Channel channel, String username);

    void unbind(Channel channel);

    Object getAttibute(Channel channel, String name);

    void setAttribute(Channel channel, String name, Object value);

    Channel getChannel(String name);

}
