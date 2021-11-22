package com.future.netty.chat.common.exception;

import com.future.netty.chat.common.util.ChatConfiguration;

public class SerializerException extends RuntimeException {

    public SerializerException() {
        super("Serializer Exception, current codec=" + ChatConfiguration.getCodecType());
    }
}
