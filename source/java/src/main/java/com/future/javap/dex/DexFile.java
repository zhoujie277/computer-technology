package com.future.javap.dex;

import com.future.javap.IReader;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件版式
 * <p>
 * header	header_item	标头
 * <p>
 * string_ids	string_id_item[]	字符串标识符列表。这些是此文件使用的所有字符串的标识符，用于内部命名（例如类型描述符）或用作代码引用的常量对象。此列表必须使用 UTF-16 代码点值按字符串内容进行排序（不采用语言区域敏感方式），且不得包含任何重复条目。
 * <p>
 * type_ids	type_id_item[]	类型标识符列表。这些是此文件引用的所有类型（类、数组或原始类型）的标识符（无论文件中是否已定义）。此列表必须按 string_id 索引进行排序，且不得包含任何重复条目。
 * <p>
 * proto_ids	proto_id_item[]	方法原型标识符列表。这些是此文件引用的所有原型的标识符。此列表必须按返回类型（按 type_id 索引排序）主要顺序进行排序，然后按参数列表（按 type_id 索引排序的各个参数，采用字典排序方法）进行排序。该列表不得包含任何重复条目。
 * <p>
 * field_ids	field_id_item[]	字段标识符列表。这些是此文件引用的所有字段的标识符（无论文件中是否已定义）。此列表必须进行排序，其中定义类型（按 type_id 索引排序）是主要顺序，字段名称（按 string_id 索引排序）是中间顺序，而类型（按 type_id 索引排序）是次要顺序。该列表不得包含任何重复条目。
 * <p>
 * method_ids	method_id_item[]	方法标识符列表。这些是此文件引用的所有方法的标识符（无论文件中是否已定义）。此列表必须进行排序，其中定义类型（按 type_id 索引排序）是主要顺序，方法名称（按 string_id 索引排序）是中间顺序，而方法原型（按 proto_id 索引排序）是次要顺序。该列表不得包含任何重复条目。
 * <p>
 * class_defs	class_def_item[]	类定义列表。这些类必须进行排序，以便所指定类的超类和已实现的接口比引用类更早出现在该列表中。此外，对于在该列表中多次出现的同名类，其定义是无效的。
 * <p>
 * call_site_ids	call_site_id_item[]	调用站点标识符列表。这些是此文件引用的所有调用站点的标识符（无论文件中是否已定义）。此列表必须按 call_site_off 以升序进行排序。
 * <p>
 * method_handles	method_handle_item[]	方法句柄列表。此文件引用的所有方法句柄的列表（无论文件中是否已定义）。此列表未进行排序，而且可能包含将在逻辑上对应于不同方法句柄实例的重复项。
 * <p>
 * data	ubyte[]	数据区，包含上面所列表格的所有支持数据。不同的项有不同的对齐要求；如有必要，则在每个项之前插入填充字节，以实现所需的对齐效果。
 * <p>
 * link_data	ubyte[]	静态链接文件中使用的数据。本文档尚未指定本区段中数据的格式。此区段在未链接文件中为空，而运行时实现可能会在适当的情况下使用这些数据。
 *
 * @author future
 */
public class DexFile {

    private final int bytes;

    private final IReader reader;

    private final DexFileHeader fileHeader;
    private StringIdItem[] stringIds;
    private TypeIdItem[] typeIds;
    private ProtoIdItem[] protoIds;
    private FieldIdItem[] fieldIds;
    private MethodIdItem[] methodIds;
    private ClassDefItem[] classDefs;
    private CallSiteIdItem[] callSiteIds;
    private MethodHandleItem[] methodHandles;
    // 数据区，包含上面所列表格的所有支持数据。
    private byte[] data;
    // 静态链接文件中使用的数据。
    private byte[] linkData;

