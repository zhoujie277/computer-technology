package com.future.jvm.asm;

import com.future.annotation.JGetterAndSetter;

public class TargetObject {
    @JGetterAndSetter
    private int age;

    @JGetterAndSetter
    private String name;

    @JGetterAndSetter
    private long id;
}
