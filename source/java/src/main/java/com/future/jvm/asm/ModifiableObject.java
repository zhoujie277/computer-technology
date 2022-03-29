package com.future.jvm.asm;

public class ModifiableObject {

    private String field;//= "hello";

    public int foo(int a) {
        return a;
    }

    public void testInject() {
        System.out.println("will throw null pointer exception...");
        int j = field.hashCode();
        System.out.println("no null pointer exception:" + j);
    }

    public static void main(String[] args) {
        ModifiableObject test = new ModifiableObject();
        System.out.println(test.foo(1));
        test.testInject();
    }
}
