package com.future.jvm.bytecode;

/**
 * try-catch 机制分析
 */
@SuppressWarnings("all")
class TryCatchBytecode {

    /**
     * 0 bipush 10
     * 2 istore_1
     * 3 bipush 20
     * 5 istore_1
     * 6 bipush 40  // finally 代码
     * 8 istore_1
     * 9 goto 32 (+23)
     * 12 astore_2
     * 13 bipush 30
     * 15 istore_1
     * 16 aload_2
     * 17 invokevirtual #3 <java/lang/Exception.printStackTrace>
     * 20 bipush 40  // finally 代码
     * 22 istore_1
     * 23 goto 32 (+9)
     * 26 astore_3
     * 27 bipush 40  // finally 代码
     * 29 istore_1
     * 30 aload_3
     * 31 athrow
     * 32 return
     * <p>
     * ExceptionTable:
     * No  From to switch exceptionType
     * 0	3	6	9	cp_info #17 (java/lang/Exception)
     * 1	3	6	26	cp_info #0 (any)
     * 2	12	20	26	cp_info #0 (any)
     * <p>
     * stacks: 1, locals: 4
     */
    public static void main(String[] args) {
        int a = 10;
        try {
            a = 20;
        } catch (Exception e) {
            a = 30;
            e.printStackTrace();
        } finally {
            a = 40;
        }
    }


    /**
     * 该方法结果返回 10.
     * 字节码如下：
     * <p>
     * 0 bipush 10  // 向操作数栈压入常量 10
     * 2 istore_1   // 操作数栈出栈，将数字 10 存入 slot1
     * 3 iload_1    // 再将 slot1 的 10 取出来压入操作数栈
     * 4 istore_3   // 将操作数栈中的 10 弹出到 slot3
     * 5 bipush 20  // 向操作数栈压入常量 20
     * 7 istore_1   // 操作数栈弹出 10，存入 slot1
     * 8 iload_3    // 将 slot3 的数值压入操作数栈，也就是 10
     * 9 ireturn    // 将操作数栈的值出栈，也就是 10
     * 10 astore_2  // 将 slot2 数值压入操作数栈（类型是异常错误）
     * 11 bipush 20 // 将 数组 20 压入操作数栈
     * 13 istore_1  // 将操作数栈出栈，也就是数字 20 存到 slot1
     * 14 aload_2   // 将 slot2 的值压入操作数栈
     * 15 athrow   // 抛出异常
     */
    int test() {
        int i = 10;
        try {
            return i;
        } finally {
            i = 20;
        }
    }

    /**
     * 此处会返回 20。
     * finally 中 return 语句将不会抛出异常，所以 lint 工具会有警告。
     */
    int swallow() {
//        try {
        return 10;
//        } finally {
//            return 20;
//        }
    }
}
