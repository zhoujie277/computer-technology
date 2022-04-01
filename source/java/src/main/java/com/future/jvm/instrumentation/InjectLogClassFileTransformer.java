package com.future.jvm.instrumentation;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Instrumentation API 功能演示
 *
 * @author future
 */
public class InjectLogClassFileTransformer implements ClassFileTransformer {


    private static class SimpleClassVisitor extends ClassVisitor {

        public SimpleClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (name.equals("<init>") || name.equals("main")) return methodVisitor;
            System.out.println("name:" + name);
            return new SimpleMethodVisitor(methodVisitor, access, name, desc);
        }

    }

    private static class SimpleMethodVisitor extends AdviceAdapter {

        public SimpleMethodVisitor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }


        @Override
        protected void onMethodEnter() {
            this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            this.mv.visitLdcInsn("hello, simple log enter");
            this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            super.onMethodEnter();
        }

        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode);
            this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            this.mv.visitLdcInsn("hello, simple log exit");
            this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.equals("com/future/jvm/instrumentation/InstrumentationMain")) return classfileBuffer;
        System.out.println("transform:" + className + ", loader is " + loader);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new SimpleClassVisitor(writer);
        ClassReader reader = new ClassReader(classfileBuffer);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
}
