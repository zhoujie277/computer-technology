package com.future.jvm.bytecode;

/**
 * 内部类的字节码分析
 *
 * @author future
 */
@SuppressWarnings("all")
class InnerClassBytecode {

    /*
     * 在属性当中会增加一个 innerClasses
     * <p>
     *  // compiled from: InnerClassAnalyzer.java
     *   // access flags 0x8
     *   static INNERCLASS com/future/jvm/InnerClassAnalyzer$StaticClass com/future/jvm/InnerClassAnalyzer StaticClass
     *
     *   // access flags 0x0
     *   <init>()V
     *    L0
     *     LINENUMBER 14 L0
     *     ALOAD 0
     *     INVOKESPECIAL java/lang/Object.<init> ()V
     *     RETURN
     *    L1
     *     LOCALVARIABLE this Lcom/future/jvm/InnerClassAnalyzer$StaticClass; L0 L1 0
     *     MAXSTACK = 1
     *     MAXLOCALS = 1
     */
    static class StaticClass {

    }

    class NonStaticClass {

    }
}
