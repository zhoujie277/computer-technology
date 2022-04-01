package com.future.jvm.asm;

import com.future.annotation.JGetterAndSetter;
import com.future.util.FileUtils;
import com.future.util.ResourceUtil;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AnalyzerAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 根据注解生成 getter、setter 方法
 *
 * @author future
 */
public class GenerateGetterAndSetter extends ClassVisitor {

    private final List<FieldInfo> annotationFields = new ArrayList<>();

    private String owner;

    public GenerateGetterAndSetter(ClassVisitor visitor) {
        super(Opcodes.ASM5, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        System.out.println("owner = " + owner);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        System.out.println("visitField: " + access + ", " + name + ", " + desc + ", " + signature + ", " + value);
        Type type = Type.getType(desc);
        FieldVisitor fieldVisitor = super.visitField(access, name, desc, signature, value);
        return new FieldAdapter(new FieldInfo(name, type), fieldVisitor);
    }

    @Override
    public void visitEnd() {
        for (FieldInfo field : annotationFields) {
            System.out.println(field);
            generateGetter(field);
            generateSetter(field);
        }
        super.visitEnd();
    }

    private void generateGetter(FieldInfo fieldInfo) {
        // int access, String name, String desc, String signature, String[] exceptions
        String methodName = methodName("get", fieldInfo.name);
        String desc = "()" + fieldInfo.type.getDescriptor();
        MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_PUBLIC, methodName, desc, null, null);
        AnalyzerAdapter aa = new AnalyzerAdapter(owner, Opcodes.ACC_PUBLIC, methodName, desc, mv);
        visitGetMethod(aa, fieldInfo);
    }

    private void visitGetMethod(MethodVisitor mv, FieldInfo fieldInfo) {
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // int opcode, String owner, String name, String desc
        mv.visitFieldInsn(Opcodes.GETFIELD, owner, fieldInfo.name, fieldInfo.type.getDescriptor());
        int opcode = fieldInfo.type.getOpcode(Opcodes.IRETURN);
        mv.visitInsn(opcode);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateSetter(FieldInfo fieldInfo) {
        // int access, String name, String desc, String signature, String[] exceptions
        String methodName = methodName("set", fieldInfo.name);
        String desc = "(" + fieldInfo.type.getDescriptor() + ")V";
        MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_PUBLIC, methodName, desc, null, null);
        AnalyzerAdapter aa = new AnalyzerAdapter(owner, Opcodes.ACC_PUBLIC, methodName, desc, mv);
        visitSetMethod(aa, fieldInfo);
    }

    private void visitSetMethod(MethodVisitor mv, FieldInfo fieldInfo) {
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(fieldInfo.type.getOpcode(Opcodes.ILOAD), 1);
        // int opcode, String owner, String name, String desc
        mv.visitFieldInsn(Opcodes.PUTFIELD, owner, fieldInfo.name, fieldInfo.type.getDescriptor());
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static String methodName(String prefix, String fieldName) {
        return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    static class FieldInfo {
        String name;
        Type type;

        public FieldInfo(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "FieldInfo{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    private class FieldAdapter extends FieldVisitor {

        private final FieldInfo info;

        public FieldAdapter(FieldInfo name, FieldVisitor visitor) {
            super(Opcodes.ASM5, visitor);
            this.info = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (Type.getType(JGetterAndSetter.class).getDescriptor().equals(desc)) {
                annotationFields.add(info);
                return null;
            }
            return super.visitAnnotation(desc, visible);
        }
    }

    public static void main(String[] args) throws IOException {
        String className = "com.future.jvm.asm.TargetObject";
        byte[] modified = ASMUtil.injectJavap(className, GenerateGetterAndSetter::new);
        String fullPath = ResourceUtil.getAbsolutePath(className);
        System.out.println("fullPath=" + fullPath);
        FileUtils.writeFile(fullPath, modified);
    }
}
