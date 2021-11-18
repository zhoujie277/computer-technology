package com.future.netty.chat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public interface Serializer {

    <T> T deserialize(Class<T> clazz, byte[] bytes);

    <T> byte[] serialize(T object);

    enum Algorithm implements Serializer {
        Java {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                try {
                    ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
                    ObjectInputStream ois = new ObjectInputStream(bios);
                    return (T) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException();
                }
            }

            @Override
            public <T> byte[] serialize(T object) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(object);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
        },

        Json {
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = new String(bytes, StandardCharsets.UTF_8);
                return gson.fromJson(json, clazz);
            }

            @Override
            public <T> byte[] serialize(T object) {
                Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = gson.toJson(object);
                return json.getBytes(StandardCharsets.UTF_8);
            }
        }

    }

    static class ClassCodec implements JsonDeserializer<Class<?>>, JsonSerializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                Class<?> forName = Class.forName(json.getAsString());
                return forName;
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
