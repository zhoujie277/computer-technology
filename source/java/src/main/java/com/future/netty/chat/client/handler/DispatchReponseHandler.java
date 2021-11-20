package com.future.netty.chat.client.handler;

import java.util.HashMap;
import java.util.Map;

import com.future.netty.chat.proto.ProtoMsg;
import com.future.netty.chat.proto.ProtoMsg.HeadType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DispatchReponseHandler extends ChannelInboundHandlerAdapter {

    private static Map<HeadType, IMessageResponse> msgMap = new HashMap<>();

    static {
        msgMap.put(ProtoMsg.HeadType.LOGIN_RESPONSE, new ChatMsgResponse());
        // msgMap.put(ProtoMsg.HeadType.MESSAGE_RESPONSE, new ChatMsgResponse());
        msgMap.put(ProtoMsg.HeadType.MESSAGE_REQUEST, new ChatMsgResponse());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 判断消息实例
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        // 判断类型
        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;
        ProtoMsg.HeadType headType = ((ProtoMsg.Message) msg).getType();
        IMessageResponse iMessageResponse = msgMap.get(headType);
        if (iMessageResponse == null) {
            super.channelRead(ctx, msg);
            return;
        }
        iMessageResponse.onResponse(ctx, pkg);
    }
}
