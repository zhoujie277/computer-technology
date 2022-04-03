package com.future.javap.dex;

import com.future.util.PrintUtils;
import lombok.ToString;

import java.util.Arrays;

public class DexStruct {
}


// 出现在 string_ids 区段中
class StringIdItem {
    // string_data_off uint 从文件开头到此项的字符串数据的偏移量。该偏移量应该是到 data 区段中某个位置的偏移量，且其中的数据应采用下文中“string_data_item”指定的格式。 没有偏移量对齐要求。
    int stringDataOff;

    public StringIdItem(int stringDataOff) {
        this.stringDataOff = stringDataOff;
    }

    @Override
    public String toString() {
        return "StringIdItem (0x" + PrintUtils.toHexString(stringDataOff) + ')';
    }
}

// 出现在数据区段中
class StringDataItem {
    // utf16_size uleb128	此字符串的大小；以 UTF-16 代码单元（在许多系统中为“字符串长度”）为单位。也就是说，这是该字符串的解码长度（编码长度隐含在 0 字节的位置）。
    // data	ubyte[] 一系列 MUTF-8 代码单元（又称八位字节），后跟一个值为 0 的字节。请参阅上文中的“MUTF-8（修改后的 UTF-8）编码”，了解有关该数据格式的详情和讨论。

    public int size;
    public byte[] data;

    public StringDataItem(int size, byte[] data) {
        this.size = size;
        this.data = data;
    }

    public String value() {
        return new String(data);
    }

    @Override
    public String toString() {
        return "StringDataItem {" +
                "size = " + size +
                ", data = " + value() +
                '}';
    }
}

// 出现在 type_ids 区段中
class TypeIdItem {
    // descriptor_idx uint 此类描述符字符串的 string_ids 列表中的索引。该字符串必须符合上文定义的 TypeDescriptor 的语法。
    int descriptorIdx;

    public TypeIdItem(int descriptorIdx) {
        this.descriptorIdx = descriptorIdx;
    }

    @Override
    public String toString() {
        return "TypeIdItem {" + descriptorIdx +
                '}';
    }
}

// 出现在 proto_ids 区段中
class ProtoIdItem {
    // shorty_idx	uint	此原型的简短式描述符字符串的 string_ids 列表中的索引。该字符串必须符合上文定义的 ShortyDescriptor 的语法，而且必须与该项的返回类型和参数相对应。
    // return_type_idx	uint	此原型的返回类型的 type_ids 列表中的索引
    // parameters_off	uint	从文件开头到此原型的参数类型列表的偏移量；如果此原型没有参数，则该值为 0。该偏移量（如果为非零值）应该位于 data 区段，且其中的数据应采用下文中“"type_list"”指定的格式。此外，不得对列表中的类型 void 进行任何引用。
    int shortyIdx;
    int returnTypeIdx;
    int parametersOff;

    public ProtoIdItem(int shortyIdx, int returnTypeIdx, int parametersOff) {
        this.shortyIdx = shortyIdx;
        this.returnTypeIdx = returnTypeIdx;
        this.parametersOff = parametersOff;
    }
}

class TypeList {
    // size	uint	列表的大小（以条目数表示）
    // list	type_item[size]	列表的元素
    int size;
    TypeItem[] typeItems;

    public TypeList(int size, TypeItem[] typeItems) {
        this.size = size;
        this.typeItems = typeItems;
    }
}

class TypeItem {
    // type_idx	ushort	type_ids 列表中的索引
    int typeIdx;

    public TypeItem(int typeIdx) {
        this.typeIdx = typeIdx;
    }
}

// 出现在 field_ids 区段中
class FieldIdItem {
    // class_idx	ushort	此字段的定义符的 type_ids 列表中的索引。此项必须是“类”类型，而不能是“数组”或“基元”类型。
    // type_idx	ushort	此字段的类型的 type_ids 列表中的索引
    // name_idx	uint	此字段的名称的 string_ids 列表中的索引。该字符串必须符合上文定义的 MemberName 的语法。

    int classIdx;
    int typeIdx;
    int nameIdx;

    public FieldIdItem(int classIdx, int typeIdx, int nameIdx) {
        this.classIdx = classIdx;
        this.typeIdx = typeIdx;
        this.nameIdx = nameIdx;
    }
}

// 出现在 method_ids 区段中
class MethodIdItem {
    // class_idx	ushort	此方法的定义符的 type_ids 列表中的索引。此项必须是“类”或“数组”类型，而不能是“基元”类型。
    // proto_idx	ushort	此方法的原型的 proto_ids 列表中的索引
    // name_idx	uint	此方法的名称的 string_ids 列表中的索引。该字符串必须符合上文定义的 MemberName 的语法。

