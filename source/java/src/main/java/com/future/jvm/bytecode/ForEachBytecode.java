package com.future.jvm.bytecode;

import java.util.Arrays;
import java.util.List;

/**
 * foreach 语句字节码分析
 *
 * @author future
 */
@SuppressWarnings("all")
class ForEachBytecode {

    /**
     * 0 iconst_5                               // 常量 5 入栈
     * 1 anewarray #2 <java/lang/String>        // 创建一个组件类型为 String 的数组，数组大小为出栈元素 5. 创建成功后将数组引用入栈。
     * 4 dup                                    // 将栈顶元素复制一份到栈顶，此处是复制数组的引用
     * 5 iconst_0                               // 数字 0 入栈
     * 6 ldc #3 <AB>                            // 字符串 AB 入栈
     * 8 aastore                                // 出栈三个元素，一个 reference 值，一个索引，一个数组引用、此处是将 AB 存入数组的 0 号元素。
     * 9 dup                                    // 将栈顶元素复制一份到栈顶。此处是数组引用。
     * 10 iconst_1                              // 数字 1 入栈
     * 11 ldc #4 <AC>                           // 字符串 AC 入栈
     * 13 aastore                               // 出栈 AC、1、数组引用。并将 AC 存入到数组 1 号元素。
     * 14 dup                                   // 复制栈顶的数组引用
     * 15 iconst_2                              // 数字 2 入栈
     * 16 ldc #5 <AD>                           // 字符串 AD 入栈
     * 18 aastore                               // 出栈 3 个元素，将 AD 存入数组 2 号位置
     * 19 dup                                   // 复制栈顶的数组引用
     * 20 iconst_3                              // 数字 3 入栈
     * 21 ldc #6 <AE>                           // 字符串 AE 入栈
     * 23 aastore                               // 出栈 3 个元素，将 AE 存入数组 3 号元素
     * 24 dup                                   // 数组赋值操作，和上同。
     * 25 iconst_4
     * 26 ldc #7 <AF>
     * 28 aastore
     * 29 astore_1                              // 将栈顶元素数组引用存储到 slot1
     * 30 aload_1                               // 将 slot1 的数组引用入栈
     * 31 astore_2                              // 将栈顶元素数组引用存储到 slot2
     * 32 aload_2                               // 将 slot2 的数组引用入栈
     * 33 arraylength                           // 获取数组长度。数组引用入栈，int 类型长度入栈。
     * 34 istore_3                              // 将数组长度出栈存储到 slot3
     * 35 iconst_0                              // 数字 0 入栈
     * 36 istore 4                              // 栈顶元素数字 0 出栈，保存到 slot4 中
     * 38 iload 4                               // 将 slot4 的元素入栈，此处是索引
     * 40 iload_3                               // 将 slot3 的元素入栈，此处是长度
     * 41 if_icmpge 64 (+23)                    // 出栈两个元素。先 length，再 index，如果 index ≥ length，则跳转到 64 行
     * 44 aload_2                               // 将 slot2 的数组引用入栈。
     * 45 iload 4                               // 将 slot4 的元素入栈，此处是索引
     * 47 aaload                                // 出栈两个元素，分别是 index，数组引用。以获取数组元素入栈。
     * 48 astore 5                              // 将出栈的数组元素保存至 slot5 中。
     * 50 getstatic #8 <java/lang/System.out>   // 获取 System.out
     * 53 aload 5                               // 将 slot5 元素入栈，此处是刚刚出栈的数组元素。
     * 55 invokevirtual #9 <java/io/PrintStream.println>    // 调用 println 方法，并出栈该方法需要的参数。
     * 58 iinc 4 by 1                           // 将 slot4 自增 1，操作数栈无改变。
     * 61 goto 38 (-23)                         // 跳转执行 38 行。
     * 64 return                                // 返回
     */
    void foreachArray() {
        String[] strings = {"AB", "AC", "AD", "AE", "AF"};
        for (String string : strings) {
            System.out.println(string);
        }
    }

    /**
     * 0 iconst_5                                   // 此处一直到 28 行和上同
     * 1 anewarray #2 <java/lang/String>
     * 4 dup
     * 5 iconst_0
     * 6 ldc #3 <AB>
     * 8 aastore
     * 9 dup
     * 10 iconst_1
     * 11 ldc #4 <AC>
     * 13 aastore
     * 14 dup
     * 15 iconst_2
     * 16 ldc #5 <AD>
     * 18 aastore
     * 19 dup
     * 20 iconst_3
     * 21 ldc #6 <AE>
     * 23 aastore
     * 24 dup
     * 25 iconst_4
     * 26 ldc #7 <AF>
     * 28 aastore
     * 29 invokestatic #10 <java/util/Arrays.asList>        // 调用 asList 静态方法，出栈数组引用，并入栈 list 的引用
     * 32 astore_1                                          // 将 list 的引用保存至 slot1
     * 33 aload_1                                           // 将 slot1 的 list 引用入栈
     * 34 invokeinterface #11 <java/util/List.iterator> count 1     // 出栈 list 引用，调用 iterator 方法。并入栈 iterator 引用
     * 39 astore_2                                          // 将 iterator 引用保存至 slot2
     * 40 aload_2                                           // 将 slot2 入栈
     * 41 invokeinterface #12 <java/util/Iterator.hasNext> count 1  // 出栈 iterator 引用，并调用 hasNext 方法。并入栈返回值。
     * 46 ifeq 69 (+23)                                     // 出栈栈顶元素，如果等于 0，也就是 false，则跳转到 69。
     * 49 aload_2                                           // 将 slot2 入栈。
     * 50 invokeinterface #13 <java/util/Iterator.next> count 1     // 出栈 iterator 引用，并调用 next 方法。将返回值入栈。
     * 55 checkcast #2 <java/lang/String>                   // 类型校验，是否符合 String 类型，如果不符合，将抛出异常。
     * 58 astore_3                                          // 出栈 String 引用并保存到 slot3 中。
     * 59 getstatic #8 <java/lang/System.out>               // 获取 System.out，并入栈
     * 62 aload_3                                           // 将 slot3 的数组元素入栈
     * 63 invokevirtual #9 <java/io/PrintStream.println>    // 调用 println 方法，出栈数组元素和 System.out
     * 66 goto 40 (-26)                                     // 跳转到 40 行，进行循环，
     * 69 return                                            // 返回
     */
    void foreachList() {
        List<String> strings = Arrays.asList("AB", "AC", "AD", "AE", "AF");
        for (String string : strings) {
            System.out.println(string);
        }
    }

    public static void main(String[] args) {
        new ForEachBytecode();
    }
}
