package com.future.javap;

import java.io.IOException;
import java.io.InputStream;

/**
 * 类文件读取器
 *
 * @author future
 */
public class SimpleClassReader implements IReader {

    enum State {
        magic,
        version,
        constantPool,
        accessFlag,
        thisClass,
        superClass,
        interfaces,
        fields,
        methods,
        attributes,
        finalState
    }

    private State currentState = State.magic;

    private int offset;
    private byte[] buffer;

    public void startupStatusMachine(String className) {
        String filepath = className.replace(".", "/") + ".class";
        try (InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(filepath)) {
            if (inputStream == null) throw new RuntimeException("class file not found :" + className);
            buffer = new byte[inputStream.available()];
            // 简单起见，全部读进来
            int bytes = inputStream.read(buffer);
            ClassFile classFile = new ClassFile(this, bytes);
            offset = 0;
            while (currentState != State.finalState) {
                switch (currentState) {
                    case magic:
                        boolean isJava = parseMagic();
                        if (!isJava) throw new ClassFormatError("魔数解析错误");
                        currentState = State.version;
                        break;
                    case version:
                        classFile.parseVersion();
                        currentState = State.constantPool;
                        break;
                    case constantPool:
                        classFile.parseConstantPool();
                        currentState = State.accessFlag;
                        break;
                    case accessFlag:
                        classFile.parseAccessFlag();
                        currentState = State.thisClass;
                        break;
                    case thisClass:
                        classFile.parseThisClass();
                        currentState = State.superClass;
                        break;
                    case superClass:
                        classFile.parseSuperClass();
                        currentState = State.interfaces;
                        break;
                    case interfaces:
                        classFile.parseInterfaces();
                        currentState = State.fields;
                        break;
                    case fields:
                        classFile.parseFields();
                        currentState = State.methods;
                        break;
                    case methods:
                        classFile.parseMethods();
                        currentState = State.attributes;
                        break;
                    case attributes:
                        classFile.parseAttributes();
                        currentState = State.finalState;
                        break;
                    default:
                        break;
                }
            }
            System.out.println(classFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int readUnsignedShort() {
        // Java Class 字节码采用大端字节序
        int result = Javap.byteArrayToIntInBigIndian(buffer, offset, Javap.U2);
        offset += Javap.U2;
        return result;
    }

    @Override
    public int readUnsignedInt() {
        // Java Class 字节码采用大端字节序
        int result = Javap.byteArrayToIntInBigIndian(buffer, offset, Javap.U4);
        offset += Javap.U4;
        return result;
    }

    @Override
    public int readUnsignedByte() {
        int b = buffer[offset] & 0xFF;
        offset += Javap.U1;
        return b;
    }

    @Override
    public byte[] readArray(int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, offset, data, 0, data.length);
        offset += data.length;
        return data;
    }

    @Override
    public String readString(int len) {
        String content = new String(buffer, offset, len);
        offset += len;
        return content;
    }

    public boolean parseMagic() {
        if (buffer.length < 4) {
            return false;
        }
        if (readUnsignedByte() != 0xCA) return false;
        if (readUnsignedByte() != 0xFE) return false;
        if (readUnsignedByte() != 0xBA) return false;
        return readUnsignedByte() == 0xBE;
    }

    public static void main(String[] args) {
        SimpleClassReader reader = new SimpleClassReader();
        reader.startupStatusMachine("com.future.javap.SimpleBean");
    }
}