    public int classIdx;
    public int protoIdx;
    public int nameIdx;

    public MethodIdItem(int classIdx, int protoIdx, int nameIdx) {
        this.classIdx = classIdx;
        this.protoIdx = protoIdx;
        this.nameIdx = nameIdx;
    }
}

// 出现在 class_defs 区段中
class ClassDefItem {
    // class_idx	uint	此类的 type_ids 列表中的索引。此项必须是“类”类型，而不能是“数组”或“基元”类型。
    // access_flags	uint	类的访问标记（public、final 等）。如需了解详情，请参阅“access_flags 定义”。
    // superclass_idx	uint	父类的 type_ids 列表中的索引。如果此类没有父类（即它是根类，例如 Object），则该值为常量值 NO_INDEX。如果此类存在父类，则此项必须是“类”类型，而不能是“数组”或“基元”类型。
    // interfaces_off	uint	从文件开头到接口列表的偏移量；如果没有接口，则该值为 0。该偏移量（如果为非零值）应该位于 data 区段，且其中的数据应采用下文中“type_list”指定的格式。该列表的每个元素都必须是“类”类型（而不能是“数组”或“基元”类型），并且不得包含任何重复项。
    // source_file_idx	uint	文件（包含这个类（至少大部分）的原始来源）名称的 string_ids 列表中的索引；或者该值为特殊值 NO_INDEX，以表示缺少这种信息。任何指定方法的 debug_info_item 都可以替换此源文件，但预期情况是大多数类只来自一个源文件。
    // annotations_off	uint	从文件开头到此类的注释结构的偏移量；如果此类没有注释，则该值为 0。此偏移量（如果为非零值）应该位于 data 区段，且其中的数据应采用下文中“annotations_directory_item”指定的格式，同时所有项将此类作为定义符进行引用。
    // class_data_off	uint	从文件开头到此项的关联类数据的偏移量；如果此类没有类数据，则该值为 0（这种情况有可能出现，例如，如果此类是标记接口）。该偏移量（如果为非零值）应该位于 data 区段，且其中的数据应采用下文中“class_data_item”指定的格式，同时所有项将此类作为定义符进行引用。
    // static_values_off	uint	从文件开头到 static 字段初始值列表的偏移量；如果没有该列表（并且所有 static 字段都将使用 0 或 null 进行初始化），则该值为 0。此偏移量应位于 data 区段，且其中的数据应采用下文中“encoded_array_item”指定的格式。该数组的大小不得超出此类所声明的 static 字段的数量，且 static 字段所对应的元素应采用相对应的 field_list 中所声明的相同顺序。每个数组元素的类型均必须与其相应字段的声明类型相匹配。 如果该数组中的元素比 static 字段中的少，则剩余字段将使用适当类型的 0 或null 进行初始化。

    public int classIdx;
    public int accessFlags;
    public int superclassIdx;
    public int interfacesOff;
    public int sourceFileIdx;
    public int annotationsOff;
    public int classDataOff;
    public int staticValuesOff;

    public ClassDefItem(int classIdx, int accessFlags, int superclassIdx, int interfacesOff, int sourceFileIdx, int annotationsOff, int classDataOff, int staticValuesOff) {
        this.classIdx = classIdx;
        this.accessFlags = accessFlags;
        this.superclassIdx = superclassIdx;
        this.interfacesOff = interfacesOff;
        this.sourceFileIdx = sourceFileIdx;
        this.annotationsOff = annotationsOff;
        this.classDataOff = classDataOff;
        this.staticValuesOff = staticValuesOff;
    }
}

// 出现在 call_site_ids 区段中
class CallSiteIdItem {
    // call_site_off	uint	从文件开头到调用点定义的偏移量。该偏移量应该位于数据区段，且其中的数据应采用下文中“call_site_item”指定的格式。
}

class callSiteItem {

}

// 出现在 method_handles 区段中
class MethodHandleItem {
    // method_handle_type	ushort	方法句柄的类型；见下表
    // unused	ushort	（未使用）
    // field_or_method_id	ushort	字段或方法 ID 取决于方法句柄类型是访问器还是方法调用器
    // unused	ushort	（未使用）

    public int methodHandleType;
    public int unused1;
    public int fieldOrMethodId;
    public int unused2;

    public MethodHandleItem(int methodHandleType, int unused1, int fieldOrMethodId, int unused2) {
        this.methodHandleType = methodHandleType;
        this.unused1 = unused1;
        this.fieldOrMethodId = fieldOrMethodId;
        this.unused2 = unused2;
    }
}

