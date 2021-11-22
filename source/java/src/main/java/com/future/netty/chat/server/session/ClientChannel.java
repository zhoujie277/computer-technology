package com.future.netty.chat.server.session;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class ClientChannel {

    private Channel channel;
}
