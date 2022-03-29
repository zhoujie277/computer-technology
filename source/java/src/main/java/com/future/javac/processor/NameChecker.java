package com.future.javac.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * 程序名称规范的编译器插件
 * 如果程序命名不合规范，将会输出一个编译器的 WARNING 信息
 */
public class NameChecker {
    private final NameCheckScanner nameCheckScanner;

    NameChecker(ProcessingEnvironment env) {
        nameCheckScanner = new NameCheckScanner(env.getMessager());
    }

    /**
     * 对 Java 程序命名进行检查，根据《Java 语言规范》第三版第 6.8 节的要求，Java 程序命名应当符合下列格式。
     * <p>
     * 类或接口：符合驼峰命名法，首字母大写。
     * 方法：符合驼峰命名法，首字母小写
     * 常量：要求全部大写
     */
    public void checkNames(Element element) {
        nameCheckScanner.scan(element);
    }
}
