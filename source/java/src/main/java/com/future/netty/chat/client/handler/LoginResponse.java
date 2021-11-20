package com.future.netty.chat.client.handler;

import com.future.netty.chat.client.ClientSession;
import com.future.netty.chat.common.ProtoInstant;
import com.future.netty.chat.proto.ProtoMsg;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginResponse implements IMessageResponse {

    @Override
    public void onResponse(ChannelHandlerContext ctx, ProtoMsg.Message msg) {
        // 判断返回是否成功
        ProtoMsg.LoginResponse info = msg.getLoginResponse();

        ProtoInstant.ResultCodeEnum result = ProtoInstant.ResultCodeEnum.values()[info.getCode()];

        if (!result.equals(ProtoInstant.ResultCodeEnum.SUCCESS)) {
            // 登录失败
            log.info(result.getDesc());
        } else {
            // 登录成功
            ClientSession.getSession().loginSuccess(msg);
        }
    }

}
