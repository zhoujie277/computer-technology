package com.future.netty.chat.common.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.future.netty.chat.common.exception.SerializerException;
import com.future.netty.chat.common.message.Message;

public class JavaSerializer implements Serializer<Message> {

    @Override
    public Message deserialize(int msgType, byte[] bytes) {
        try {
            ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bios);
            return (Message) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializerException();
        }
    }

    @Override
    public byte[] serialize(Message msg) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(msg);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializerException();
        }
    }

}