class ClassDataItem {
    // static_fields_size	uleb128	此项中定义的静态字段的数量
    // instance_fields_size	uleb128	此项中定义的实例字段的数量
    // direct_methods_size	uleb128	此项中定义的直接方法的数量
    // virtual_methods_size	uleb128	此项中定义的虚拟方法的数量
    // static_fields	encoded_field[static_fields_size]	定义的静态字段；以一系列编码元素的形式表示。这些字段必须按 field_idx 以升序进行排序。
    // instance_fields	encoded_field[instance_fields_size]	定义的实例字段；以一系列编码元素的形式表示。这些字段必须按 field_idx 以升序进行排序。
    // direct_methods	encoded_method[direct_methods_size]	定义的直接（static、private 或构造函数的任何一个）方法；以一系列编码元素的形式表示。这些方法必须按 method_idx 以升序进行排序。
    // virtual_methods	encoded_method[virtual_methods_size]	定义的虚拟（非 static、private 或构造函数）方法；以一系列编码元素的形式表示。此列表不得包括继承方法，除非被此项所表示的类覆盖。这些方法必须按 method_idx 以升序进行排序。 虚拟方法的 method_idx 不得与任何直接方法相同。

    int staticFieldsSize;
    int instanceFieldsSize;
    int directMethodsSize;
    int virtualMethodsSize;

    EncodedField[] staticFields;
    EncodedField[] instanceFields;

    EncodedMethod[] directMethods;
    EncodedMethod[] virtualMethods;

    public ClassDataItem(int staticFieldsSize, int instanceFieldsSize, int directMethodsSize, int virtualMethodsSize) {
        this.staticFieldsSize = staticFieldsSize;
        this.instanceFieldsSize = instanceFieldsSize;
        this.directMethodsSize = directMethodsSize;
        this.virtualMethodsSize = virtualMethodsSize;
    }

    public EncodedField[] getStaticFields() {
        return staticFields;
    }

    public void setStaticFields(EncodedField[] staticFields) {
        this.staticFields = staticFields;
    }

    public EncodedField[] getInstanceFields() {
        return instanceFields;
    }

    public void setInstanceFields(EncodedField[] instanceFields) {
        this.instanceFields = instanceFields;
    }

    public EncodedMethod[] getDirectMethods() {
        return directMethods;
    }

    public void setDirectMethods(EncodedMethod[] directMethods) {
        this.directMethods = directMethods;
    }

    public EncodedMethod[] getVirtualMethods() {
        return virtualMethods;
    }

    public void setVirtualMethods(EncodedMethod[] virtualMethods) {
        this.virtualMethods = virtualMethods;
    }
}

class EncodedField {
    // field_idx_diff	uleb128	此字段标识（包括名称和描述符）的 field_ids 列表中的索引；它会表示为与列表中前一个元素的索引之间的差值。列表中第一个元素的索引则直接表示出来。
    // access_flags	uleb128	字段的访问标记（public、final 等）。如需了解详情，请参阅“access_flags 定义”
    int fieldIdxDiff;
    int accessFlags;

    public EncodedField(int fieldIdxDiff, int accessFlags) {
        this.fieldIdxDiff = fieldIdxDiff;
        this.accessFlags = accessFlags;
    }
}

class EncodedMethod {
    // method_idx_diff	uleb128	此方法标识（包括名称和描述符）的 method_ids 列表中的索引；它会表示为与列表中前一个元素的索引之间的差值。列表中第一个元素的索引则直接表示出来。
    // access_flags	uleb128	方法的访问标记（public、final等）。如需了解详情，请参阅“access_flags 定义”。
    // code_off	uleb128	从文件开头到此方法的代码结构的偏移量；如果此方法是 abstract 或 native，则该值为 0。偏移量应该是到 data 区段中某个位置的偏移量。数据格式由下文的“code_item”指定。
    int methodIdxDiff;
    int accessFlags;
    int codeOff;

    public EncodedMethod(int methodIdxDiff, int accessFlags, int codeOff) {
        this.methodIdxDiff = methodIdxDiff;
        this.accessFlags = accessFlags;
        this.codeOff = codeOff;
    }
}

