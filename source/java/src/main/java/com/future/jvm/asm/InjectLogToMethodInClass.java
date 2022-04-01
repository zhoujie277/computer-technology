package com.future.jvm.asm;

import com.future.util.FileUtils;
import com.future.util.ResourceUtil;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Function;

/**
 * ASM 注入工具类
 * 给指定的类和方法注入打印日志
 * 演示注入 try/catch 代码块
 *
 * @author future
 */
@Slf4j
public class InjectLogToMethodInClass {

    static class InjectLogAdapter extends AdviceAdapter {
        private final String name;
        //
        Label startLabel = new Label();

        protected InjectLogAdapter(final int api, final MethodVisitor methodVisitor, final int access,
                                   final String name, final String descriptor) {
            super(api, methodVisitor, access, name, descriptor);
            this.name = name;
        }

        @Override
        protected void onMethodEnter() {
            log.debug("name = {}", name);
            this.mv.visitLabel(startLabel);
            // 新增 System.out.println("enter " + name)
            this.mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            this.mv.visitLdcInsn("enter " + name);
            this.mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            log.debug("name = {}, maxStack = {}, maxLocals = {}", name, maxStack, maxLocals);
            Label endLabel = new Label();
            // void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type)
            // start, end 表示异常表开始和结束的位置，handler 表示异常发生后需要跳转到哪里继续执行。type 是异常的类型
            this.mv.visitTryCatchBlock(startLabel, endLabel, endLabel, null);
            this.mv.visitLabel(endLabel);
            injectLog(Opcodes.ATHROW);
            this.mv.visitInsn(Opcodes.ATHROW);
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        protected void onMethodExit(int opcode) {
            log.debug("name = {}, opcode = {}", name, opcode);
            if (opcode != Opcodes.ATHROW)
                injectLog(opcode);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            log.debug("name = {}", name);
        }

        private void injectLog(int opcode) {
            // 新增 System.out.println("enter " + name)
            this.mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            if (opcode == ATHROW) {
                this.mv.visitLdcInsn("err exit " + name);
            } else {
                this.mv.visitLdcInsn("normal exit " + name);
            }
            this.mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

    static class InjectClassVisitor extends ClassVisitor {
        private final VisitorParams params;

        public InjectClassVisitor(VisitorParams params, ClassVisitor visitor) {
            super(Opcodes.ASM5, visitor);
            this.params = params;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            log.debug("{}, {}, {}, {}, {}", access, name, desc, signature, exceptions);
            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (!name.equals(params.name)) {
                return methodVisitor;
            }
            return new InjectLogAdapter(Opcodes.ASM5, methodVisitor, access, name, desc);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    public static byte[] run(VisitorParams params, byte[] classBytes) {
        System.out.println("run....");
        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        System.out.println("run1....");
        InjectClassVisitor visitor = new InjectClassVisitor(params, classWriter);
        System.out.println("run2....");
        classReader.accept(visitor, 0);
        System.out.println("run3....");
        return classWriter.toByteArray();
    }

    public static void main(String[] args) {
        VisitorParams params = new VisitorParams("com.future.jvm.instrumentation.InstrumentationMain", "run");
        byte[] bytes = ResourceUtil.loadBytesInClassPath(params.getSrcPath());
        byte[] modified = run(params, bytes);
        String fullPath = ResourceUtil.getClassPath() + File.separator + params.getDstPath();
        FileUtils.writeFile(fullPath, modified);
    }
}
