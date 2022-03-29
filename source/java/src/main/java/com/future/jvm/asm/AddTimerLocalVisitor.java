package com.future.jvm.asm;

import com.future.util.FileUtils;
import com.future.util.ResourceUtil;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AnalyzerAdapter;
import jdk.internal.org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;

/**
 * 在 TargetTimer 增加局部变量的访问
 *
 * @author future
 */
public class AddTimerLocalVisitor extends ClassVisitor {

    private String owner;
    private boolean isInterface;

    public AddTimerLocalVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM5, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        isInterface = (version & Opcodes.ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (!isInterface && methodVisitor != null && !name.equals("<init>")) {
            /*
             * 为了使用这个适配器，你必须将一个 LocalVariablesSorter 链到一个 AnalyzerAdapter 上，
             * 这个适配器本身也被链到你的适配器上：第一个适配器将对局部变量进行排序并相应地更新帧，
             * 分析器适配器将计算中间框架，并考虑到在前一个适配器中所做的重新编号，
             * 而你的适配器将可以访问这些重新编号的中间帧。这个链条可以在 visitMethod 中构建如下。
             */
            AddTimerLocalAdapter at = new AddTimerLocalAdapter(methodVisitor);
            at.aa = new AnalyzerAdapter(owner, access, name, desc, at);
            at.lvs = new LocalVariablesSorter(access, desc, at.aa);
            return at.lvs;
        }
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        if (!isInterface) {
            // final int access, final String name, final String desc, final String signature, final Object value
            FieldVisitor fieldVisitor = this.cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "timer", "J", null, null);
            if (fieldVisitor != null) {
                fieldVisitor.visitEnd();
            }
        }
        super.visitEnd();
    }

    private class AddTimerLocalAdapter extends MethodVisitor {

        public LocalVariablesSorter lvs;
        public AnalyzerAdapter aa;
        private int localTimeSlot;
        private int maxStack;

        public AddTimerLocalAdapter(MethodVisitor visitor) {
            super(Opcodes.ASM5, visitor);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            localTimeSlot = lvs.newLocal(Type.LONG_TYPE);
            // final int opcode, final String owner, final String name, final String desc, final boolean itf
            this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            this.mv.visitVarInsn(Opcodes.LSTORE, localTimeSlot);
            maxStack = 4;
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
                this.mv.visitVarInsn(Opcodes.LLOAD, localTimeSlot);
                this.mv.visitInsn(Opcodes.LSUB);
                // final int opcode, final String owner, final String name, final String desc
                this.mv.visitFieldInsn(Opcodes.GETSTATIC, owner, "timer", "J");
                this.mv.visitInsn(Opcodes.LADD);
                this.mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, "timer", "J");
                maxStack = Math.max(maxStack, this.aa.stack.size() + 4);
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(Math.max(maxStack, this.maxStack), maxLocals);
        }
    }

    public static void main(String[] args) throws IOException {
        String className = "com.future.jvm.asm.TargetTimer";
        byte[] modified = ASMUtil.injectJavap(className, AddTimerLocalVisitor::new);
        String fullPath = ResourceUtil.getAbsolutePath(className);
        System.out.println("fullPath=" + fullPath);
        FileUtils.writeFile(fullPath, modified);
    }
}
