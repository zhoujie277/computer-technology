package com.future.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {

    private ReflectUtil() {
    }

    public static void convertObjectByFrom(Object fromObject, Object toObject)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method[] methods = fromObject.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get")) {
                Object attr = method.invoke(fromObject);
                if (attr == null)
                    continue;
                toObject.getClass().getDeclaredMethod("set" + methodName.substring(3), method.getReturnType())
                        .invoke(toObject, attr);
            }
        }
    }

    public static void convertObjectByTo(Object fromObject, Object toObject)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method[] methods = toObject.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                String getMethodName = convertGet(methodName);
                Object value = toObject.getClass().getMethod(getMethodName).invoke(toObject);
                if (value == null) {
                    Method getMethod = fromObject.getClass().getDeclaredMethod(getMethodName);
                    Object attr = getMethod.invoke(fromObject);
                    if (attr == null)
                        continue;
                    method.invoke(toObject, attr);
                }
            }
        }
    }

    public static String convertSet(String methodName) {
        return "set" + methodName.substring(3);
    }

    public static String convertGet(String methodName) {
        return "get" + methodName.substring(3);
    }

    public static String fieldToGetMethod(String name) {
        return fieldToMethod("get", name);

    }

    public static String fieldToSetMethod(String name) {
        return fieldToMethod("set", name);
    }

    public static String fieldToMethod(String prefix, String name) {
        return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}
