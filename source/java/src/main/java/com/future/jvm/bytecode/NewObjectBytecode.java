package com.future.jvm.bytecode;

public class NewObjectBytecode {

    public static void main(String[] args) {
        Object o = new Object();
        System.out.println(o.hashCode());
    }
}

/*
 * public class com/future/jvm/bytecode/NewObjectBytecode {
 *
 *   // compiled from: NewObjectBytecode.java
 *
 *   // access flags 0x1
 *   public <init>()V
 *    L0
 *     LINENUMBER 3 L0
 *     ALOAD 0
 *     INVOKESPECIAL java/lang/Object.<init> ()V
 *     RETURN
 *    L1
 *     LOCALVARIABLE this Lcom/future/jvm/bytecode/NewObjectBytecode; L0 L1 0
 *     MAXSTACK = 1
 *     MAXLOCALS = 1
 *
 *   // access flags 0x9
 *   public static main([Ljava/lang/String;)V
 *    L0
 *     LINENUMBER 6 L0
 *     NEW java/lang/Object
 *     DUP
 *     INVOKESPECIAL java/lang/Object.<init> ()V
 *     ASTORE 1
 *    L1
 *     LINENUMBER 7 L1
 *     GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
 *     ALOAD 1
 *     INVOKEVIRTUAL java/lang/Object.hashCode ()I
 *     INVOKEVIRTUAL java/io/PrintStream.println (I)V
 *    L2
 *     LINENUMBER 8 L2
 *     RETURN
 *    L3
 *     LOCALVARIABLE args [Ljava/lang/String; L0 L3 0
 *     LOCALVARIABLE o Ljava/lang/Object; L1 L3 1
 *     MAXSTACK = 2
 *     MAXLOCALS = 2
 * }
 */