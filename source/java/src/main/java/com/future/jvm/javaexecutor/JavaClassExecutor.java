package com.future.jvm.javaexecutor;

import java.lang.reflect.Method;

@SuppressWarnings("all")
public class JavaClassExecutor {

    public static String execute(byte[] bytes) {
        HackSystem.clearBuffer();
        ClassModifier cm = new ClassModifier(bytes);
        byte[] modiBytes = cm.modifyUTF8Constant("java/lang/System", "com/future/javaexecutor/HackSystem");
        HotSpotClassLoader loader = new HotSpotClassLoader();
        Class<?> clazz = loader.loadByte(modiBytes);
        try {
            Method method = clazz.getMethod("main", new Class[]{String[].class});
            method.invoke(null, new String[]{null});
        } catch (Throwable e) {
            e.printStackTrace(HackSystem.out);
        }
        return HackSystem.getBufferString();
    }
}
