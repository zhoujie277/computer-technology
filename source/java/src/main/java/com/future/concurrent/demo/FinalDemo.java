package com.future.concurrent.demo;

@SuppressWarnings("all")
public class FinalDemo {

    static int A = 10;
    final static int B = 11;
    final static int C = Short.MAX_VALUE + 1;

    final int a = 20;
    final int b = Integer.MAX_VALUE;

    public void test() {
        System.out.println(A);
        System.out.println(B);
        System.out.println(C);
        System.out.println(new FinalDemo().a);
        System.out.println(new FinalDemo().b);
    }

    /**
     *  0 getstatic #33 <java/lang/System.out>
     *  3 getstatic #19 <com/future/concurrent/demo/FinalDemo.A>
     *  6 invokevirtual #39 <java/io/PrintStream.println>
     *  9 getstatic #33 <java/lang/System.out>
     * 12 bipush 11
     * 14 invokevirtual #39 <java/io/PrintStream.println>
     * 17 getstatic #33 <java/lang/System.out>
     * 20 ldc #11 <32768>
     * 22 invokevirtual #39 <java/io/PrintStream.println>
     * 25 getstatic #33 <java/lang/System.out>
     * 28 new #1 <com/future/concurrent/demo/FinalDemo>
     * 31 dup
     * 32 invokespecial #45 <com/future/concurrent/demo/FinalDemo.<init>>
     * 35 invokevirtual #46 <java/lang/Object.getClass>
     * 38 pop
     * 39 bipush 20
     * 41 invokevirtual #39 <java/io/PrintStream.println>
     * 44 getstatic #33 <java/lang/System.out>
     * 47 new #1 <com/future/concurrent/demo/FinalDemo>
     * 50 dup
     * 51 invokespecial #45 <com/future/concurrent/demo/FinalDemo.<init>>
     * 54 invokevirtual #46 <java/lang/Object.getClass>
     * 57 pop
     * 58 ldc #15 <2147483647>
     * 60 invokevirtual #39 <java/io/PrintStream.println>
     * 63 return
     */
}
