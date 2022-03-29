package com.future.jvm.bytecode;

/**
 * 默认构造器的字节码分析
 * <p>
 * 0 aload_0                                        // 入栈。加载 slot0，也就是 this 引用
 * 1 invokespecial #8 <java/lang/Object.<init>>     // 出栈首元素，调用构造函数 init()
 * 4 return                                         // 返回
 *
 * @author future
 */
@SuppressWarnings("all")
public class DefaultConstructorBytecode {

}
