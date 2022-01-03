package com.future.concurrent.jmm;

/**
 * 单例模式
 *
 * @author future
 */
@SuppressWarnings("all")
public class SingleObject {

    /** 需要使用 volatile 禁止字节码重排序，
     * 否则，putstatic 指令 可能在构造函数指令 init() 之前，
     * 可能使其他线程拿到了没有调用过构造函数的指针。从而引发对象使用安全问题。
     */
    private /*volatile*/ static SingleObject sInstance = null;

    private SingleObject() {
    }


    /**
     * 下面的方法生成的字节码如下所示
     *
     *  0 getstatic #10 <com/future/concurrent/jmm/SingleObject.sInstance>
     *  3 ifnonnull 35 (+32)
     *  6 ldc #1 <com/future/concurrent/jmm/SingleObject>       // 加载类对象
     *  8 dup                                                  // 复制引用
     *  9 astore_0                                             // 将复制的引用存储到局部变量表 0 号位置。
     * 10 monitorenter
     * 11 getstatic #10 <com/future/concurrent/jmm/SingleObject.sInstance>
     * 14 ifnonnull 27 (+13)
     * 17 new #1 <com/future/concurrent/jmm/SingleObject>
     * 20 dup
     * 21 invokespecial #20 <com/future/concurrent/jmm/SingleObject.<init>>   // 这里可能和下面一行代码进行重排序
     * 24 putstatic #10 <com/future/concurrent/jmm/SingleObject.sInstance>    // synchronize 不保证内部代码不会进行指令重排
     * 27 aload_0
     * 28 monitorexit
     * 29 goto 35 (+6)
     * 32 aload_0
     * 33 monitorexit
     * 34 athrow
     * 35 getstatic #10 <com/future/concurrent/jmm/SingleObject.sInstance>
     * 38 areturn
     */
    public static SingleObject getInstance() {
        if (sInstance == null) {
            synchronized (SingleObject.class) {
                if (sInstance == null) {
                    sInstance = new SingleObject();
                }
            }
        }
        return sInstance;
    }


}
