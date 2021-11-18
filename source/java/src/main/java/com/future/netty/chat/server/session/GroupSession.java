
package com.future.netty.chat.server.session;

import java.util.List;
import java.util.Set;

import io.netty.channel.Channel;

public interface GroupSession {

    Group createGroup(String name, Set<String> members);

    Group joinGroup(String name, String member);

    Group removeMember(String name, String member);

    Group removeGroup(String name);

    Set<String> getMembers(String name);

    List<Channel> getMemberChannels(String name);

}
