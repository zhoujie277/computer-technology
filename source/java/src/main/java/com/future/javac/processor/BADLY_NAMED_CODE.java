package com.future.javac.processor;

/**
 * 插入式注解管理处理器
 * <p>
 * 包含了不合 Java 语言规范的代码示例，供
 * 供同目录下 NameCheckProcessor 测试使用。
 * <p>
 * 测试命令如下
 *
 * javac -processor com.future.annotation.NameCheckProcessor com/future/annotation/BADLY_NAMED_CODE.java
 *
 * @author future
 */
@SuppressWarnings("all")
public class BADLY_NAMED_CODE {
    enum colors {
        red, blue, green
    }

    static final int _FORTY_TWO = 42;

    public static int NOT_A_CONSTANT = _FORTY_TWO;

    protected void Test() {
        return;
    }

    public void NOTcamelCASEmethodNAME() {
        return;
    }

    /*
     * 测试命令如下
     * javac -processor com.future.annotation.NameCheckProcessor com/future/annotation/BADLY_NAMED_CODE.java
     */
}
