package com.future.jvm.bytecode;

/**
 * 枚举的字节码分析
 * <p>
 * * clinit 方法:
 * 0 new #1 <com/future/jvm/EnumBytecode>                       // 入栈。new EnumAnalyzer，并将引用入栈
 * 3 dup                                                        // 入栈。复制栈顶元素。即枚举对象的引用。
 * 4 ldc #14 <RED>                                              // 入栈。常量 RED
 * 6 iconst_0                                                   // 入栈。常量 0
 * 7 invokespecial #15 <com/future/jvm/EnumBytecode.<init>>     // 出栈三个参数，调用构造函数。无返回值，
 * 10 putstatic #19 <com/future/jvm/EnumBytecode.RED>           // 出栈。用第 0 行的引用给静态字段 RED 赋值。
 * 13 new #1 <com/future/jvm/EnumBytecode>                      // 入栈。new EnumBytecode 对象的引用。
 * 16 dup                                                       // 此处至 36 行逻辑和上面一样。
 * 17 ldc #21 <BLUE>
 * 19 iconst_1
 * 20 invokespecial #15 <com/future/jvm/EnumBytecode.<init>>
 * 23 putstatic #22 <com/future/jvm/EnumBytecode.BLUE>
 * 26 new #1 <com/future/jvm/EnumAnalyzer>
 * 29 dup
 * 30 ldc #24 <GREEN>
 * 32 iconst_2
 * 33 invokespecial #15 <com/future/jvm/EnumBytecode.<init>>
 * 36 putstatic #25 <com/future/jvm/EnumBytecode.GREEN>
 * 39 iconst_3                                                  // 入栈。将常量 3
 * 40 anewarray #1 <com/future/jvm/EnumBytecode>                // 出栈数组长度以构造 EnumAnalyzer 数组，并将数组引用入栈，
 * 43 dup                                                       // 入栈。复制数组引用。
 * 44 iconst_0                                                  // 入栈，数字 0。
 * 45 getstatic #19 <com/future/jvm/EnumBytecode.RED>           // 入栈。获取静态字段 RED
 * 48 aastore                                                   // 出栈三个元素。数组元素和数组索引，并将数组赋值。
 * 49 dup                                                       // 此处至 60 行，和上同，给数组元素赋值。
 * 50 iconst_1
 * 51 getstatic #22 <com/future/jvm/EnumBytecode.BLUE>
 * 54 aastore
 * 55 dup
 * 56 iconst_2
 * 57 getstatic #25 <com/future/jvm/EnumBytecode.GREEN>
 * 60 aastore
 * 61 putstatic #27 <com/future/jvm/EnumBytecode.ENUM$VALUES>   // 出栈，用 40 行的数组引用给静态字段 VALUES 赋值
 * 64 return                                                    // 返回 void。
 * <p>
 * init 方法：
 * 0 aload_0                                    // 入栈 slot0 的引用，即 this
 * 1 aload_1                                    // 入栈 slot1 的引用，即构造函数的第 1 个参数
 * 2 iload_2                                    // 入栈 slot2 的数字，即构造函数的第 2 个参数
 * 3 invokespecial #31 <java/lang/Enum.<init>>  // 出栈三个元素。调用父类 Enum 的构造函数
 * 6 return                                     // 返回 void。即无返回值
 * <p>
 * values 方法：
 * 0 getstatic #27 <com/future/jvm/EnumBytecode.ENUM$VALUES>    // 入栈，获取 VALUES 的引用。
 * 3 dup                                                        // 入栈。复制 VALUES 引用
 * 4 astore_0                                                   // 出栈。存储到 slot0
 * 5 iconst_0                                                   // 入栈。数字 0
 * 6 aload_0                                                    // 入栈。slot0 的元素
 * 7 arraylength                                                // 出栈，数组引用，获取数组长度，并入栈。
 * 8 dup                                                        // 入栈。复制数组长度。
 * 9 istore_1                                                   // 出栈。将数组长度保存到 slot1
 * 10 anewarray #1 <com/future/jvm/EnumBytecode>                // 出栈。用数组长度构造 EnumAnalyzer 数组。并将引用入栈。
 * 13 dup                                                       // 入栈。复制数组引用
 * 14 astore_2                                                  // 出栈，将数组引用保存至 slot2
 * 15 iconst_0                                                  // 入栈。数字 0
 * 16 iload_1                                                   // 入栈。slot1 的元素
 * 17 invokestatic #35 <java/lang/System.arraycopy>             // 出栈 5 个参数。调用 System.arraycopy(). 无返回值。
 * 20 aload_2                                                   // 入栈。slot2 的数组引用
 * 21 areturn                                                   // 返回数组引用。
 * <p>
 * valueOf 方法：
 * 0 ldc #1 <com/future/jvm/EnumBytecode>                       // 入栈。EnumAnalyzer 的类信息。
 * 2 aload_0                                                    // 入栈。slot0 的元素
 * 3 invokestatic #43 <java/lang/Enum.valueOf>                  // 出栈两个参数。调用 Enum.valueOf 方法，并将返回值入栈。
 * 6 checkcast #1 <com/future/jvm/EnumBytecode>                 // 出栈并入栈操作（如果通过校验，则等效于无操作）。校验返回值是否属于 EnumAnalyzer 类型
 * 9 areturn                                                    // 返回 EnumAnalyzer 引用。
 *
 * @author future
 */
