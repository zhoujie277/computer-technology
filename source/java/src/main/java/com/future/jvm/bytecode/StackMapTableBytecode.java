package com.future.jvm.bytecode;

@SuppressWarnings("all")
public class StackMapTableBytecode {

    public void forLoop() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += i;
        }
        System.out.println(sum);
    }

    /*
     * // class version 52.0 (52)
     * // access flags 0x21
     * public class com/future/jvm/bytecode/StackMapTableBytecode {
     *
     *   // compiled from: StackMapTableBytecode.java
     *
     *   // access flags 0x1
     *   public <init>()V
     *    L0
     *     LINENUMBER 3 L0
     *     ALOAD 0
     *     INVOKESPECIAL java/lang/Object.<init> ()V
     *     RETURN
     *    L1
     *     LOCALVARIABLE this Lcom/future/jvm/bytecode/StackMapTableBytecode; L0 L1 0
     *     MAXSTACK = 1
     *     MAXLOCALS = 1
     *
     *   // access flags 0x1
     *   public forLoop()V
     *    L0
     *     LINENUMBER 6 L0
     *     ICONST_0
     *     ISTORE 1
     *    L1
     *     LINENUMBER 7 L1
     *     ICONST_0
     *     ISTORE 2
     *    L2
     *    FRAME APPEND [I I]
     *     ILOAD 2
     *     BIPUSH 10
     *     IF_ICMPGE L3
     *    L4
     *     LINENUMBER 8 L4
     *     ILOAD 1
     *     ILOAD 2
     *     IADD
     *     ISTORE 1
     *    L5
     *     LINENUMBER 7 L5
     *     IINC 2 1
     *     GOTO L2
     *    L3
     *     LINENUMBER 10 L3
     *    FRAME CHOP 1
     *     GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
     *     ILOAD 1
     *     INVOKEVIRTUAL java/io/PrintStream.println (I)V
     *    L6
     *     LINENUMBER 11 L6
     *     RETURN
     *    L7
     *     LOCALVARIABLE i I L2 L3 2
     *     LOCALVARIABLE this Lcom/future/jvm/bytecode/StackMapTableBytecode; L0 L7 0
     *     LOCALVARIABLE sum I L1 L7 1
     *     MAXSTACK = 2
     *     MAXLOCALS = 3
     * }
     */
    private int f;

    public void checkAndSetF(int f) {
        if (f >= 0) {
            this.f = f;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /*
     * // access flags 0x1
     *   public checkAndSetF(I)V
     *    L0
     *     LINENUMBER 76 L0
     *     ILOAD 1
     *     IFLT L1
     *    L2
     *     LINENUMBER 77 L2
     *     ALOAD 0
     *     ILOAD 1
     *     PUTFIELD com/future/jvm/bytecode/StackMapTableBytecode.f : I
     *     GOTO L3
     *    L1
     *     LINENUMBER 79 L1
     *    FRAME SAME
     *     NEW java/lang/IllegalArgumentException
     *     DUP
     *     INVOKESPECIAL java/lang/IllegalArgumentException.<init> ()V
     *     ATHROW
     *    L3
     *     LINENUMBER 81 L3
     *    FRAME SAME
     *     RETURN
     *    L4
     *     LOCALVARIABLE this Lcom/future/jvm/bytecode/StackMapTableBytecode; L0 L4 0
     *     LOCALVARIABLE f I L0 L4 1
     *     MAXSTACK = 2
     *     MAXLOCALS = 2
     */

    public static void sleep(long d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     *   // access flags 0x9
     *   public static sleep(J)V
     *     TRYCATCHBLOCK L0 L1 L2 java/lang/InterruptedException
     *    L0
     *     LINENUMBER 118 L0
     *     LLOAD 0
     *     INVOKESTATIC java/lang/Thread.sleep (J)V
     *    L1
     *     LINENUMBER 121 L1
     *     GOTO L3
     *    L2
     *     LINENUMBER 119 L2
     *    FRAME SAME1 java/lang/InterruptedException
     *     ASTORE 2
     *    L4
     *     LINENUMBER 120 L4
     *     ALOAD 2
     *     INVOKEVIRTUAL java/lang/InterruptedException.printStackTrace ()V
     *    L3
     *     LINENUMBER 122 L3
     *    FRAME SAME
     *     RETURN
     *    L5
     *     LOCALVARIABLE e Ljava/lang/InterruptedException; L4 L3 2
     *     LOCALVARIABLE d J L0 L5 0
     *     MAXSTACK = 2
     *     MAXLOCALS = 3
     */
}
