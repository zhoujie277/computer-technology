package com.future.netty.chat.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class RpcResponseMessage extends Message {

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * 异常值
     */
    private Exception exception;

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_RESPONSE;
    }

}
