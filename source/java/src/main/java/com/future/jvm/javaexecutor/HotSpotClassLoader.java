package com.future.jvm.javaexecutor;

public class HotSpotClassLoader extends ClassLoader {

    public HotSpotClassLoader() {
        super(HotSpotClassLoader.class.getClassLoader());
    }

    public Class<?> loadByte(byte[] bytes) {
        return defineClass(null, bytes, 0, bytes.length);
    }
}
