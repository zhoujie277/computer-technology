package com.future.jvm.bytecode;

/**
 * 方法字节码执行分析
 */
@SuppressWarnings("all")
class MethodByteCodeBytecode {
    static class MethodInvoke {
        private void a() {

        }

        private final void b() {

        }

        public void c() {

        }

        public static void d() {

        }
    }

    /**
     * 0 new #15 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke>
     * 3 dup
     * 4 invokespecial #17 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.<init>>
     * 7 astore_1
     * 8 aload_1
     * 9 invokestatic #18 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.access$0>
     * 12 aload_1
     * 13 invokestatic #22 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.access$1>
     * 16 aload_1
     * 17 invokevirtual #25 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.c>
     * 20 invokestatic #28 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.d>
     * 23 invokestatic #28 <com/future/jvm/MethodByteCodeAnalyzer$MethodInvoke.d>
     * 26 return
     */
    void methodInstructions() {
        MethodInvoke invoke = new MethodInvoke();
        invoke.a();
        invoke.b();
        invoke.c();
        // 此处会生成两条多余的字节码指令
        invoke.d();
        MethodInvoke.d();
    }

    /**
     * *  0 iconst_0
     * *  1 istore_1
     * *  2 iload_1
     * *  3 bipush 10
     * *  5 if_icmpge 14 (+9)
     * *  8 iinc 1 by 1
     * * 11 goto 2 (-9)
     * * 14 return
     */
    void loopInstructions() {
        int a = 0;
        while (a < 10) {
            a++;
        }
    }

    /**
     * 0 bipush 10
     * 2 istore_1
     * 3 iload_1
     * 4 iinc 1 by 1
     * 7 iinc 1 by 1
     * 10 iload_1
     * 11 iadd
     * 12 iload_1
     * 13 iinc 1 by -1
     * 16 iadd
     * 17 istore_2
     * 18 getstatic #2 <java/lang/System.out>
     * 21 iload_1
     * 22 invokevirtual #3 <java/io/PrintStream.println>
     * 25 getstatic #2 <java/lang/System.out>
     * 28 iload_2
     * 29 invokevirtual #3 <java/io/PrintStream.println>
     * 32 return
     */
    void operationInstructions() {
        int a = 10;
        int b = a++ + ++a + a--;
        System.out.println(a);
        System.out.println(b);
    }

    /**
     * 0 aload_0
     * 1 invokespecial #55 <java/lang/Object.finalize : ()V>
     * 4 return
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * 0 new #4 <com/future/jvm/MethodByteCodeAnalyzer>
     * 3 dup
     * 4 invokespecial #5 <com/future/jvm/MethodByteCodeAnalyzer.<init>>
     * 7 astore_1
     * 8 return
     */
    public static void main(String[] args) {
        MethodByteCodeBytecode analyzer = new MethodByteCodeBytecode();
    }
}
