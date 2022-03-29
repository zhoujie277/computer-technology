package com.future.jvm.bytecode;

/**
 * static 关键字字节码分析
 *
 * @author future
 */
@SuppressWarnings("all")
public class StaticBytecode {

    private static final int a = 10;    // 直接编译进常量池
    private static final String A = "ABC";  // 直接编译进常量池
    private static final Object o = new Object();   // 类初始化进行，即 clinit

    private static int b = 20;
    private static String s = "DEF";

    private static int c;
    private static int d;

    /*
     * clinit 方法
     *
     *  0 new #3 <java/lang/Object>                         // 入栈。new Object 的引用
     *  3 dup                                               // 入栈。复制栈顶 Object 的引用
     *  4 invokespecial #22 <java/lang/Object.<init>>       // 出栈，o 调用 init 方法，无返回值
     *  7 putstatic #25 <com/future/jvm/StaticAnalyzer.o>   // 出栈。将引用存储给静态字段 StaticAnalyzer.o
     * 10 bipush 20                                         // 入栈。数字 20
     * 12 putstatic #27 <com/future/jvm/StaticAnalyzer.b>   // 出栈。将 20 赋值给静态字段 StaticAnalyzer.b
     * 15 ldc #29 <DEF>                                     // 入栈。字符串 DEF
     * 17 putstatic #31 <com/future/jvm/StaticAnalyzer.s>   // 出栈。将 DEF 赋值给 StaticAnalyzer.s
     * 20 bipush 30                                         // 入栈。数字 30
     * 22 putstatic #33 <com/future/jvm/StaticAnalyzer.c>   // 出栈。将 30 赋值给 StaticAnalyzer.c
     * 25 bipush 10                                         // 入栈。数字 10
     * 27 putstatic #35 <com/future/jvm/StaticAnalyzer.d>   // 出栈。将 10 赋值给 StaticAnalyzer.d
     * 30 return                                            // 返回 void
     */
    static {
        c = 30;
        d = a;
    }
}
