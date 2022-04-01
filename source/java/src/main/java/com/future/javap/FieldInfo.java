package com.future.javap;

class FieldInfo {
    int accessFlags;
    int nameIndex;
    int descriptorIndex;
    Attributes attributes;
    private ClassFile classFile;

    public void parseField(ClassFile file, SimpleClassReader reader) {
        this.classFile = file;
        this.accessFlags = reader.readUnsignedShort();
        this.nameIndex = reader.readUnsignedShort();
        this.descriptorIndex = reader.readUnsignedShort();
        this.attributes = Attributes.create(file, reader);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("accessFlags = 0x")
                .append(Integer.toHexString(accessFlags).toUpperCase())
                .append(", nameIndex = ").append(classFile.toString(nameIndex))
                .append(", descriptorIndex = ").append(classFile.toString(descriptorIndex))
                .append(", attributesCount = ").append(attributes.size());
        for (int i = 0; i < attributes.size(); i++) {
            builder.append("\n\t\t").append(attributes.get(i));
        }
        return builder.toString();
    }
}
