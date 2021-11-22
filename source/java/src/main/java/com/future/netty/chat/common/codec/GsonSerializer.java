package com.future.netty.chat.common.codec;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.future.netty.chat.common.message.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonSerializer implements Serializer<Message> {

    @Override
    public Message deserialize(int msgType, byte[] bytes) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Message.MsgType type = Message.MsgType.values()[msgType];
        Class<? extends Message> clazz = Message.getMessageClass(type);
        return gson.fromJson(json, clazz);
    }

    @Override
    public byte[] serialize(Message msg) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = gson.toJson(msg);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    static class ClassCodec implements JsonDeserializer<Class<?>>, JsonSerializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return Class.forName(json.getAsString());
            } catch (Exception exception) {
                throw new JsonParseException(exception);
            }
        }

        @Override
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }
}
