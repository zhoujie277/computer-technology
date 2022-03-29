# Java ClassFile Format

## StackMapTable 属性
StackMapTable 属性是一个变长属性，位于 Code 属性的属性表中。这个属性会在虚拟机类加载的类型阶段被使用。

StackMapTable 属性包含 0 至多个栈映射帧（Stack Map Frames），每个栈映射帧都显式或隐式地指定了一个字节码偏移量，用于表示局部变量表和操作数栈的验证类型。

类型检测器（Type Checker）会检查和处理目标方法的局部变量和操作数栈所需要的类型。本章节中，一个存储单元（Location）的含义是唯一的局部变量或操作数栈项。

我们还将用到术语“栈映射帧”（Stack Map Frame）和“类型状态”（Type State）来描述如何从方法的局部变量和操作数栈的存储单元映射到验证类型（Verification Types）。 当描述 Class 文件侧的映射时，我们通常使用的术语是“栈映射帧”，而当描述类型检查器侧的映射关系时，我们通常使用的术语是“类型状态”。
　　
在版本号大于或等于 50.0 的 Class 文件中，如果方法的 Code 属性中没有附带 StackMapTable 属性，那就意味着它带有一个隐式的 StackMap 属性。这个 StackMap 属性的作用等同于 number_of_entries 值为 0 的 StackMapTable 属性。一个方法的 Code 属性最多只能有一个 StackMapTable 属性，否则将抛出 ClassFormatError 异常。

StackMapTable 属性的格式如下：
```java
    StackMapTable_attribute {
        u2 attribute_name_index;
        u4 attribute_length;
        u2 number_of_entries;
        stack_map_frame entries[number_of_entries];
    }
```

StackMapTable 结构项的说明如下：

+ **attribute_name_index** 
    attribute_name_index 项的值必须是对常量池的有效索引，常量池在该索引的项处必须是CONSTANT_Utf8_info 结构，表示“StackMapTable”字符串。
+ **attribute_length**
    attribute_length 项的值表示当前属性的长度，不包括开始的 6 个字节。
+ **number_of_entries**
    number_of_entries 项的值给出了 entries 表中的成员数量。Entries 表的每个成员是都是一个 stack_map_frame 结构的项。
+ **entries[]**
    entries 表给出了当前方法所需的 stack_map_frame 结构。

每个 stack_map_frame 结构都使用一个特定的字节偏移量来表示类型状态。每个帧类型（Frame Type）都显式或隐式地标明一个 offset_delta（增量偏移量）值，用于计算每个帧在运行时的实际字节码偏移量。使用时帧的字节偏移量计算方法为：前一帧的字节码偏移量（Bytecode Offset）加上 offset_delta 的值再加 1，如果前一个帧是方法的初始帧（Initial Frame），那这时候字节码偏移量就是 offset_delta。

只要保证栈映射帧有正确的存储顺序，在类型检查时我们就可以使用增量偏移量而不是实际的字节码偏移量。此外，由于堆每一个帧都使用了 offset_delta + 1 的计算方式，我们可以确保偏移量不会重复。

在 Code 属性的 code[] 数组项中，如果偏移量 i 的位置是某条指令的起点，同时这个 Code 属性包含有 StackMapTable 属性，它的 entries 项中也有一个适用于地址偏移量 i 的 stack_map_frame 结构，那我们就说这条指令拥有一个与之相对应的栈映射帧。

stack_map_frame 结构的第一个字节作为类型标记（Tag），第一个字节后会跟随 0 或多个字节用于说明更多信息，这些信息因类型标记的不同而变化。

一个栈映射帧可以包含若干种帧类型（Frame Types）：

```c++
    union stack_map_frame {
        same_frame;
        same_locals_1_stack_item_frame;
        same_locals_1_stack_item_frame_extended;
        chop_frame;
        same_frame_extended;
        append_frame; 
        full_frame;
    } 
```

所有的帧类型，包括 full_frame，它们的部分语义会依赖于前置帧，这点使得确定基准帧（Very First Frame）变得尤为重要，方法的初始帧是隐式的，它通过方法描述符计算得出，详细信息请参考 methodInitialStackFrame。

