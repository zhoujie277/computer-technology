package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class RpcResponse extends Message {
    private static final long serialVersionUID = 1905222041750251207L;

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * 异常值
     */
    private Exception exception;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.RPC_ACK;
    }

}
