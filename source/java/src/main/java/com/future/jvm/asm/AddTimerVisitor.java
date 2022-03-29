package com.future.jvm.asm;

import com.future.util.FileUtils;
import com.future.util.ResourceUtil;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.io.IOException;

/**
 * 在 TargetTimer 注入方法计时的统计代码。
 * 该注入方法是无状态的, 即 Stateless
 *
 * @author future
 */
public class AddTimerVisitor extends ClassVisitor {

    private String owner;
    private boolean isInterface;

    public AddTimerVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM5, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (!isInterface && methodVisitor != null && !name.equals("<init>")) {
            methodVisitor = new AddTimerMethodAdapter(methodVisitor);
        }
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        if (!isInterface) {
            // 增加成员变量 public static long timer;
            FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "timer", "J", null, null);
            if (fv != null) {
                fv.visitEnd();
            }
        }
        super.visitEnd();
    }

    private class AddTimerMethodAdapter extends MethodVisitor {

        public AddTimerMethodAdapter(MethodVisitor visitor) {
            super(Opcodes.ASM5, visitor);
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            // 在开始之处加上计时语句: timer -= System.currentTimeMillis();
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, "timer", "J");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitInsn(Opcodes.LSUB);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, "timer", "J");
        }

        @Override
        public void visitInsn(int opcode) {
            System.out.println("====visit instruction=====" + opcode);
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
                mv.visitFieldInsn(Opcodes.GETSTATIC, owner, "timer", "J");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
                mv.visitInsn(Opcodes.LADD);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, "timer", "J");
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // 此处是保守起见，因为注入的代码用到了两个 long 型变量，所以需要 4 个槽。即最大为 max(s + 4); 可能会存在一些小小的浪费。
            super.visitMaxs(maxStack + 4, maxLocals);
        }
    }

    public static void main(String[] args) throws IOException {
        String className = "com.future.jvm.asm.TargetTimer";
        byte[] modified = ASMUtil.injectJavap(className, AddTimerVisitor::new);
        String fullPath = ResourceUtil.getAbsolutePath(className);
        System.out.println("fullPath=" + fullPath);
        FileUtils.writeFile(fullPath, modified);
    }
}
