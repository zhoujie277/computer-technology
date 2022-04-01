package com.future.javap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 类结构实体类
 *
 * @author future
 */
@Getter
@NoArgsConstructor
public class ClassFile {

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SUPER = 0x0020;
    public static final int ACC_INTERFACE = 0x0200;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_SYNTHETIC = 0x1000;
    public static final int ACC_ANNOTATION = 0x2000;
    public static final int ACC_ENUM = 0x4000;
    public static final int ACC_MODULE = 0x8000;

    @Setter
    private int bytes;

    private int minorVersion;

    private int majorVersion;

    private final ConstantPool constantPool = new ConstantPool();

    private int accessFlag;

    private int thisClass;

    private int superClass;

    private final List<Integer> interfaces = new ArrayList<>();

    private final List<FieldInfo> fieldInfos = new ArrayList<>();

    private final List<MethodInfo> methodInfos = new ArrayList<>();

    private Attributes attributeTable = new Attributes(new ArrayList<>());

    protected SimpleClassReader reader;

    public ClassFile(SimpleClassReader reader, int bytes) {
        this.reader = reader;
        this.bytes = bytes;
    }

    public void parseVersion() {
        this.minorVersion = reader.readUnsignedShort();
        this.majorVersion = reader.readUnsignedShort();
    }

    public void parseAccessFlag() {
        this.accessFlag = reader.readUnsignedShort();
    }

    public void parseThisClass() {
        this.thisClass = reader.readUnsignedShort();
    }

    public void parseSuperClass() {
        this.superClass = reader.readUnsignedShort();
    }

    public void parseInterfaces() {
        int interfacesCount = reader.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            this.interfaces.add(reader.readUnsignedShort());
        }
    }

    public void parseFields() {
        int fieldsCount = reader.readUnsignedShort();
        for (int i = 0; i < fieldsCount; i++) {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.parseField(this, reader);
            this.fieldInfos.add(fieldInfo);
        }
    }

    public void parseMethods() {
        int methodCount = reader.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.parseMethod(this, reader);
            this.methodInfos.add(methodInfo);
        }
    }

    public void parseAttributes() {
        this.attributeTable = Attributes.create(this, reader);
    }

    public void parseConstantPool() {
        constantPool.parseConstantPool(reader);
    }

    public String toString(int nameIndex) {
        return constantPool.toString(nameIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("bytes=").append(bytes);
        builder.append(", minorVersion=").append(minorVersion);
        builder.append(", majorVersion=").append(majorVersion).append("\n");
        builder.append(constantPool);
        builder.append("accessFlag= 0x").append(Integer.toHexString(accessFlag).toUpperCase()).append("\n");
        builder.append("thisClass= #").append(thisClass).append("\n");
        builder.append("superClass= #").append(superClass).append("\n");
        builder.append("interfacesCount=").append(interfaces.size());
        builder.append(", fieldsCount=").append(fieldInfos.size());
        builder.append(", methodsCount=").append(methodInfos.size());
        builder.append(", attributesCount=").append(attributeTable.size()).append("\n");
        // interfaces
        builder.append("interfaces: [");
        for (Integer integer : interfaces) {
            builder.append("#").append(integer).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length()).append("]\n");
        // fields
        builder.append("fields: [").append("\n");
        for (FieldInfo fieldInfo : fieldInfos) {
            builder.append("\t").append(fieldInfo).append("\n");
        }
        builder.append("]\n");
        // methods
        builder.append("methods: [").append("\n");
        for (MethodInfo methodInfo : methodInfos) {
            builder.append("\t").append(methodInfo).append("\n");
        }
        builder.append("]\n");
        // attributes
        builder.append("attributes: [").append("\n");
        builder.append("\t").append(attributeTable);
        builder.append("\n]");
        return builder.toString();
    }
}