    private final Map<StringIdItem, StringDataItem> stringDataItemMap = new HashMap<>();
    private final Map<ClassDefItem, ClassDataItem> classDataItemMap = new HashMap<>();
    private final Map<ProtoIdItem, TypeList> protoIdItemTypeListMap = new HashMap<>();
    private final Map<EncodedMethod, CodeItem> methodCodeItemMap = new HashMap<>();

    public DexFile(IReader reader, int size) {
        this.reader = reader;
        this.bytes = size;
        fileHeader = new DexFileHeader(reader);
    }

    public void parseHeader() {
        fileHeader.parseHeader();
        parseStringIds();
        parseTypeIds();
        parseProtoIds();
        parseTypeList();
        parseFieldIds();
        parseMethodIds();
        parseClassDef();
        parseStringDataItems();
        parseClassDataItems();
    }

    public void parseStringIds() {
        reader.seek(fileHeader.getStringIdsOff());
        stringIds = new StringIdItem[fileHeader.getStringIdsSize()];
        for (int i = 0; i < stringIds.length; i++) {
            int stringDataOff = reader.readUnsignedInt();
            stringIds[i] = new StringIdItem(stringDataOff);
        }
    }

    public void parseTypeIds() {
        reader.seek(fileHeader.getTypeIdsOff());
        typeIds = new TypeIdItem[fileHeader.getTypeIdsSize()];
        for (int i = 0; i < typeIds.length; i++) {
            int descriptor_idx = reader.readUnsignedInt();
            typeIds[i] = new TypeIdItem(descriptor_idx);
        }
    }

    public void parseProtoIds() {
        reader.seek(fileHeader.getProtoIdsOff());
        protoIds = new ProtoIdItem[fileHeader.getProtoIdsSize()];
        for (int i = 0; i < protoIds.length; i++) {
            protoIds[i] = new ProtoIdItem(reader.readUnsignedInt(), reader.readUnsignedInt(), reader.readUnsignedInt());
        }
    }

    /**
     * type_list
     * <p>
     * 引用自 class_def_item 和 proto_id_item
     * 出现在数据区段中
     * size	uint	列表的大小（以条目数表示）
     * list	type_item[size]	列表的元素
     */
    public void parseTypeList() {
        for (ProtoIdItem protoId : protoIds) {
            if (protoId.parametersOff == 0) continue;
            reader.seek(protoId.parametersOff);
            int size = reader.readUnsignedInt();
            TypeItem[] typeItems = new TypeItem[size];
            for (int i = 0; i < size; i++) {
                typeItems[i] = new TypeItem(reader.readUnsignedShort());
            }
            TypeList typeList = new TypeList(size, typeItems);
            protoIdItemTypeListMap.put(protoId, typeList);
        }
    }

    public void parseFieldIds() {
        reader.seek(fileHeader.getFieldIdsOff());
        fieldIds = new FieldIdItem[fileHeader.getFieldIdsSize()];
        for (int i = 0; i < fieldIds.length; i++) {
            fieldIds[i] = new FieldIdItem(reader.readUnsignedShort(), reader.readUnsignedShort(), reader.readUnsignedInt());
        }
    }

    public void parseMethodIds() {
        reader.seek(fileHeader.getMethodIdsOff());
        methodIds = new MethodIdItem[fileHeader.getMethodIdsSize()];
        for (int i = 0; i < methodIds.length; i++) {
            methodIds[i] = new MethodIdItem(reader.readUnsignedShort(), reader.readUnsignedShort(), reader.readUnsignedInt());
        }
    }

    public void parseClassDef() {
        reader.seek(fileHeader.getClassDefsOff());
        classDefs = new ClassDefItem[fileHeader.getClassDefsSize()];
        for (int i = 0; i < classDefs.length; i++) {
            classDefs[i] = new ClassDefItem(reader.readUnsignedInt(), reader.readUnsignedInt(),
                    reader.readUnsignedInt(), reader.readUnsignedInt(), reader.readUnsignedInt(),
                    reader.readUnsignedInt(), reader.readUnsignedInt(), reader.readUnsignedInt());
        }
    }

