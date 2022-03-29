package com.future.jvm;

import com.future.bean.Book;
import com.future.bean.JavaBook;

/**
 * 类加载过程
 */
@SuppressWarnings("all")
class ClassLoadProcess {

    /**
     * 0 getstatic #15 <com/future/bean/JavaBook.JAVA_BOOK_MEMBER>
     * 3 istore_1
     * 4 getstatic #21 <java/lang/System.out>
     * 7 iload_1
     * 8 invokevirtual #27 <java/io/PrintStream.println>
     * 11 return
     * <p>
     * 日志表明，其子类和父类都会被初始化。即 clinit 都会被执行。
     * 因为初始化子类的过程会先初始化其父类。
     */
    private void initBoth() {
        int member = JavaBook.JAVA_BOOK_MEMBER;
        System.out.println(member);
    }

    /**
     * 0 getstatic #15 <com/future/bean/JavaBook.BOOK_MEMBER>
     * 3 istore_1
     * 4 getstatic #21 <java/lang/System.out>
     * 7 iload_1
     * 8 invokevirtual #27 <java/io/PrintStream.println>
     * 11 return
     * <p>
     * 索引为 2 的字节码说明，对 JavaBook 进行加载，但未对其进行初始化。
     * 通过日志表明，其父类 Book 会被初始化。
     */
    public void initParent() {
        int member = JavaBook.BOOK_MEMBER;
        System.out.println(member);
    }

    /**
     * 0 iconst_1
     * 1 istore_1
     * 2 bipush 10
     * 4 anewarray #7 <com/future/bean/Book>
     * 7 astore_2
     * 8 getstatic #3 <java/lang/System.out>
     * 11 iload_1
     * 12 invokevirtual #4 <java/io/PrintStream.println>
     * 15 return
     * <p>
     * 以上字节码说明，读取 TYPE_JAVA 没有对 JavaBook 和 Book 做类的初始化。
     * 因为 TYPE_JAVA 使用 static final 修饰的基本类型，在准备阶段就用常量池的值对其赋值。
     * <p>
     * 注意 anewarray 也不会对其进行初始化。而是由 JVM 创建一个数组。
     */
    public void unInitBoth() {
        int member = JavaBook.TYPE_JAVA;
        Book[] books = new Book[10];
        System.out.println(member);
    }

    /**
     * 下列几种情况中，将会触发类的初始化
     * 1. 虚拟机启动时，会先初始化这个含有 main 方法的主类
     * 2. 遇到 new、getstatic、putstatic、invokestatic 的字节码指令时。
     * 3. 当初始化类的时候，发现其父类还没有进行过初始化，则需要先触发器父类的初始化。
     * 4. 使用 java.lang.reflect 包的方法对类型进行反射调用的时候，需要先触发其初始化。
     * 5. 当一个接口定义了 JDK8 新加入的默认方法（default 修饰），如果有这个接口的实现类发生了初始化，该接口要在其之前被初始化。
     * 6. 当使用 JDK7 加入的动态语言支持时，如果一个 java.lang.invoke.MethodHandler 实例最后的解析结果为 REF_getStatic 等的方法句柄，
     * * 并且这个方法句柄对应的类还没有被初始化，则需要先触发其初始化。
     */
    public static void main(String[] args) {
        ClassLoadProcess process = new ClassLoadProcess();
        process.unInitBoth();
    }

}
