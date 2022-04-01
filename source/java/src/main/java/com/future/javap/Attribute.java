package com.future.javap;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * 属性抽象类
 *
 * @author future
 */
@Slf4j
@SuppressWarnings("unused")
public abstract class Attribute implements IReader {
    public static final String AnnotationDefault = "AnnotationDefault";
    public static final String BootstrapMethods = "BootstrapMethods";
    public static final String CharacterRangeTable = "CharacterRangeTable";
    public static final String Code = "Code";
    public static final String ConstantValue = "ConstantValue";
    public static final String CompilationID = "CompilationID";
    public static final String Deprecated = "Deprecated";
    public static final String EnclosingMethod = "EnclosingMethod";
    public static final String Exceptions = "Exceptions";
    public static final String InnerClasses = "InnerClasses";
    public static final String LineNumberTable = "LineNumberTable";
    public static final String LocalVariableTable = "LocalVariableTable";
    public static final String LocalVariableTypeTable = "LocalVariableTypeTable";
    public static final String MethodParameters = "MethodParameters";
    public static final String RuntimeVisibleAnnotations = "RuntimeVisibleAnnotations";
    public static final String RuntimeInvisibleAnnotations = "RuntimeInvisibleAnnotations";
    public static final String RuntimeVisibleParameterAnnotations = "RuntimeVisibleParameterAnnotations";
    public static final String RuntimeInvisibleParameterAnnotations = "RuntimeInvisibleParameterAnnotations";
    public static final String RuntimeVisibleTypeAnnotations = "RuntimeVisibleTypeAnnotations";
    public static final String RuntimeInvisibleTypeAnnotations = "RuntimeInvisibleTypeAnnotations";
    public static final String Signature = "Signature";
    public static final String SourceDebugExtension = "SourceDebugExtension";
    public static final String SourceFile = "SourceFile";
    public static final String SourceID = "SourceID";
    public static final String StackMap = "StackMap";
    public static final String StackMapTable = "StackMapTable";
    public static final String Synthetic = "Synthetic";


    public static class Factory {
        private Map<String, Class<? extends Attribute>> standardAttributes;

        public Attribute createAttribute(ClassFile file, int name_index, byte[] data) {
            if (standardAttributes == null) {
                init();
            }

            String reasonForDefaultAttr;
            try {
                ConstantPool cp = file.getConstantPool();
                String name = cp.getUTF8Value(name_index);
                Class<? extends Attribute> attrClass = standardAttributes.get(name);
                if (attrClass != null) {
                    try {
                        Class<?>[] constrArgTypes = {ClassFile.class, int.class, byte[].class};
                        Constructor<? extends Attribute> constr = attrClass.getDeclaredConstructor(constrArgTypes);
                        Attribute attribute = constr.newInstance(file, name_index, data);
                        attribute.parse();
                        return attribute;
                    } catch (Throwable t) {
                        reasonForDefaultAttr = t.toString();
                    }
                } else {
                    reasonForDefaultAttr = "unknown attribute: " + name;
                    log.debug(reasonForDefaultAttr);
                }
            } catch (ConstantPoolException e) {
                reasonForDefaultAttr = e.toString();
            }
            return new UnknownAttribute(file, name_index, data, reasonForDefaultAttr);
        }

        protected void init() {
            standardAttributes = new HashMap<>();
            standardAttributes.put(Code, CodeAttribute.class);
            standardAttributes.put(ConstantValue, ConstantValueAttribute.class);
            standardAttributes.put(SourceFile, SourceFileAttribute.class);
            standardAttributes.put(Exceptions, ExceptionsAttribute.class);
            standardAttributes.put(LineNumberTable, LineNumberTableAttribute.class);
            standardAttributes.put(LocalVariableTable, LocalVariableTableAttribute.class);
            standardAttributes.put(StackMapTable, StackMapTableAttribute.class);
        }
    }

    static class UnknownAttribute extends Attribute {

        private final String reason;

        protected UnknownAttribute(ClassFile classFile, int nameIndex, byte[] data, String reason) {
            super(classFile, nameIndex, data);
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "UnknownAttribute {" +
                    "reason = '" + reason + '\'' +
                    '}';
        }

        @Override
        public void parse() {
            throw new UnsupportedOperationException();
        }
    }

    protected final int nameIndex;
    protected final int attributeLength;
    private final byte[] data;
    private int offset;
    protected ClassFile classFile;

    public Attribute(ClassFile classFile, int nameIndex, byte[] data) {
        this.classFile = classFile;
        this.nameIndex = nameIndex;
        this.data = data;
        this.attributeLength = data.length;
        this.offset = 0;
    }

    public abstract void parse();

    @Override
    public int readInt() {
        int value = Javap.byteArrayToIntInBigIndian(data, offset, Javap.U4);
        offset += Javap.U4;
        return value;
    }

    @Override
    public int readUnsignedShort() {
        int value = Javap.byteArrayToIntInBigIndian(data, offset, Javap.U2);
        offset += Javap.U2;
        return value;
    }

    @Override
    public byte[] readArray(int length) {
        byte[] buf = new byte[length];
        System.arraycopy(this.data, offset, buf, 0, buf.length);
        offset += buf.length;
        return buf;
    }

    @Override
    public int readByte() {
        int b = data[offset] & 0xFF;
        offset += Javap.U1;
        return b;
    }

    @Override
    public String readString(int len) {
        String content = new String(this.data, offset, len);
        offset += len;
        return content;
    }

    @Override
    public String toString() {
        return "nameIndex=" + classFile.toString(nameIndex) +
                ", attributeLength=" + attributeLength;
    }
}