@SuppressWarnings("all")
enum EnumBytecode {
    /*
     * 等价于如下代码。
     *
     * public final class EnumBytecode extends Enum<EnumBytecode> {
     *      public static final EnumBytecode RED;
     *      public static final EnumBytecode BLUE;
     *      public static final EnumBytecode GREEN;
     *      private static final EnumBytecode[] $VALUES;
     *
     *      static {
     *          RED = new EnumBytecode("RED", 0);
     *          BLUE = new EnumBytecode("BLUE", 1);
     *          GREEN = new EnumBytecode("GREEN", 2);
     *          $VALUES = new EnumBytecode[] {"RED", "BLUE", "GREEN"};
     *      }
     *
     *      private EnumBytecode(String name, int ordinal) {
     *          super(name, ordinal);
     *      }
     *
     *      public static EnumAnalyzer[] values() {
     *          EnumBytecode[] x = new EnumBytecode[VALUES.length];
     *          return System.arraycopy(VALUES, 0, x, 0, VALUES.length);
     *      }
     *
     *      public static EnumBytecode valueOf(String name) {
     *          return Enum.valueOf(EnumBytecode.class, name);
     *      }
     *
     * }
     */

    RED, BLUE, GREEN
}

@SuppressWarnings("all")
class Runner {

    /*
     *  0 bipush 10             // 入栈。数字 10
     *  2 istore_2              // 出栈，将 10 存入 slot2
     *  3 invokestatic #18 <com/future/jvm/Runner.$SWITCH_TABLE$com$future$jvm$EnumBytecode>    // 调用无参静态方法，入栈返回值，int[] 数组的引用。
     *  6 aload_1                                                       // 入栈。slot1 的引用。即参数 EnumAnalyzer e
     *  7 invokevirtual #21 <com/future/jvm/EnumAnalyzer.ordinal>       // 出栈。即调用 e.ordinal() 方法。并将返回值入栈。
     * 10 iaload                                                        // 出栈两个元素，分别是 index, arrayref, 并将 arrayref[index] 的值入栈。
     * 11 tableswitch 1 to 3	1:  36 (+25)                            // 出栈。即刚刚被压入的数组元素。如果是 1，则跳转 36 行。
     * 	2:  42 (+31)                                                    // 如果是 2，则跳转到 42 行
     * 	3:  48 (+37)                                                    // 如果是 3，则跳转到 48 行
     * 	default:  52 (+41)                                              // 否则，跳转到 52 行
     * 36 bipush 20                                                     // 入栈。数字 20
     * 38 istore_2                                                      // 出栈。存储到 slot2
     * 39 goto 52 (+13)                                                 // 跳转至 52 行。
     * 42 bipush 100
     * 44 istore_2
     * 45 goto 52 (+7)
     * 48 sipush 500
     * 51 istore_2
     * 52 getstatic #27 <java/lang/System.out>                          // 入栈。获取 System.out 的引用
     * 55 iload_2                                                       // 将 slot2 元素入栈。
     * 56 invokevirtual #33 <java/io/PrintStream.println>               // 出栈两个元素。分别是函数参数，System.out 的引用。无返回值
     * 59 return                                                        // 返回 void
     */
    void run(EnumBytecode e) {
        int a = 10;
        switch (e) {
            case RED:
                a = 20;
                break;
            case BLUE:
                a = 100;
                break;
            case GREEN:
                a = 500;
                break;
        }
        System.out.println(a);
    }