帧类型 same_frame 的类型标记（frame_type）的取值范围是 0 至 63，如果类型标记所确定的帧类型是 same_frame 类型，则明当前帧拥有和前一个栈映射帧完全相同的 locals[] 数组，并且对应的 stack 项的成员个数为 0。当前帧的 offset_delta 值就使用 frame_type 项的值来表示。

```c++
    same_frame {
        u1 frame_type = SAME; /* 0-63 */
    }
```

帧类型 same_locals_1_stack_item_frame 的类型标记的取值范围是 64 至 127。如果类型标记所确定的帧类型是 same_locals_1_stack_item_frame 类型，则说明当前帧拥有和前一个栈映射帧完全相同的 locals[] 数组，同时对应的 stack[] 数组的成员个数为 1。当前帧的 offset_delta 值为 frame_type-64。并且有一个 verification_type_info 项跟随在此帧类型之后，用于表示那一个 stack 项的成员。

```
    same_locals_1_stack_item_frame {
        u1 frame_type = SAME_LOCALS_1_STACK_ITEM; /* 64-127 */
        verification_type_info stack[1];
    }
```

范围在 128 至 246 的类型标记值是为未来使用而预留的。

帧类型 same_locals_1_stack_item_frame_extended 由值为 247 的类型标记表示，它表明当前帧拥有和前一个栈映射帧完全相同的 locals[] 数组，同时对应的 stack[] 数组的成员个数为 1。当前帧的 offset_delta 的值需要由 offset_delta 项明确指定。有一个 stack[] 数组的成员跟随在 offset_delta 项之后。

```java
    same_locals_1_stack_item_frame_extended {
        u1 frame_type = SAME_LOCALS_1_STACK_ITEM_EXTENDED; /* 247 */
        u2 offset_delta; verification_type_info stack[1];
    }
```

帧类型 chop_frame 的类型标记的取值范围是 248 至 250。如果类型标记所确定的帧类型是为 chop_frame，则说明对应的操作数栈为空，并且拥有和前一个栈映射帧相同的 locals[] 数组，不过其中的第 k 个之后的 locals 项是不存在的。k 的值由 251 - frame_type 确定。

```java
    chop_frame {
        u1 frame_type = CHOP; /* 248-250 */
        u2 offset_delta;
    }
```

帧类型 same_frame_extended 由值为 251 的类型标记表示。如果类型标记所确定的帧类型是 same_frame_extended 类型，则说明当前帧有拥有和前一个栈映射帧的完全相同的 locals[] 数组，同时对应的 stack[] 数组的成员数量为 0。

```java
    same_frame_extended {
        u1 frame_type = SAME_FRAME_EXTENDED; /* 251 */
        u2 offset_delta;
    }
```

帧类型 append_frame 的类型标记的取值范围是 252 至 254。如果类型标记所确定的帧类型为 append_frame，则说明对应操作数栈为空，并且包含和前一个栈映射帧相同的 locals[] 数组，不过还额外附加 k 个的 locals 项。k 值为 frame_type - 251。

```java
    append_frame {
        u1 frame_type = APPEND; /* 252-254 */
        u2 offset_delta;
        verification_type_info locals[frame_type - 251];
    }
```

在 locals[]数组中，索引为 0 的（第一个）成员表示第一个添加的局部变量。如果要从条件“ locals[M]表示第 N 个局部变量”中推导出结论“locals[M + 1]就表示第 N + 1 个局部变量”的话，那就意味着 locals[M] 一定是下列结构之一: 
+ Top_variable_info
+ Integer_variable_info
+ Float_variable_info
+ Null_variable_info
+ UninitializedThis_variable_info
+ Object_variable_info
+ Uninitialized_variable_info

