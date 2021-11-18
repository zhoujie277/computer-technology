package com.future.netty.chat.rpc;

import java.lang.reflect.Method;

import com.future.netty.chat.message.RpcRequestMessage;
import com.future.netty.chat.message.RpcResponseMessage;
import com.future.netty.chat.rpc.service.HelloService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class RpcMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) throws Exception {
        final RpcResponseMessage response = new RpcResponseMessage();
        response.setSequenceId(msg.getSequenceId());
        try {
            Object returnValue = invokePPCMethod(msg);
            response.setReturnValue(returnValue);
        } catch (Exception e) {
            log.error("RPC error", e);
            response.setException(new Exception("调用错误：" + e.getCause().getMessage()));
        }
        ctx.writeAndFlush(response);
    }

    private static Object invokePPCMethod(RpcRequestMessage msg) throws Exception {
        Class<?> clazz = Class.forName(msg.getInterfaceName());
        Method method = clazz.getMethod(msg.getMethodName(), msg.getParameterTypes());
        Object obj = RPCServiceFactory.getService(clazz);
        return method.invoke(obj, msg.getParameterValue());
    }

    public static void main(String[] args) throws Exception {
        RpcRequestMessage message = new RpcRequestMessage(1, "com.future.netty.chat.rpc.service.HelloService",
                "sayHello", String.class, new Class[] { String.class }, new Object[] { "张三" });
        HelloService service = (HelloService) RPCServiceFactory.getService(Class.forName(message.getInterfaceName()));
        service.sayHello("name");
        val value = invokePPCMethod(message);
        log.debug("{}", value);
    }
}
