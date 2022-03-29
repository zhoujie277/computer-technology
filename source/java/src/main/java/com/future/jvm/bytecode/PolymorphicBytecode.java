package com.future.jvm.bytecode;

import java.io.IOException;

/**
 * 多态原理分析
 *
 * -XX:-UseCompressedOops
 * -XX:-UseCompressedClassPointers
 * 此处禁用指针压缩，仅仅是为了查看方便，不用进行地址换算。
 *
 *
 * 多态方法调用过程。
 * 1. 通过栈帧中的对象引用找到对象。
 * 2. 分析对象头，找到对象的实际 class。
 * 3. Class 结构中有 vtable，它在类加载的链接阶段就已经根据方法的重写规则生成好了。
 * 4. 查表得到方法的具体地址。
 * 5. 执行方法的字节码。
 *
 * java8: java -cp ./lib/sa-jdi.jar sun.jvm.hotspot.HSDB
 */
public class PolymorphicBytecode {

    /**
     * 0 aload_0
     * 1 invokevirtual #2 <com/future/jvm/Animal.cry>
     * 4 getstatic #3 <java/lang/System.out>
     * 7 aload_0
     * 8 invokevirtual #4 <java/io/PrintStream.println>
     * 11 return
     */
    static void cry(Animal animal) {
        animal.cry();
        System.out.println(animal);
    }

    /**
     *  0 new #5 <com/future/jvm/Dog>
     *  3 dup
     *  4 invokespecial #6 <com/future/jvm/Dog.<init>>
     *  7 invokestatic #7 <com/future/jvm/PolymorphicBytecode.cry>
     * 10 new #8 <com/future/jvm/Chicken>
     * 13 dup
     * 14 invokespecial #9 <com/future/jvm/Chicken.<init>>
     * 17 invokestatic #7 <com/future/jvm/PolymorphicBytecode.cry>
     * 20 getstatic #10 <java/lang/System.in>
     * 23 invokevirtual #11 <java/io/InputStream.read>
     * 26 pop
     * 27 return
     */
    public static void main(String[] args) throws IOException {
        cry(new Dog());
        cry(new Chicken());
        System.in.read();
    }
}

abstract class Animal {
    public abstract void cry();

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}

class Dog extends Animal {

    @Override
    public void cry() {
        System.out.println("fei..");
    }
}

class Chicken extends Animal {
    @Override
    public void cry() {
        System.out.println("ming...");
    }
}