否则，locals[M+1] 就将表示第 N + 2 个局部变量。对于任意的索引 i，locals[i] 所表示的局部变量的索引都不能大于此方法的局部变量表的最大索引值。
　　
在 stack[] 数组中，索引为 0 的（第一个）成员表示操作数栈的最底部的元素，之后的成员依次靠近操作数栈的顶部。操作数栈栈底的元素对应的索引为 0，我们称之为元素 0，之后元素依次是元素 1、元素 2 等。如果要从条件“ stack[M] 表示第 N 个元素”中推导出结论“ stack[M + 1] 表示第 N + 1 个元素”的话，那就意味着 stack[M] 一定是下列结构之一：

+ Top_variable_info
+ Integer_variable_info
+ Float_variable_info
+ Null_variable_info
+ UninitializedThis_variable_info
+ Object_variable_info
+ Uninitialized_variable_info

否则，stack[M + 1] 将表示第 N + 2 个元素，对于任意的索引 i，stack[i] 所表示的栈元素索引都不能大于此方法的操作数的最大深度。

verification_type_info 结构的第一个字节 tag 作为类型标记，之后跟随 0 至多个字节表示由 tag 类型所决定的信息。每个 verification_type_info 结构可以描述 1 个至 2 个存储单元的验证类型信息。

```c++
    union verification_type_info {
        Top_variable_info;
        Integer_variable_info;
        Float_variable_info;
        Long_variable_info;
        Double_variable_info;
        Null_variable_info;
        UninitializedThis_variable_info;
        Object_variable_info;
        Uninitialized_variable_info;
    }
```

Top_variable_info 类型说明这个局部变量拥有验证类型 top(ᴛ)。

```c++
    Top_variable_info {
        u1 tag = ITEM_Top; /* 0 */
    }
```

Integer_variable_info 类型说明这个局部变量包含验证类型 int

```c++
    Integer_variable_info {
        u1 tag = ITEM_Integer; /* 1 */
    } 
```

Float_variable_info 类型说明局部变量包含验证类型 float

```c++
    Float_variable_info {
        u1 tag = ITEM_Float; /* 2 */
    }
```

Long_variable_info 类型说明存储单元包含验证类型 long，如果存储单元是局部变量，则要求：不能是最大索引值的局部变量。按顺序计数的下一个局部变量包含验证类型 ᴛ 。如果单元存储是操作数栈成员，则要求：当前的存储单元不能在栈顶。靠近栈顶方向的下一个存储单元包含验证类型 ᴛ。Long_variable_info 结构在局部变量表或操作数栈中占用 2 个存储单元。

```c++
    Long_variable_info {
        u1 tag = ITEM_Long; /* 4 */
    }
```

Double_variable_info 类型说明存储单元包含验证类型 double。如果存储单元是局部变量，则要求：不能是最大索引值的局部变量。按顺序计数的下一个局部变量包含验证类型 ᴛ 。如果单元存储是操作数栈成员，则要求：当前的存储单元不能在栈顶。靠近栈顶方向的下一个存储单元包含验证类型 ᴛ。 Double_variable_info 结构在局部变量表或操作数栈中占用 2 个存储单元。

```c++
    Double_variable_info {
        u1 tag = ITEM_Double; /* 3 */
    }
```

Null_variable_info 类型说明存储单元包含验证类型 null。

```c++
    Null_variable_info {
        u1 tag = ITEM_Null; /* 5 */
    }
```

UninitializedThis_variable_info 类型说明存储单元包含验证类型uninitializedThis。

```c++
    UninitializedThis_variable_info {
        u1 tag = ITEM_UninitializedThis; /* 6 */
    }
```

Object_variable_info 类型说明存储单元包含某个 Class 的实例。由常量池在cpool_index 给出的索引处的 CONSTANT_CLASS_Info 结构表示。

```c++
    Object_variable_info {
        u1 tag = ITEM_Object; /* 7 */
        u2 cpool_index;
    }
```

Uninitialized_variable_info 说明存储单元包含验证类型。uninitialized(offset)。offset 项给出了一个偏移量，表示在包含此 StackMapTable 属性的 Code 属性中，new 指令创建的对象所存储的位置。

```c++
    Uninitialized_variable_info {
        u1 tag = ITEM_Uninitialized /* 8 */
        u2 offset;
    }
```