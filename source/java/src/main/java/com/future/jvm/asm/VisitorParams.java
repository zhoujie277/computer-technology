package com.future.jvm.asm;

import java.io.File;

class VisitorParams {
    String basePath;
    String srcName;
    String dstName;
    String name;
    String signature;
    int opcodeAcc;

    public VisitorParams(String basePath, String srcName, String dstName) {
        this.basePath = basePath;
        this.srcName = srcName;
        this.dstName = dstName;
    }

    public String getSrcPath() {
        return basePath + File.separator + srcName;
    }

    public String getDstPath() {
        return basePath + File.separator + dstName;
    }
}
