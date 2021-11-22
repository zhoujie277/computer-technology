package com.future.netty.chat.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // if (cause instanceof InvalidFrameException) {
        // log.error(cause.getMessage());
        // ServerSession.closeSession(ctx);
        // } else {
        // 捕捉异常信息
        log.error(cause.getMessage());
        ctx.close();
        // }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }
}
