package com.future.netty.chat.common.codec;

import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Message.Codec;
import com.future.netty.chat.common.util.ChatConfiguration;

public interface Serializer<T> {

    T deserialize(int msgType, byte[] in);

    byte[] serialize(T msg);

    public static Serializer<Message> getSerializer() {
        Serializer<Message> serializer = null;
        Codec codecType = ChatConfiguration.getCodecType();
        if (codecType == Codec.PROTO_BUF) {
            serializer = new ProtobufSerializer();
        } else if (codecType == Codec.GSON) {
            serializer = new GsonSerializer();
        } else if (codecType == Codec.JAVA) {
            serializer = new JavaSerializer();
        }
        return serializer;
    }
}
