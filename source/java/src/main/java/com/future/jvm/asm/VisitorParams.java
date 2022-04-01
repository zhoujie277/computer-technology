package com.future.jvm.asm;

public class VisitorParams {
    String srcName;
    String dstName;
    String name;
    String signature;
    int opcodeAcc;

    public VisitorParams(String className) {
        this.srcName = className.replace(".", "/") + ".class";
        this.dstName = srcName;
    }

    public VisitorParams(String className, String methodName) {
        this(className);
        this.name = methodName;
    }

    public String getSrcPath() {
        return srcName;
    }

    public String getDstPath() {
        return dstName;
    }
}