    /*
     *  0 getstatic #54 <com/future/jvm/Runner.$SWITCH_TABLE$com$future$jvm$EnumBytecode>       // 入栈。获取 static 字段，即 int[] 的引用
     *  3 dup                                                                                   // 入栈。复制 int[] 数组的引用
     *  4 ifnull 8 (+4)                                                                         // 出栈。如果是 null,则跳转到第 8 行
     *  7 areturn                                                                               // 如果不为 null，返回 int[] 数组的引用。
     *  8 pop                                                                                   // 出栈一个元素。此处是清空栈。
     *  9 invokestatic #56 <com/future/jvm/EnumAnalyzer.values>                                 // 入栈。返回值为新的 EnumAnalyzer 数组引用。调用 EnumAnalyzer.values 的无参静态方法。
     * 12 arraylength                                                                           // 出栈数组引用。入栈数组长度。
     * 13 newarray 10 (int)                                                                     // 出栈数组长度，入栈新数组的引用。构造一个新的 int 数组。(10 代表类型 int)
     * 15 astore_0                                                                              // 出栈。将新的 int 数组引用存入 slot0
     * 16 aload_0                                                                               // 入栈。获取 slot0 的 int[] 引用
     * 17 getstatic #47 <com/future/jvm/EnumAnalyzer.BLUE>                                      // 入栈。获取静态变量 EnumAnalyzer.BLUE 的引用
     * 20 invokevirtual #21 <com/future/jvm/EnumAnalyzer.ordinal>                               // 出栈。即 EnumAnalyzer.BLUE，调用其 ordinal() 方法。返回 int 值入栈。
     * 23 iconst_2                                                                              // 入栈。数字 2
     * 24 iastore                                                                               // 出栈三个元素。分别是 value, index, arrayref。即 arrayref[index] = value。
     * 25 goto 29 (+4)                                                                          // 跳转到 29 行。
     * 28 pop                                                                                   // 出栈。
     * 29 aload_0                                                                               // 入栈。读取 slot0 的 int[] 数组引用
     * 30 getstatic #60 <com/future/jvm/EnumAnalyzer.GREEN>                                     // 直至 54 行均和上同，填充 int[] 数组
     * 33 invokevirtual #21 <com/future/jvm/EnumAnalyzer.ordinal>
     * 36 iconst_3
     * 37 iastore
     * 38 goto 42 (+4)
     * 41 pop
     * 42 aload_0
     * 43 getstatic #63 <com/future/jvm/EnumAnalyzer.RED>
     * 46 invokevirtual #21 <com/future/jvm/EnumAnalyzer.ordinal>
     * 49 iconst_1
     * 50 iastore
     * 51 goto 55 (+4)
     * 54 pop
     * 55 aload_0                                                                               // 入栈。获取 int[] 数组的引用
     * 56 dup                                                                                   // 入栈。复制 int[] 数组的引用。
     * 57 putstatic #54 <com/future/jvm/Runner.$SWITCH_TABLE$com$future$jvm$EnumBytecode>       // 出栈。给静态变量赋值。
     * 60 areturn                                                                               // 返回栈顶元素，即 int[] 引用。
     *
     * 自动生成方法：
     * 方法签名: $SWITCH_TABLE$com$future$jvm$EnumAnalyzer()[I
     * 方法访问标志：static 无参，返回 int 数组
     *
     * 自动生成的变量：
     * 字段签名：[I $SWITCH_TABLE$com$future$jvm$EnumBytecode
     * 字段访问标志：private static volatile
     */

    /*
     *  上述代码等价于：
     *  private static volatile int[] $SWITCH_TABLE$com$future$jvm$EnumBytecode;
     *
     *  static int[] $SWITCH_TABLE$com$future$jvm$EnumBytecode() {
     *      int[] a = $SWITCH_TABLE$com$future$jvm$EnumBytecode;
     *      if (a == null) {
     *          EnumAnalyzer[] e = EnumAnalyzer.values();
     *          a = new int[e.length];
     *          a[EnumAnalyzer.BLUE.ordinal()] = 2;
     *          a[EnumAnalyzer.GREEN.ordinal()] = 3;
     *          a[EnumAnalyzer.RED.ordinal()] = 1;
     *          $SWITCH_TABLE$com$future$jvm$EnumAnalyzer = a;
     *      }
     *      return a;
     *  }
     */

    /**
     *  0 new #1 <com/future/jvm/Runner>                    // 入栈。new Runner 的对象引用
     *  3 dup                                               // 入栈。复制 runner 引用
     *  4 invokespecial #46 <com/future/jvm/Runner.<init>>  // 出栈。调用 Runner 无参构造函数。
     *  7 getstatic #47 <com/future/jvm/EnumAnalyzer.BLUE>  // 入栈。获取 EnumAnalyzer.BLUE
     * 10 invokevirtual #50 <com/future/jvm/Runner.run>     // 出栈两个元素。一个参数，一个 runner 引用。无返回值。
     * 13 return                                            // 返回 void
     */
    public static void main(String[] args) {
        new Runner().run(EnumBytecode.BLUE);
    }
}
