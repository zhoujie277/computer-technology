package com.future.jvm.bytecode;

/**
 * synchronized 字节码分析
 */
@SuppressWarnings("all")
public class SynchronizedBytecode {
    static final Object obj = new Object();

    /**
     * 0 getstatic #13 <com/future/jvm/SynchronizedAnalyzer.obj>    // 加载 obj 的引用
     * 3 dup            // 复制操作数栈的最上层的数值。
     * 4 astore_1       // 操作数栈出栈，存入 slot1
     * 5 monitorenter   // 进入同步代码块
     * 6 getstatic #21 <java/lang/System.out>  // 获取 System.out 对象
     * 9 ldc #27 <hello> // 加载字符串 "hello"
     * 11 invokevirtual #29 <java/io/PrintStream.println>
     * 14 aload_1       // 将 slot1 的数值入栈
     * 15 monitorexit   // 退出同步代码块
     * 16 goto 22 (+6)  // 跳转到第 22 行字节码指令
     * 19 aload_1       // 将 slot1 的数值入栈，此处防止异常，请看异常表
     * 20 monitorexit   // 退出同步代码块。
     * 21 athrow        // 抛出异常
     * 22 return        // 操作数出栈，返回
     *
     * Exception table:
     * No. from to switch exception_type
     * 0	6	16	19	cp_info #0 (any)  (意思是在 6-16 行出现的异常将会跳转到 19 行。
     * 1	19	21	19	cp_info #0 (any) (意思是在 19-21 行出现的异常将会跳转到 19 行。
     */
    public static void main(String[] args) {
        synchronized (obj) {
            System.out.println("hello");
        }
    }


}