    private void parseStringDataItems() {
        for (StringIdItem stringId : stringIds) {
            reader.seek(stringId.stringDataOff);
            int size = reader.readUnsignedLEB128();
            byte[] bytes = reader.readArray(size);
            stringDataItemMap.put(stringId, new StringDataItem(size, bytes));
        }
    }

    private void parseClassDataItems() {
        for (ClassDefItem classDef : classDefs) {
            reader.seek(classDef.classDataOff);
            int staticFieldsSize = reader.readUnsignedLEB128();
            int instanceFieldsSize = reader.readUnsignedLEB128();
            int directMethodsSize = reader.readUnsignedLEB128();
            int virtualMethodsSize = reader.readUnsignedLEB128();
            EncodedField[] staticFields = new EncodedField[staticFieldsSize];
            for (int i = 0; i < staticFieldsSize; i++) {
                staticFields[i] = new EncodedField(reader.readUnsignedLEB128(), reader.readUnsignedLEB128());
            }
            EncodedField[] instanceFields = new EncodedField[instanceFieldsSize];
            for (int i = 0; i < instanceFieldsSize; i++) {
                instanceFields[i] = new EncodedField(reader.readUnsignedLEB128(), reader.readUnsignedLEB128());
            }
            EncodedMethod[] directMethods = new EncodedMethod[directMethodsSize];
            for (int i = 0; i < directMethodsSize; i++) {
                directMethods[i] = new EncodedMethod(reader.readUnsignedLEB128(), reader.readUnsignedLEB128(), reader.readUnsignedLEB128());
                int mark = reader.mark();
                parseCodeItem(directMethods[i]);
                reader.seek(mark);
            }
            EncodedMethod[] virtualMethods = new EncodedMethod[virtualMethodsSize];
            for (int i = 0; i < virtualMethodsSize; i++) {
                virtualMethods[i] = new EncodedMethod(reader.readUnsignedLEB128(), reader.readUnsignedLEB128(), reader.readUnsignedLEB128());
                int mark = reader.mark();
                parseCodeItem(virtualMethods[i]);
                reader.seek(mark);
            }
            ClassDataItem classDataItem = new ClassDataItem(staticFieldsSize, instanceFieldsSize, directMethodsSize, virtualMethodsSize);
            classDataItem.setStaticFields(staticFields);
            classDataItem.setInstanceFields(instanceFields);
            classDataItem.setDirectMethods(directMethods);
            classDataItem.setVirtualMethods(virtualMethods);
            classDataItemMap.put(classDef, classDataItem);
        }
    }

    /**
     * 从文件开头到此方法的代码结构的偏移量；
     * 如果此方法是 abstract 或 native，则该值为 0。偏移量应该是到 data 区段中某个位置的偏移量。数据格式由下文的“code_item”指定。
     */
    private void parseCodeItem(EncodedMethod method) {
        int codeOff = method.codeOff;
        if (codeOff == 0) return;
        reader.seek(codeOff);
        // 此代码使用的寄存器数量
        int registers = reader.readUnsignedShort();
        // 此代码所用方法的传入参数的字数
        int ins_size = reader.readUnsignedShort();
        // 此代码进行方法调用所需的传出参数空间的字数
        int outs_size = reader.readUnsignedShort();
        // 此实例的 try_item 数量。
        int tries_size = reader.readUnsignedShort();
        // 从文件开头到此代码的调试信息（行号 + 局部变量信息）序列的偏移量
        int debug_info_off = reader.readUnsignedInt();
        // 指令列表的大小
        int insns_size = reader.readUnsignedInt();
        int[] insns = new int[insns_size];
        for (int i = 0; i < insns_size; i++) {
            int unsignedShort = reader.readUnsignedShort();
            insns[i] = unsignedShort;
        }
        CodeItem codeItem = new CodeItem(registers, ins_size, outs_size, tries_size, debug_info_off, insns_size);
        codeItem.setInsns(insns);
        methodCodeItemMap.put(method, codeItem);
    }

