package com.future.netty.chat.client.handler;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.common.message.Ping;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartBeatHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state() == IdleState.WRITER_IDLE) {
            log.debug("3s arrived, send ping msg");
            Cookie clientSession = Cookie.getCookie();
            Ping message = new Ping();
            clientSession.writeMessage(message);
        }
    }
}