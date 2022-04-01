package com.future.jvm.asm;

import com.future.bean.Person;
import com.future.util.FileUtils;
import com.future.util.ResourceUtil;
import jdk.internal.org.objectweb.asm.*;

import java.io.File;


public class AsmLibraryApi {

    public static void testAddField(VisitorParams params) {
        byte[] bytes = ResourceUtil.loadBytesInClassPath(params.getSrcPath());
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);    // 这种方式不会去计算操作数栈和局部变量表的大小，需要我们手动指定。
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                FieldVisitor fieldVisitor = visitField(params.opcodeAcc, params.name, params.signature, null, null);
                if (fieldVisitor != null)
                    fieldVisitor.visitEnd();
            }
        };
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        byte[] modified = writer.toByteArray();
        String fullPath = ResourceUtil.getClassPath() + File.separator + params.getDstPath();
        FileUtils.writeFile(fullPath, modified);
    }

    public static void testAddField() {
        VisitorParams params = new VisitorParams("com.future.bean.Person");
        params.opcodeAcc = Opcodes.ACC_PUBLIC;
        params.name = "tempField";
        params.signature = "Ljava/lang/String;";
        testAddField(params);
    }

    public static void testAddMethod(VisitorParams params) {
        byte[] bytes = ResourceUtil.loadBytesInClassPath(params.getSrcPath());
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);     // 会自动计算操作数栈和局部变量表的大小，前提是需要调用 visitMaxs 方法来触发计算上述两个值，参数值可以随便指定。
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                MethodVisitor methodVisitor = visitMethod(params.opcodeAcc, params.name, params.signature, null, null);
                if (methodVisitor != null) {
                    methodVisitor.visitEnd();
                }
            }
        };
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
//        reader.accept(visitor, 0);
        byte[] modified = writer.toByteArray();
        String fullPath = ResourceUtil.getClassPath() + File.separator + params.getDstPath();
        FileUtils.writeFile(fullPath, modified);
    }

    public static void testAddMethod() {
        VisitorParams params = new VisitorParams("com.future.bean.Person");
        params.opcodeAcc = Opcodes.ACC_PUBLIC;
        params.name = "tempMethod";
        params.signature = "(ILjava/lang/String;)V";
        testAddMethod(params);
    }

    public static void testModifiedMethod(VisitorParams params) {
        byte[] bytes = ResourceUtil.loadBytesInClassPath(params.getSrcPath());
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);     // 会自动计算操作数栈和局部变量表的大小，前提是需要调用 visitMaxs 方法来触发计算上述两个值，参数值可以随便指定。
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (params.name.equals(name)) {
                    // 删除 foo 方法
                    return null;
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                MethodVisitor methodVisitor = cv.visitMethod(params.opcodeAcc, params.name, params.signature, null, null);
                System.out.println(methodVisitor);
                if (methodVisitor != null) {
                    methodVisitor.visitCode();
                    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1);
                    methodVisitor.visitIntInsn(Opcodes.BIPUSH, 100);
                    methodVisitor.visitInsn(Opcodes.IADD);
                    methodVisitor.visitInsn(Opcodes.IRETURN);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
            }
        };
        reader.accept(visitor, 0);
        byte[] modified = writer.toByteArray();
        String fullPath = ResourceUtil.getClassPath() + File.separator + params.getDstPath();
        FileUtils.writeFile(fullPath, modified);
    }

    public static void testModifiedMethod() {
        // 此处 dstName 需要和 srcName 相同，因为需要保证文件名和类名一致。否则加载不到
        // java.lang.NoClassDefFoundError: com/future/javac/Test (wrong name: com/future/javac/Test1)
        VisitorParams params = new VisitorParams("com.future.javac.ModifiableObject");
        params.opcodeAcc = Opcodes.ACC_PUBLIC;
        params.name = "foo";
        params.signature = "(I)I";
        testModifiedMethod(params);
    }

    public static void main(String[] args) {
//        testAddField();
//        testAddMethod();
        testModifiedMethod();
    }
}
