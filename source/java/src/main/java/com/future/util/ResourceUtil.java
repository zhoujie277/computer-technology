package com.future.util;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@SuppressWarnings("unused")
public class ResourceUtil {

    public static String getAbsolutePath(String className) {
        return getClassPath() + className.replace(".", "/") + ".class";
    }

    public static String getClassPath(Class<?> clazz) {
        return Objects.requireNonNull(clazz.getResource("")).toString();
    }

    public static String getClassPath() {
        return Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("")).getPath();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static byte[] loadBytesInClassPath(String relativePath) {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(relativePath);
        if (inputStream == null) throw new NoClassDefFoundError(relativePath);
        try {
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);
            return b;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
