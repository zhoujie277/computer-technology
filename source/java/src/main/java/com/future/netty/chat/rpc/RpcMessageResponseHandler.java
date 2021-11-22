package com.future.netty.chat.rpc;

import com.future.netty.chat.common.message.RpcResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.concurrent.Promise;

@Sharable
public class RpcMessageResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        long sequenceId = msg.getSequence();
        Promise<Object> promise = RpcClientSession.getPromise(sequenceId);
        if (msg.getException() != null) {
            promise.setFailure(msg.getException());
        } else {
            promise.setSuccess(msg.getReturnValue());
        }
    }
}