class CodeItem {
    // registers_size	ushort	此代码使用的寄存器数量
    // ins_size	ushort	此代码所用方法的传入参数的字数
    // outs_size	ushort	此代码进行方法调用所需的传出参数空间的字数
    // tries_size	ushort	此实例的 try_item 数量。如果此值为非零值，则这些项会显示为 insns 数组（正好位于此实例中 tries 的后面）。
    // debug_info_off	uint	从文件开头到此代码的调试信息（行号 + 局部变量信息）序列的偏移量；如果没有任何信息，则该值为 0。该偏移量（如果为非零值）应该是到 data 区段中某个位置的偏移量。数据格式由下文的“debug_info_item”指定。
    // insns_size	uint	指令列表的大小（以 16 位代码单元为单位）
    // insns	ushort[insns_size]	字节码的实际数组。insns 数组中的代码格式由随附文档 Dalvik 字节码指定。请注意，尽管此项被定义为 ushort 的数组，但仍有一些内部结构倾向于采用四字节对齐方式。此外，如果此项恰好位于某个字节序交换文件中，则交换操作将只在单个 ushort 上进行，而不在较大的内部结构上进行。
    // padding	ushort（可选）= 0	使 tries 实现四字节对齐的两字节填充。只有 tries_size 为非零值且 insns_size 是奇数时，此元素才会存在。
    // tries	try_item[tries_size]（可选）	用于表示在代码中捕获异常的位置以及如何对异常进行处理的数组。该数组的元素在范围内不得重叠，且数值地址按照从低到高的顺序排列。只有 tries_size 为非零值时，此元素才会存在。
    // handlers	encoded_catch_handler_list（可选）	用于表示“捕获类型列表和关联处理程序地址”的列表的字节。每个 try_item 都具有到此结构的分组偏移量。只有 tries_size 为非零值时，此元素才会存在。

    int registersSize;
    int insSize;
    int outsSize;
    int triesSize;
    int debugInfoOff;
    int insnsSize;
    int[] insns;
    int padding;
    TryItem tries;
    EncodedCatchHandlerList handlers;

    public CodeItem(int registersSize, int insSize, int outsSize, int triesSize, int debugInfoOff, int insnsSize) {
        this.registersSize = registersSize;
        this.insSize = insSize;
        this.outsSize = outsSize;
        this.triesSize = triesSize;
        this.debugInfoOff = debugInfoOff;
        this.insnsSize = insnsSize;
    }

    public void setInsns(int[] insns) {
        this.insns = insns;
    }
}

class TryItem {
    // start_addr	uint	此条目涵盖的代码块的起始地址。该地址是到第一个所涵盖指令开头部分的 16 位代码单元的计数。
    // insn_count	ushort	此条目所覆盖的 16 位代码单元的数量。所涵盖（包含）的最后一个代码单元是 start_addr + insn_count - 1。
    // handler_off	ushort	从关联的 encoded_catch_hander_list 开头部分到此条目的 encoded_catch_handler 的偏移量（以字节为单位）。此偏移量必须是到 encoded_catch_handler 开头部分的偏移量。
    int startAddr;
    int insnCount;
    int handlerOff;

    public TryItem(int startAddr, int insnCount, int handlerOff) {
        this.startAddr = startAddr;
        this.insnCount = insnCount;
        this.handlerOff = handlerOff;
    }
}

class EncodedCatchHandlerList {
    // size	uleb128	列表的大小（以条目数表示）
    // list	encoded_catch_handler[handlers_size]	处理程序列表的实际列表，直接表示（不作为偏移量）并依序连接
    int size;
    EncodedCatchHandler[] list;
}

class EncodedCatchHandler {
    // size	sleb128	此列表中捕获类型的数量。如果为非正数，则该值是捕获类型数量的负数，捕获数量后跟一个“全部捕获”处理程序。例如，size 为 0 表示捕获类型为“全部捕获”，而没有明确类型的捕获。size 为 2 表示有两个明确类型的捕获，但没有“全部捕获”类型的捕获。size 为 -1 表示有一个明确类型的捕获和一个“全部捕获”类型的捕获。
    // handlers	encoded_type_addr_pair[abs(size)]	abs(size) 编码项的流（一种捕获类型对应一项），按照对类型进行测试时应遵循的顺序排列。
    // catch_all_addr	uleb128（可选）	“全部捕获”处理程序的字节码地址。只有当 size 为非正数时，此元素才会存在。
    int size;
    EncodedTypeAddrPair[] handlers;
    int catchAllAttr;
}

class EncodedTypeAddrPair {
    // type_idx	uleb128	要捕获的异常类型的 type_ids 列表中的索引
    // addr	uleb128	关联的异常处理程序的字节码地址
    int typeIdx;
    int addr;

    public EncodedTypeAddrPair(int typeIdx, int addr) {
        this.typeIdx = typeIdx;
        this.addr = addr;
    }
}