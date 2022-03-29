package com.future.jvm.asm;

public class CheckJavaClass {

    public static void check(String clazz) throws Exception {
        Class<?> aClass = Class.forName(clazz);
        Object o = aClass.newInstance();
        System.out.println(o);
    }

    public static void main(String[] args) throws Exception {
//        check("com.future.jvm.asm.TargetTimer");
        check("com.future.jvm.asm.TargetObject");
    }
}
