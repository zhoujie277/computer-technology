package com.future.netty.chat.client.handler;

import com.future.netty.chat.proto.ProtoMsg;

import io.netty.channel.ChannelHandlerContext;

public interface IMessageResponse {

    void onResponse(ChannelHandlerContext ctx, ProtoMsg.Message msg);
}
