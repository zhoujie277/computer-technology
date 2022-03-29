package com.future.jvm.asm;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.util.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;

public class ASMUtil {

    /**
     * 以 javap 格式打印类结构信息
     */
    public static byte[] injectJavap(String clazz, Function<ClassVisitor, ClassVisitor> visitor) throws IOException {
        return inject(clazz, visitor, new Textifier());
    }

    /**
     * 以 asm 格式打印类结构信息
     */
    public static byte[] inject(String clazz, Function<ClassVisitor, ClassVisitor> visitor) throws IOException {
        return inject(clazz, visitor, new ASMifier());
    }

    public static byte[] inject(String clazz, Function<ClassVisitor, ClassVisitor> visitor, Printer printer) throws IOException {
        ClassReader reader = new ClassReader(clazz);
        ClassWriter writer = new ClassWriter(0);
        PrintWriter pw = new PrintWriter(System.out);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(writer, printer, pw);
        CheckClassAdapter adapter = new CheckClassAdapter(traceClassVisitor);
        ClassVisitor apply = visitor.apply(adapter);
        reader.accept(apply, 0);
        return writer.toByteArray();
    }

    public static void write(byte[] bytes) {

    }
}
