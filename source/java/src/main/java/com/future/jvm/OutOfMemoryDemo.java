package com.future.jvm;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存溢出的演示
 */
@SuppressWarnings("all")
class OutOfMemoryDemo {


    static class CustomClassLoader extends ClassLoader {
        /**
         * 演示内存溢出需要增加：-XX:MaxMetaspaceSize=8m
         */
        public void testMethodArea() {
            int j = 0;
            try {
                for (int i = 0; i < 20000; i++, j++) {
                    ClassWriter writer = new ClassWriter(0);
                    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Class" + i, null, "java/lang/Object", null);
                    byte[] code = writer.toByteArray();
                    defineClass("Class" + i, code, 0, code.length);
                }
            } finally {
                System.out.println(j);
            }

            // will throw Exception in thread "main" java.lang.OutOfMemoryError: Compressed class space
        }
    }

    void testMethodArea() {
        CustomClassLoader loader = new CustomClassLoader();
        loader.testMethodArea();
    }

    /**
     * 下面的测试方法可测试 intern 方法内存分配的位置。（严瑾起见，放在 JDK 6 中可调整代码去掉 list）
     * -Xmx10m
     * -XX:-UseGCOverheadLimit
     * <p>
     * JDK 8 will throw java.lang.OutOfMemoryError: Java heap space
     * JDK 6 是永久代
     * jdk6 下设置 -XX:MaxPermSize=10m
     */
    void testHeapMemory() {
        List<String> list = new ArrayList<>();
        int i = 0;
        try {
            for (int j = 0; j < 260000; j++) {
                list.add(String.valueOf(j).intern());
                i++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println(i);
        }
    }

    //
    void testPermMemory() {

    }

    public static void main(String[] args) {
        OutOfMemoryDemo demo = new OutOfMemoryDemo();
        demo.testHeapMemory();
    }
}
