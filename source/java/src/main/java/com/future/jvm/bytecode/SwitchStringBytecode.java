package com.future.jvm.bytecode;

/**
 * Switch-String 的字节码分析
 * <p>
 * 该字节码分析结果说明，Switch-String 的语法实际执行效果为
 * 先调用 String 对象的 hashCode 值，再比较其 equals 值。
 * 然后决定其稀疏程度采用 tableSwitch 还是 lookupSwitch 得到 matchCase 的值。
 * 最后再通过 matchCase 的 tableSwitch 来得到最终的执行代码块。
 *
 * @author future
 */
@SuppressWarnings("all")
class SwitchStringBytecode {

    /**
     * slots: 4, stacks: 2, bytecode_length=124
     * <p>
     * 0 iconst_0  // 将 int 类型的常量 0 入栈到操作数栈中
     * 1 istore_1   // 将 0 存入 slot1
     * 2 aload_0    // slot0 的 reference 值加载入操作数栈
     * 3 astore_2   // 弹出操作数栈，存储到 slot2
     * 4 iconst_m1  // 将 常量 -1 入栈到操作数栈中
     * 5 istore_3   // 弹出操作数栈，保存到 slot3 中
     * 6 aload_2    // 将 slot2 中加载一个 reference 类型值到操作数栈
     * 7 invokevirtual #2 <java/lang/String.hashCode>  //调用操作数栈的顶部的值的 hashCode 方法，并将返回值入栈。
     * 10 tableswitch 65 to 67	65:  36 (+26)   // 根据索引值在跳转表中寻找配对的分支并进行跳转，如果栈顶元素为 A: 跳到 36 行
     * 66:  50 (+40)                            // B 调到 50 行
     * 67:  64 (+54)                            // C 跳到 64 行
     * default:  75 (+65)                       // default 跳转到 75 行
     * 36 aload_2                               // 加载 slot2 的数值并压入操作数栈
     * 37 ldc #3 <A>                            // 将 字符串 A 入栈
     * 39 invokevirtual #4 <java/lang/String.equals>    // 弹出栈顶元素并调用 equals 方法
     * 42 ifeq 75 (+33)                                 // 如果相等 则跳转到 75 行
     * 45 iconst_0                                      // 加载常量 0 到操作数栈
     * 46 istore_3                                      // 弹出栈顶元素并保存至 slot3
     * 47 goto 75 (+28)                                 // 跳转到 75 行
     * 50 aload_2
     * 51 ldc #5 <B>                                    // 将字符串 B 入栈
     * 53 invokevirtual #4 <java/lang/String.equals>    // 弹出操作数栈并调用 equals 方法
     * 56 ifeq 75 (+19)                                 // 如果相等，则跳转到 75 行
     * 59 iconst_1                                      // 将 1 入栈
     * 60 istore_3                                      // 弹出栈顶元素并保存至 slot3
     * 61 goto 75 (+14)                                 // 跳转到 75 行
     * 64 aload_2                                       // slot2 的元素入栈
     * 65 ldc #6 <C>                                    // 字符串 C 入栈
     * 67 invokevirtual #4 <java/lang/String.equals>    // 弹出栈顶，并调用 equals 方法
     * 70 ifeq 75 (+5)                                  // 如果相等，则跳转到 75 行
     * 73 iconst_2                                      // 将 2 入栈
     * 74 istore_3                                      // 弹出栈顶元素保存至 slot3
     * 75 iload_3                                       // 将 slot3 的元素入栈
     * 76 tableswitch 0 to 2	0:  104 (+28)           // 跳转表。如果栈顶元素为 0 则跳转到 104 行。
     * 1:  110 (+34)                                    // 如果 1，则跳转到 110 行。
     * 2:  116 (+40)                                    // 如果 2，则跳转到 116 行。
     * default:  119 (+43)                              // 否则，跳转到 119 行。
     * 104 bipush 10                                    // 将 10 入栈
     * 106 istore_1                                     // 出栈元素保存至 slot1
     * 107 goto 122 (+15)                               // 跳转到 122 行
     * 110 bipush 20                                    //  将 20 入栈
     * 112 istore_1                                     // 出栈元素保存至 slot1
     * 113 goto 122 (+9)                                // 跳转到 122 行
     * 116 goto 122 (+6)                                // 跳转到 122 行
     * 119 bipush 30                                    // 将 30 入栈
     * 121 istore_1                                     // 出战元素保存至 slot1
     * 122 iload_1                                      // 将 slot1 元素入栈
     * 123 ireturn                                      // 返回
     * <p>
     * localVariableTable:
     * No from length  seq     name           description
     * 0	0	124  	0	cp_info #21 (s)	 cp_info #22 (Ljava/lang/String)
     * 1	2	122	    1	cp_info #23	(i)  cp_info #24 (I)
     */
    static int switchString(String s) {
        int i = 0;
        switch (s) {
            case "A":
                i = 10;
                break;
            case "B":
                i = 20;
                break;
            case "C":
                break;
            default:
                i = 30;
                break;
        }
        return i;
    }

    /**
     * 0 bipush 10
     * 2 istore_1
     * 3 aload_0
     * 4 dup
     * 5 astore_2
     * 6 invokevirtual #16 <java/lang/String.hashCode : ()I>
     * 9 lookupswitch 3
     * -2041707231:  44 (+35)
     * 2301506:  56 (+47)
     * 803262031:  68 (+59)
     * default:  98 (+89)
     * 44 aload_2
     * 45 ldc #38 <Kotlin>
     * 47 invokevirtual #24 <java/lang/String.equals : (Ljava/lang/Object;)Z>
     * 50 ifne 92 (+42)
     * 53 goto 98 (+45)
     * 56 aload_2
     * 57 ldc #40 <Java>
     * 59 invokevirtual #24 <java/lang/String.equals : (Ljava/lang/Object;)Z>
     * 62 ifne 80 (+18)
     * 65 goto 98 (+33)
     * 68 aload_2
     * 69 ldc #42 <Android>
     * 71 invokevirtual #24 <java/lang/String.equals : (Ljava/lang/Object;)Z>
     * 74 ifne 86 (+12)
     * 77 goto 98 (+21)
     * 80 bipush 20
     * 82 istore_1
     * 83 goto 101 (+18)
     * 86 bipush 30
     * 88 istore_1
     * 89 goto 101 (+12)
     * 92 bipush 50
     * 94 istore_1
     * 95 goto 101 (+6)
     * 98 bipush 60
     * 100 istore_1
     * 101 iload_1
     * 102 ireturn
     */
    static int switchString2(String s) {
        int i = 10;
        switch (s) {
            case "Java":
                i = 20;
                break;
            case "Android":
                i = 30;
                break;
            case "Kotlin":
                i = 50;
                break;
            default:
                i = 60;
        }
        return i;
    }

    public static void main(String[] args) {
        int r = switchString("A");
        System.out.println(r);
    }
}
