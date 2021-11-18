package com.future.netty.chat.rpc;

import com.future.netty.chat.message.RpcResponseMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.concurrent.Promise;

@Sharable
public class RpcMessageResponseHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) throws Exception {
        int sequenceId = msg.getSequenceId();
        Promise<Object> promise = RpcClientSession.getPromise(sequenceId);
        if (msg.getException() != null) {
            promise.setFailure(msg.getException());
        } else {
            promise.setSuccess(msg.getReturnValue());
        }
    }
}
