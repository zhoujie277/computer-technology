package com.future.jvm;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存溢出的演示
 */
@SuppressWarnings("all")
class OutOfMemoryDemo {
    private static final int _2MB = 1 << 21;

    static class Data {
        final byte[] data = new byte[_2MB];
        final int i;

        Data(int i) {
            this.i = i;
        }
    }

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
     * 下面的测试方法可测试 intern 方法内存分配的位置。
     * -Xmx10m
     * <p>
     * -XX:-UseGCOverheadLimit
     * 如果 JVM 花费了 98% 的时间进行垃圾回收，而只得到 2% 可用的内存，
     * 频繁的进行内存回收(最起码已经进行了 5 次连续的垃圾回收)，JVM 就会曝出 java.lang.OutOfMemoryError: GC overhead limit exceeded 错误。
     *
     * <p>
     * * java.lang.OutOfMemoryError: Java heap space
     * * 	at java.lang.Integer.toString(Integer.java:403)
     * * 	at java.lang.String.valueOf(String.java:3099)
     */
    void metaspaceMemory() {
        List<String> list = new ArrayList<>();
        int i = 0;
        try {
            for (int j = 0; j < 2600000; j++) {
                list.add(String.valueOf(j).intern());
                i++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println(i);
        }
    }

    /**
     * -Xms20m -Xmx20m
     * <p>
     * * java.lang.OutOfMemoryError: Java heap space
     * * 	at com.future.jvm.OutOfMemoryDemo$Data.<init>(OutOfMemoryDemo.java:17)
     * * 	at com.future.jvm.OutOfMemoryDemo.heapMemory(OutOfMemoryDemo.java:82)
     * * 	at com.future.jvm.OutOfMemoryDemo.main(OutOfMemoryDemo.java:92)
     */
    void heapMemory() {
        List<Data> list = new ArrayList<>();
        try {
            for (int j = 0; j < 10; j++) {
                list.add(new Data(j));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * -Xss256k 设置栈容量
     * <p>
     * depth: 18312
     * * Exception in thread "main" java.lang.StackOverflowError
     * * 	at com.future.jvm.OutOfMemoryDemo$StackOverflow.depthInvoke(OutOfMemoryDemo.java:102)
     */
    static class StackOverflow {
        int depth = 0;

        void depthInvoke() {
            depth++;
            depthInvoke();
        }

        void exception() {
            try {
                depthInvoke();
            } finally {
                System.out.println("depth: " + this.depth);
            }
        }
    }


    /**
     * -XX:MaxDirectMemorySize=10m
     * <p>
     * 设置最大的直接内存
     * <p>
     * 如果使用 Java 自带的 ByteBuffer.allocateDirect(size) 或者直接 new DirectByteBuffer(capacity) ,
     * 这样受-XX:MaxDirectMemorySize 这个 JVM 参数的限制. 其实底层都是用的 Unsafe#allocateMemory, 区别是对大小做了限制. 如果超出限制直接OOM.
     * <p>
     * 使用 Bits.reserveMemory 真正调用 Unsafe.allocateMemory 之前在 Java 层做了校验内存是否可分配处理。
     * <p>
     * 如果通过反射的方式拿到 Unsafe 的实例,然后用 Unsafe::allocateMemory 方法分配堆外内存.
     * 确实不受-XX:MaxDirectMemorySize 这个 JVM 参数的限制 .
     * 所以限制的内存大小为操作系统的内存.
     * <p>
     * 如果不设置-XX:MaxDirectMemorySize 默认的话,是跟堆内存大小保持一致.
     * [堆内存大小如果不设置的话,默认为操作系统的 1/4, 所以 DirectMemory 的大小限制 JVM 的 Runtime.getRuntime().maxMemory() 内存大小 . ]
     * <p>
     * * Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
     * * 	at java.nio.Bits.reserveMemory(Bits.java:695)
     * * 	at java.nio.DirectByteBuffer.<init>(DirectByteBuffer.java:123)
     * * 	at java.nio.ByteBuffer.allocateDirect(ByteBuffer.java:311)
     */
    static class DirectMemory {
        final Unsafe unsafe;

        DirectMemory() {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }

        void exception() throws InterruptedException {
            List<ByteBuffer> buf = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                buf.add(ByteBuffer.allocateDirect(_2MB));
                Thread.sleep(1000);
            }
        }

        void system() throws InterruptedException {
            long last = 0, now = 0;
            for (int i = 0; i < 100000; i++) {
                now = unsafe.allocateMemory(_2MB);
                System.out.println("allocate=" + (now - last));
                last = now;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        OutOfMemoryDemo demo = new OutOfMemoryDemo();
//        demo.heapMemory();
//        demo.metaspaceMemory();
//        StackOverflow flow = new StackOverflow();
//        flow.exception();
        DirectMemory memory = new DirectMemory();
        memory.exception();
    }
}
