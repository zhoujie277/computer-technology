package com.future.jvm.bytecode;

public class PrimitiveBytecode {

    void primitive() {
        int i = 100;
        Integer j = new Integer(200);
    }
}

//
//  // compiled from: PrimitiveBytecode.java
//
//  // access flags 0x1
//  public <init>()V
//   L0
//    LINENUMBER 3 L0
//    ALOAD 0
//    INVOKESPECIAL java/lang/Object.<init> ()V
//    RETURN
//   L1
//    LOCALVARIABLE this Lcom/future/jvm/bytecode/PrimitiveBytecode; L0 L1 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//
//  // access flags 0x0
//  primitive()V
//   L0
//    LINENUMBER 6 L0
//    BIPUSH 100
//    ISTORE 1
//   L1
//    LINENUMBER 7 L1
//    NEW java/lang/Integer
//    DUP
//    SIPUSH 200
//    INVOKESPECIAL java/lang/Integer.<init> (I)V
//    ASTORE 2
//   L2
//    LINENUMBER 8 L2
//    RETURN
//   L3
//    LOCALVARIABLE this Lcom/future/jvm/bytecode/PrimitiveBytecode; L0 L3 0
//    LOCALVARIABLE i I L1 L3 1
//    LOCALVARIABLE j Ljava/lang/Integer; L2 L3 2
//    MAXSTACK = 3
//    MAXLOCALS = 3
//}
