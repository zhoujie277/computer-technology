package com.future.jvm.bytecode;

/**
 * 匿名内部类的字节码分析
 *
 * @author future
 */
@SuppressWarnings("all")
class AnonymousClassBytecode {

    /*
     * // class version 52.0 (52)
     * // access flags 0x20
     * class com/future/jvm/AnonymousClassAnalyzer$1 implements java/lang/Runnable {
     *
     *   // compiled from: AnonymousClassAnalyzer.java
     *   OUTERCLASS com/future/jvm/AnonymousClassAnalyzer main ([Ljava/lang/String;)V
     *   // access flags 0x0
     *   INNERCLASS com/future/jvm/AnonymousClassAnalyzer$1 null null
     *
     *   // access flags 0x0
     *   <init>()V
     *    L0
     *     LINENUMBER 13 L0
     *     ALOAD 0
     *     INVOKESPECIAL java/lang/Object.<init> ()V
     *     RETURN
     *    L1
     *     LOCALVARIABLE this Lcom/future/jvm/AnonymousClassAnalyzer$1; L0 L1 0
     *     MAXSTACK = 1
     *     MAXLOCALS = 1
     *
     *   // access flags 0x1
     *   public run()V
     *    L0
     *     LINENUMBER 16 L0
     *     GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
     *     LDC "hello"
     *     INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
     *    L1
     *     LINENUMBER 17 L1
     *     RETURN
     *    L2
     *     LOCALVARIABLE this Lcom/future/jvm/AnonymousClassAnalyzer$1; L0 L2 0
     *     MAXSTACK = 2
     *     MAXLOCALS = 1
     * }
     */

    public static void main(String[] args) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("hello");
            }
        };
    }
}
