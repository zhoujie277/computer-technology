package com.future.jvm.bytecode;

/**
 * 可变参数的分析
 */
@SuppressWarnings("all")
class MutableParameterBytecode {

    /**
     * 0 aload_1                // 将 slot1 的 args 引用入栈
     * 1 dup                    // 在栈顶复制粘贴一个 args 引用。
     * 2 astore 5               // 出栈 args 引用，保存到 slot5
     * 4 arraylength            // 出栈数组引用，获取数组元素，并入栈
     * 5 istore 4               // 出栈数组长度，并保存到 slot4
     * 7 iconst_0               // 数字 0 入栈
     * 8 istore_3               // 出栈数字 0，保存到 slot3
     * 9 goto 27 (+18)          // 跳转到 27 行
     * 12 aload 5               // 将 slot5 的 args 引用入栈
     * 14 iload_3               // 将 slot3 的数组索引入栈
     * 15 aaload                // 出栈索引、args 引用，并入栈数组中位于索引的元素
     * 16 astore_2              // 出栈，将数组元素索引保存到 slot2
     * 17 getstatic #16 <java/lang/System.out>      // 入栈。获取 System.out 引用
     * 20 aload_2               // 入栈。获取 slot2 中的数组元素
     * 21 invokevirtual #22 <java/io/PrintStream.println> 出栈数组元素和 System.out 引用。无返回值。
     * 24 iinc 3 by 1           // 操作数栈无操作。将 slot3 自增 1.
     * 27 iload_3               // 入栈。获取 slot3 的数组索引。
     * 28 iload 4               // 入栈。获取 slot4 的数组长度
     * 30 if_icmplt 12 (-18)    // 出栈两个元素。比较，如果数组索引小于数组长度。则跳转到 12 行。
     * 33 iconst_0              // 入栈。数字 0
     * 34 ireturn                // 出栈。将 0 返回。此后，栈空。
     */
    int foo(String... args) {
        for (String arg : args) {
            System.out.println(arg);
        }
        return 0;
    }

    /**
     * 0 aload_0                                // 入栈。获取 slot0 的 this 引用
     * 1 iconst_2                               // 入栈。数字 2
     * 2 anewarray #35 <java/lang/String>       // 出栈数字 2 作为长度构造一个数组，并将数组引用入栈。
     * 5 dup                                    // 入栈。复制栈顶的数组引用。
     * 6 iconst_0                               // 入栈。数字 0
     * 7 ldc #37 <hello>                        // 入栈。字符串 hello
     * 9 aastore                                // 出现 hello，0，并将 hello 存入数组中的 0 号位置。
     * 10 dup                                   // 上同，给数组的空槽复制。
     * 11 iconst_1
     * 12 ldc #39 <world>
     * 14 aastore
     * 15 invokevirtual #41 <com/future/jvm/MutableParameterBytecode.foo>   // 出栈 this 和 数组引用，并调用 foo 方法，将返回值入栈。
     * 18 pop                                   // 出栈一个元素。
     * 19 return                                // 返回
     */
    void call() {
        foo("hello", "world");
    }

    /**
     * 0 aload_0
     * 1 iconst_3
     * 2 anewarray #35 <java/lang/String>
     * 5 dup
     * 6 iconst_0
     * 7 ldc #44 <java>
     * 9 aastore
     * 10 dup
     * 11 iconst_1
     * 12 ldc #46 <php>
     * 14 aastore
     * 15 dup
     * 16 iconst_2
     * 17 ldc #48 <c++>
     * 19 aastore
     * 20 invokevirtual #41 <com/future/jvm/MutableParameterBytecode.foo>
     * 23 pop
     * 24 return
     */
    void run() {
        foo("java", "php", "c++");
    }
}
