package com.future.jvm;

/**
 * StringTable 在 Java6 中存放在常量池永久代中。而在 Java8 中位置在堆中。迁移原因是垃圾回收的原因。放在堆中容易被回收。
 * <p>
 * Java6 中设置永久代参数: -XX:MaxPermSize=8m
 * Java8 中设置元空间大小: -XX:MaxMetaspaceSize=8m
 * <p>
 * StringTable 池及 String.intern() 方法的使用主要用处在于。
 * 如果有大量的字符串，并且其中大很多重复，可以考虑使用 intern() 复用 StringTable 中的对象，以减少内存使用。
 */
@SuppressWarnings("all")
class StringTablePractice {

    /**
     * 演示 StringTable 垃圾回收
     * -Xmx10m 设置最大堆 10m 。
     * -XX:+PrintStringTableStatistics 设置打印 StringTable 的统计信息。
     * -XX:+PrintGCDetails -verbose:gc 设置打印 GC 的详细信息。包括次数、花费时间等等
     * 下列参数可调优
     * -XX:StringTableSize=200000 设置 StringTable 桶的个数。
     */
    void print() {
        int i = 0;
        try {
            for (int j = 0; j < 10000; j++) {
                String.valueOf(i).intern();
                i++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println(i);
        }
    }

    public void test() {
        String s1 = "a";
        String s2 = "b";
        // 此处编译器会优化，直接放进串池中。
        String s3 = "a" + "b";
        String s4 = s1 + s2;
        String s5 = "ab";
        // 1.8 中 intern 方法尝试将字符串放入串池，放入成功后，s4 指的就是串池中的对象。如果有则不放入，并返回串池中的字符串。
        // 1.6 中 intern 方法区别在于，如果串池中没有，则复制一份放入串池中。
        String s6 = s4.intern();

        System.out.println(s3 == s4); // false
        System.out.println(s3 == s5); // true
        System.out.println(s3 == s6); // true

        // 此处 x2 是堆中的对象。由 StringBuilder 生成。字符串拼接 + 默认是 StringBuilder 完成的。
        String x2 = new String("c") + new String("d");
        String x1 = "cd";
        x2.intern();

        // 如果调换了最后两行代码的位置呢？
        // 如果是 jdk1.6 呢？
        System.out.println(x1 == x2); // false
    }

    public static void main(String[] args) {
        StringTablePractice practice = new StringTablePractice();
//        practice.test();
        practice.print();
    }
}
