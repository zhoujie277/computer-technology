package com.future.jvm.bytecode;

/**
 * 虚拟机规范定义：
 * ACC_VOLATILE 0x0040 Declared volatile; cannot be cached.
 */
@SuppressWarnings("all")
public class VolatileBytecode {
    // access flags 0x42.
    private volatile int alpha;
    // access flags 0x2
    private int beta;

    /*
     * // class version 52.0 (52)
     * // access flags 0x21
     * public class com/future/jvm/bytecode/VolatileByteCode {
     *
     *   // compiled from: VolatileByteCode.java
     *
     *   // access flags 0x42
     *   private volatile I alpha
     *
     *   // access flags 0x2
     *   private I beta
     *
     *   // access flags 0x1
     *   public <init>()V
     *    L0
     *     LINENUMBER 3 L0
     *     ALOAD 0
     *     INVOKESPECIAL java/lang/Object.<init> ()V
     *     RETURN
     *    L1
     *     LOCALVARIABLE this Lcom/future/jvm/bytecode/VolatileByteCode; L0 L1 0
     *     MAXSTACK = 1
     *     MAXLOCALS = 1
     * }
     */
}