    public String typeDescriptor(int typeIdx) {
        return stringDataItemMap.get(stringIds[typeIds[typeIdx].descriptorIdx]).value();
    }

    public String string(int stringIdx) {
        return stringDataItemMap.get(stringIds[stringIdx]).value();
    }

    public StringBuilder protoString(int protoIdx) {
        ProtoIdItem protoId = protoIds[protoIdx];
        TypeList typeList = protoIdItemTypeListMap.get(protoId);
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        if (typeList != null) {
            for (int j = 0; j < typeList.size; j++) {
                builder.append(typeDescriptor(typeList.typeItems[j].typeIdx));
            }
        }
        builder.append(")");
        builder.append(typeDescriptor(protoId.returnTypeIdx));
        return builder;
    }

    public StringBuilder fieldString(int fieldIdx, boolean appendClass) {
        FieldIdItem fieldId = fieldIds[fieldIdx];
        StringBuilder builder = new StringBuilder();
        builder.append(string(fieldId.nameIdx)).append("\t").append(typeDescriptor(fieldId.typeIdx));
        if (appendClass) {
            builder.append("\t\t # ").append(typeDescriptor(fieldId.classIdx));
        }
        return builder;
    }

    public StringBuilder methodString(int methodIdx, boolean appendClass) {
        MethodIdItem methodId = methodIds[methodIdx];
        StringBuilder builder = new StringBuilder();
        builder.append(string(methodId.nameIdx)).append(protoString(methodId.protoIdx));
        if (appendClass) {
            builder.append("\t\t # ").append(typeDescriptor(methodId.classIdx));
        }
        return builder;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DexFile ").append("bytes = ").append(bytes);
        // headerItem
        builder.append("\nHeaderItem = {\n\t").append(fileHeader).append("}");
        // stringDataItems
        builder.append("\nStringDataItems: [");
        for (int i = 0; i < stringIds.length; i++) {
            StringIdItem stringId = stringIds[i];
            builder.append("\n\t#").append(i).append("\t").append(stringId).append("\t").append(stringDataItemMap.get(stringId));
        }
        builder.append("\n]");
        // ProtoIds
        protoIdsToString(builder);
        // TypesIds
        typeIdsToString(builder);
        // FieldIds
        fieldsIdToString(builder);
        // MethodIds
        methodsIdToString(builder);
        // classDefs
        classDefToString(builder);
        return builder.toString();
    }

    private void protoIdsToString(StringBuilder builder) {
        builder.append("\nProtoIds [");
        for (int i = 0; i < protoIds.length; i++) {
            builder.append("\n\tshortyDescriptor: ").append(string(protoIds[i].shortyIdx));
            builder.append(" \t signature: ");
            builder.append(protoString(i));
        }
        builder.append("\n]");
    }

    private void typeIdsToString(StringBuilder builder) {
        builder.append("\nTypeIds [");
        for (int i = 0; i < typeIds.length; i++) {
            int descriptorIdx = typeIds[i].descriptorIdx;
            builder.append("\n\t#").append(i).append("\tdescriptorIdx(#").append(descriptorIdx).append(") \t").append(typeDescriptor(i));
        }
        builder.append("\n]");
    }

    private void fieldsIdToString(StringBuilder builder) {
        builder.append("\nFields [");
        for (int i = 0; i < fieldIds.length; i++) {
            builder.append("\n\t#").append(i);
            builder.append("\n\t\t").append("classIdx(#").append(fieldIds[i].classIdx).append(") \t").append(typeDescriptor(fieldIds[i].classIdx));
            builder.append("\n\t\t").append("typeIdx(#").append(fieldIds[i].typeIdx).append(") \t").append(typeDescriptor(fieldIds[i].typeIdx));
            builder.append("\n\t\t").append("nameIdx(#").append(fieldIds[i].nameIdx).append(") \t").append(string(fieldIds[i].nameIdx));
        }
        builder.append("\n]");
    }

