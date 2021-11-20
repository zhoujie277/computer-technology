package com.future.netty.chat.client.handler;

import com.future.netty.chat.client.ClientSession;
import com.future.netty.chat.proto.ProtoMsg;
import com.future.netty.chat.proto.ProtoMsg.HeadType;

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
            ClientSession clientSession = ClientSession.getSession();
            ProtoMsg.Message message = clientSession.buildCommon().setType(HeadType.PING).build();
            clientSession.writeMessage(message);
        }
    }
}