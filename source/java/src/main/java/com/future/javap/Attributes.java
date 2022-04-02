package com.future.javap;

import java.util.ArrayList;
import java.util.List;

/**
 * 属性表
 *
 * @author future
 */
public class Attributes {

    private static final Attribute.Factory factory = new Attribute.Factory();

    public static Attributes create(ClassFile file, IReader reader) {
        int attributesCount = reader.readUnsignedShort();
        List<Attribute> attributes = new ArrayList<>(attributesCount);
        for (int i = 0; i < attributesCount; i++) {
            attributes.add(createAttribute(file, reader));
        }
        return new Attributes(attributes);
    }

    private static Attribute createAttribute(ClassFile file, IReader reader) {
        int nameIndex = reader.readUnsignedShort();
        int length = reader.readUnsignedInt();
        byte[] data = reader.readArray(length);
        return factory.createAttribute(file, nameIndex, data);
    }

    private final List<Attribute> attributes;

    public Attributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public int size() {
        return attributes.size();
    }

    public Attribute get(int index) {
        return attributes.get(index);
    }

    @Override
    public String toString() {
        return attributes.toString();
    }
}
