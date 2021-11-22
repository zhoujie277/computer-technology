package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class RpcRequest extends Message {

    /**
     * 调用的接口全限定名，服务端根据它找到实现
     */
    private String interfaceName;

    /**
     * 调用接口中的方法名
     */
    private String methodName;

    /**
     * 方法返回类型
     */
    private Class<?> returnType;

    /**
     * 方法参数类型数组
     */
    private Class<?>[] parameterTypes;

    /**
     * 方法参数值数组
     */
    private Object[] parameterValue;

    public RpcRequest(long sequenceId, String interfaceName, String methodName, Class<?> returnType,
            Class<?>[] parameterTypes, Object[] parameterValue) {
        this.sequence = sequenceId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterValue = parameterValue;
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.RPC_REQ;
    }
}
