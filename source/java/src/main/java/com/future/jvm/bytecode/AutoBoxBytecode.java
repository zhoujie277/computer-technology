package com.future.jvm.bytecode;

/**
 * 自动拆装箱操作
 *
 * @author future
 */
@SuppressWarnings("all")
class AutoBoxBytecode {

    /**
     * 0 iconst_1                                       // 入栈常量 1
     * 1 invokestatic #16 <java/lang/Integer.valueOf>   // 将参数 1 出栈，将返回值入栈。调用 Integer.valueOf，将 Integer 的引用入栈。
     * 4 astore_1                                       // 出栈。将 Integer 的引用存储到 slot1
     * 5 aload_1                                        // 入栈。压入 slot1 的 Integer 对象引用
     * 6 invokevirtual #22 <java/lang/Integer.intValue> // 出栈。调用 intValue 的方法。无参，将返回值 int 入栈。
     * 9 istore_2                                       // 将返回值保存至 slot2
     * 10 return                                        // 返回
     */
    static void base() {
        Integer x = 1;          // 该行代码等价于 Integer x = Integer.valueOf(1);
        int y = x;              // 该行代码等价于 int y = x.intValue();
    }

    /**
     *   0 iconst_1                                                                 // 入栈。常量 1
     *   1 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>     // 出栈 int，入栈 Integer。调用静态方法 valueOf
     *   4 astore_0                                                                 // 出栈，存储到 slot0
     *   5 iconst_2                                                                 // 入栈，常量 2
     *   6 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>
     *   9 astore_1                                                                 // 将 Integer(2) 的引用存储到 slot1
     *  10 iconst_3
     *  11 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>
     *  14 astore_2                                                                 // 将 Integer(3) 的引用存储到 slot2
     *  15 iconst_3
     *  16 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>
     *  19 astore_3                                                                 // 将 Integer(3) 的引用存储到 slot3
     *  20 sipush 321
     *  23 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>
     *  26 astore 4                                                                 // 将 new Integer(321) 的引用存储到 slot4
     *  28 sipush 321
     *  31 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>
     *  34 astore 5                                                                 // 将 new Integer(321) 的引用存储到 slot5
     *  36 ldc2_w #4 <3>
     *  39 invokestatic #6 <java/lang/Long.valueOf : (J)Ljava/lang/Long;>           // 出栈并入栈 Long(3) 的引用，调用静态方法 Long.valueOf()
     *  42 astore 6                                                                 // 将 Long(3) 的引用存储到 slot6
     *  44 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>              // 入栈。获取静态字段 System.out 的引用
     *  47 aload_2
     *  48 aload_3
     *  49 if_acmpne 56 (+7)                                                        // 比较 c == d
     *  52 iconst_1
     *  53 goto 57 (+4)                                                             // 跳转到第 57 行，打印 1 (true)
     *  56 iconst_0
     *  57 invokevirtual #8 <java/io/PrintStream.println : (Z)V>
     *  60 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>
     *  63 aload 4
     *  65 aload 5
     *  67 if_acmpne 74 (+7)                                                        // 比较 e == f
     *  70 iconst_1
     *  71 goto 75 (+4)
     *  74 iconst_0
     *  75 invokevirtual #8 <java/io/PrintStream.println : (Z)V>
     *  78 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>
     *  81 aload_2
     *  82 invokevirtual #3 <java/lang/Integer.intValue : ()I>                      // 获取 slot2 的引用，调用 intValue() 方法。
     *  85 aload_0
     *  86 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     *  89 aload_1
     *  90 invokevirtual #3 <java/lang/Integer.intValue : ()I>                      // 在相加之前，先转成基本类型。
     *  93 iadd
     *  94 if_icmpne 101 (+7)                                                       // 比较 c == (a + b)
     *  97 iconst_1
     *  98 goto 102 (+4)
     * 101 iconst_0
     * 102 invokevirtual #8 <java/io/PrintStream.println : (Z)V>
     * 105 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>
     * 108 aload_2
     * 109 aload_0
     * 110 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     * 113 aload_1
     * 114 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     * 117 iadd                                                                     // 相加之前，先转成基本类型。
     * 118 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>     // 将 a + b 的结果转成 Integer 对象
     * 121 invokevirtual #9 <java/lang/Integer.equals : (Ljava/lang/Object;)Z>      // 出栈两个元素，将返回值入栈。出栈的元素一个为 a + b 的结果，一个为 c 的引用。调用 equals
     * 124 invokevirtual #8 <java/io/PrintStream.println : (Z)V>
     * 127 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>
     * 130 aload 6
     * 132 invokevirtual #10 <java/lang/Long.longValue : ()J>                       // 转成 long 的基本类型
     * 135 aload_0
     * 136 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     * 139 aload_1
     * 140 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     * 143 iadd                                                                     // 相加之前，先转成基本类型
     * 144 i2l                                                                      // int 类型转成 long 类型
     * 145 lcmp                                                                     // long 基本类型比较
     * 146 ifne 153 (+7)
     * 149 iconst_1
     * 150 goto 154 (+4)
     * 153 iconst_0
     * 154 invokevirtual #8 <java/io/PrintStream.println : (Z)V>                    // 打印 g == (a + b) 的结果
     * 157 getstatic #7 <java/lang/System.out : Ljava/io/PrintStream;>
     * 160 aload 6
     * 162 aload_0
     * 163 invokevirtual #3 <java/lang/Integer.intValue : ()I>                          //
     * 166 aload_1
     * 167 invokevirtual #3 <java/lang/Integer.intValue : ()I>
     * 170 iadd                                                                         // 相加之前，先转成基本类型。
     * 171 invokestatic #2 <java/lang/Integer.valueOf : (I)Ljava/lang/Integer;>         // 将 int 转为 Integer 对象
     * 174 invokevirtual #11 <java/lang/Long.equals : (Ljava/lang/Object;)Z>            // Long.equals(Integer), return false.
     * 177 invokevirtual #8 <java/io/PrintStream.println : (Z)V>
     * 180 return
     */
    static void practice() {
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;
        System.out.println(c == d);             // true, 由于享元的缘故，即 Integer.valueOf()
        System.out.println(e == f);             // false, 由于默认享元的范围只有 -128~127。

        System.out.println(c == (a + b));       // true。由于最终会转成基本类型比较。此处会转成基本类型 int 相加。然后 (Integer == int) 也是采用基本类型比较。
        System.out.println(c.equals(a + b));    // true。由于享元的缘故，虽都是 Integer，值相等，且是同一个对象。
        System.out.println(g == (a + b));       // true。最终是 long 的基本类型比较。
        System.out.println(g.equals(a + b));    // false。最终是 Long.equals(Integer)
    }

    static void cmpLongInteger() {
        Long a = 3L;
        Integer b = 3;
        System.out.println(a.equals(b)); // false。 不同的对象
    }

    public static void main(String[] args) {
//        practice();
        cmpLongInteger();
    }
}
