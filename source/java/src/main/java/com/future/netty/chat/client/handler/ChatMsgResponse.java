package com.future.netty.chat.client.handler;

import com.future.netty.chat.proto.ProtoMsg;

import io.netty.channel.ChannelHandlerContext;

public class ChatMsgResponse implements IMessageResponse {

    @Override
    public void onResponse(ChannelHandlerContext ctx, ProtoMsg.Message msg) {
        ProtoMsg.MessageRequest req = msg.getMsgRequest();
        String content = req.getContent();
        String uid = req.getFrom();

        System.out.println(" 收到消息 from uid:" + uid + " -> " + content);
    }

}
