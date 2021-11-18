package com.future.netty.chat.server.handler;

import com.future.netty.chat.server.session.SessionFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class QuitHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} ////// 主动断开了", ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} ////// 异常断开了：cause:{}", ctx.channel(), cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.debug("============handlerAdded============");
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.debug("============handlerRemoved============");
        super.handlerRemoved(ctx);
    }

}