    private void methodsIdToString(StringBuilder builder) {
        builder.append("\nMethods [");
        for (int i = 0; i < methodIds.length; i++) {
            builder.append("\n\t#").append(i);
            builder.append("\n\t\t").append("classIdx(#").append(methodIds[i].classIdx).append(") \t").append(typeDescriptor(methodIds[i].classIdx));
            builder.append("\n\t\t").append("protoIdx(#").append(methodIds[i].protoIdx).append(") \t").append(protoString(methodIds[i].protoIdx));
            builder.append("\n\t\t").append("nameIdx(#").append(methodIds[i].nameIdx).append(") \t").append(string(methodIds[i].nameIdx));
        }
        builder.append("\n]");
    }

    private void encodedMethodToString(EncodedMethod method, int methodIdx, StringBuilder builder) {
        builder.append("\n\t\t name: ").append(methodString(methodIdx, false));
        builder.append("\n\t\t\t access: 0x").append(Integer.toHexString(method.accessFlags));
        builder.append("\n\t\t\t code:");
        CodeItem codeItem = methodCodeItemMap.get(method);
        for (int k = 0; k < codeItem.insnsSize; k++) {
            int unsignedShort = codeItem.insns[k];
            // 此处是读取短整型，然后取低 8 位，就是字节码。
            DexOpCode opCode = DexOpCode.getOpcodeFromShort(unsignedShort);
            for (int m = 1; m < opCode.format.shortCount; m++, k++) {
                int params = reader.readUnsignedShort();
            }
            builder.append("\n\t\t\t\t").append(opCode.toString().toLowerCase());
        }
    }

    private void classDefToString(StringBuilder builder) {
        builder.append("\nClassDefs [");
        for (int i = 0; i < classDefs.length; i++) {
            ClassDefItem classDef = classDefs[i];
            builder.append("\n").append("  Class #").append(i);
            builder.append("\n\tClass descriptor: \t").append(typeDescriptor(classDef.classIdx));
            builder.append("\n\taccessFlag: \t0x").append(Integer.toHexString(classDef.accessFlags));
            builder.append("\n\tSuperClass: \t").append(typeDescriptor(classDef.superclassIdx));
            builder.append("\n\tsource_file_idx: \t").append(string(classDef.sourceFileIdx));
            ClassDataItem classDataItem = classDataItemMap.get(classDef);
            builder.append("\n\tstatic fields size:\t").append(classDataItem.staticFieldsSize);
            for (int j = 0, fieldIdx = 0; j < classDataItem.staticFieldsSize; j++) {
                fieldIdx += classDataItem.staticFields[j].fieldIdxDiff;
                builder.append("\n\t\t").append(fieldString(fieldIdx, false));
            }

            builder.append("\n\tinstance fields size:\t").append(classDataItem.instanceFieldsSize);
            for (int j = 0, fieldIdx = 0; j < classDataItem.instanceFieldsSize; j++) {
                fieldIdx += classDataItem.instanceFields[j].fieldIdxDiff;
                builder.append("\n\t\t name: ").append(fieldString(fieldIdx, false));
            }

            builder.append("\n\tdirect methods size:\t").append(classDataItem.directMethodsSize);
            for (int j = 0, methodIdx = 0; j < classDataItem.directMethodsSize; j++) {
                EncodedMethod method = classDataItem.directMethods[j];
                methodIdx += method.methodIdxDiff;
                encodedMethodToString(method, methodIdx, builder);
            }

            builder.append("\n\tvirtual methods size:\t").append(classDataItem.virtualMethodsSize);
            for (int j = 0, methodIdx = 0; j < classDataItem.virtualMethodsSize; j++) {
                EncodedMethod method = classDataItem.virtualMethods[j];
                methodIdx += method.methodIdxDiff;
                encodedMethodToString(method, methodIdx, builder);
            }
        }
        builder.append("\n]");
    }
}
