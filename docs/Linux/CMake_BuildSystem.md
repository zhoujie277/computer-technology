# CMake-BuildSystem

[TOC]

## Introduction
> A CMake-based buildsystem is organized as a set of high-level logical targets. Each target corresponds to an executable or library, or is a custom target containing custom commands. Dependencies between the targets are expressed in the buildsystem to determine the build order and the rules for regeneration in response to change.

一个基于 CMake 的构建系统被组织成一组高级逻辑目标。每个目标对应于一个可执行文件或库，或者是一个包含自定义命令的自定义目标。目标之间的依赖关系在构建系统中被表达出来，以确定构建顺序和响应变化的再生规则。

## Binary Targets
> Executables and libraries are defined using the `add_executable()` and `add_library()` commands. The resulting binary files have appropriate `PREFIX`, `SUFFIX` and extensions for the platform targeted. Dependencies between binary targets are expressed using the `target_link_libraries()` command:

可执行文件和库是用 `add_executable()` 和 `add_library()` 命令定义的。由此产生的二进制文件具有适当的 `PREFIX`、`SUFFIX` 和目标平台的扩展。二进制目标之间的依赖关系用 `target_link_libraries()` 命令表示。

```bash
add_library(archive archive.cpp zip.cpp lzma.cpp)
add_executable(zipapp zipapp.cpp)
target_link_libraries(zipapp archive)
```

> `archive` is defined as a `STATIC` library -- an archive containing objects compiled from `archive.cpp`, `zip.cpp`, and `lzma.cpp`. `zipapp` is defined as an executable formed by compiling and linking `zipapp.cpp`. When linking the `zipapp` executable, the archive static library is linked in.

`archive` 被定义为一个 `STATIC` 库 -- 一个包含由 `archive.cpp`、`zip.cpp` 和 `lzma.cpp` 编译的对象的档案。`zipapp` 被定义为通过编译和连接 `zipapp.cpp` 形成的可执行文件。当链接 `zipapp` 可执行文件时，存档静态库被链接进来。

### Binary Executables
The `add_executable()` command defines an executable target:

`add_executable()` 命令定义了一个可执行的目标。

```bash
add_executable(mytool mytool.cpp)
```

> Commands such as `add_custom_command()`, which generates rules to be run at build time can transparently use an `EXECUTABLE` target as a `COMMAND` executable. The buildsystem rules will ensure that the executable is built before attempting to run the command.

诸如 `add_custom_command()` 这样的命令，在构建时生成规则，可以透明地使用 `EXECUTABLE` 目标作为 `COMMAND` 可执行文件。构建系统规则将确保在尝试运行命令之前，可执行文件已经被构建。

### Binary Library Types

#### Normal Libraries
> By default, the `add_library()` command defines a STATIC library, unless a type is specified. A type may be specified when using the command:

默认情况下，`add_library()` 命令定义了一个 STATIC 库，除非指定一个类型。在使用该命令时可以指定一个类型。

```bash
add_library(archive SHARED archive.cpp zip.cpp lzma.cpp)
add_library(archive STATIC archive.cpp zip.cpp lzma.cpp)
```

The `BUILD_SHARED_LIBS` variable may be enabled to change the behavior of `add_library()` to build shared libraries by default.

可以启用 "BUILD_SHARED_LIBS" 变量来改变 "add_library() "的行为，以默认方式构建共享库。

> In the context of the buildsystem definition as a whole, it is largely irrelevant whether particular libraries are `SHARED` or `STATIC` -- the commands, dependency specifications and other APIs work similarly regardless of the library type. The `MODULE` library type is dissimilar in that it is generally not linked to -- it is not used in the right-hand-side of the `target_link_libraries()` command. It is a type which is loaded as a plugin using runtime techniques. If the library does not export any unmanaged symbols (e.g. Windows resource DLL, C++/CLI DLL), it is required that the library not be a `SHARED` library because CMake expects `SHARED` libraries to export at least one symbol.

就整个构建系统的定义而言，特定的库是 "SHARED" 还是 "STATIC" 在很大程度上是不相关的 -- 无论库的类型如何，命令、依赖规范和其他 API 的工作方式都是相似的。`MODULE` 库类型的不同之处在于它通常不被链接到--它不被用于 `target_link_libraries()` 命令的右侧。它是一种使用运行时技术作为插件加载的类型。如果该库没有输出任何非管理的符号（例如 Windows 资源 DLL，C++/CLI DLL），则要求该库不是一个 `SHARED` 库，因为 CMake 期望 `SHARED` 库至少输出一个符号。

```bash
add_library(archive MODULE 7z.cpp)
```

#### Apple Frameworks
> A `SHARED` library may be marked with the `FRAMEWORK` target property to create an macOS or iOS Framework Bundle. A library with the `FRAMEWORK` target property should also set the `FRAMEWORK_VERSION` target property. This property is typically set to the value of "A" by macOS conventions. The `MACOSX_FRAMEWORK_IDENTIFIER` sets `CFBundleIdentifier` key and it uniquely identifies the bundle.

一个 `SHARED` 库可以用 `FRAMEWORK` 目标属性标记，以创建一个 macOS 或 iOS 框架包。具有`FRAMEWORK` 目标属性的库还应该设置 `FRAMEWORK_VERSION` 目标属性。根据 macOS 的惯例，这个属性通常被设置为 "A "的值。`MACOSX_FRAMEWORK_IDENTIFIER` 设置 `CFBundleIdentifier` 键，它可以唯一地识别捆绑包。

```bash
add_library(MyFramework SHARED MyFramework.cpp)
set_target_properties(MyFramework PROPERTIES
  FRAMEWORK TRUE
  FRAMEWORK_VERSION A # Version "A" is macOS convention
  MACOSX_FRAMEWORK_IDENTIFIER org.cmake.MyFramework
)
```

#### Object Libraries
> The `OBJECT` library type defines a non-archival collection of object files resulting from compiling the given source files. The object files collection may be used as source inputs to other targets by using the syntax `$<TARGET_OBJECTS:name>`. This is a `generator expression` that can be used to supply the `OBJECT` library content to other targets:

`OBJECT` 库类型定义了一个非存档的对象文件集合，这些对象文件是通过编译给定的源文件产生的。对象文件集合可以通过使用语法 `$<TARGET_OBJECTS:name>` 作为其他目标的源输入。这是一个 "生成器表达式"，可用于向其他目标提供 "OBJECT" 库内容。

```bash
add_library(archive OBJECT archive.cpp zip.cpp lzma.cpp)

add_library(archiveExtras STATIC $<TARGET_OBJECTS:archive> extras.cpp)

add_executable(test_exe $<TARGET_OBJECTS:archive> test.cpp)
```

> The link (or archiving) step of those other targets will use the object files collection in addition to those from their own sources.

这些其他目标的链接（或归档）步骤将使用对象文件集，此外还有来自其自身来源的文件。

> Alternatively, object libraries may be linked into other targets:

另外，对象库可以被链接到其他目标中。

```bash
add_library(archive OBJECT archive.cpp zip.cpp lzma.cpp)

add_library(archiveExtras STATIC extras.cpp)
target_link_libraries(archiveExtras PUBLIC archive)

add_executable(test_exe test.cpp)
target_link_libraries(test_exe archive)
```

The link (or archiving) step of those other targets will use the object files from `OBJECT` libraries that are directly linked. Additionally, usage requirements of the `OBJECT` libraries will be honored when compiling sources in those other targets. Furthermore, those usage requirements will propagate transitively to dependents of those other targets.

这些其他目标的链接（或归档）步骤将使用直接链接的 `OBJECT` 库的对象文件。此外，在编译其他目标的源文件时，`OBJECT` 库的使用要求将被遵守。此外，这些使用要求将传播到这些其他目标的依赖者身上。

Object libraries may not be used as the `TARGET` in a use of the `add_custom_command(TARGET)` command signature. However, the list of objects can be used by `add_custom_command(OUTPUT)` or `file(GENERATE)` by using `$<TARGET_OBJECTS:objlib>`.

在使用 `add_custom_command(TARGET)` 命令签名时，不能将对象库作为 `TARGET` 使用。然而，通过使用 `$<TARGET_OBJECTS:objlib>`，对象列表可以被`add_custom_command(OUTPUT)`或`file(GENERATE)` 使用。

## Build Specification and Usage Requirements
The `target_include_directories()`, `target_compile_definitions()` and `target_compile_options()` commands specify the build specifications and the usage requirements of binary targets. The commands populate the `INCLUDE_DIRECTORIES`, `COMPILE_DEFINITIONS` and `COMPILE_OPTIONS` target properties respectively, and/or the `INTERFACE_INCLUDE_DIRECTORIES`, `INTERFACE_COMPILE_DEFINITIONS` and `INTERFACE_COMPILE_OPTIONS` target properties.

`target_include_directories()`, `target_compile_definitions()` 和`target_compile_options()` 命令指定二进制目标的构建规范和使用要求。这些命令分别填充`INCLUDE_DIRECTORIES`、`COMPILE_DEFINITIONS` 和 `COMPILE_OPTIONS` 目标属性，和/或 `INTERFACE_INCLUDE_DIRECTORIES`、`INTERFACE_COMPILE_DEFINITIONS` 和`INTERFACE_COMPILE_OPTIONS` 目标属性。


Each of the commands has a `PRIVATE`, `PUBLIC` and `INTERFACE` mode. The `PRIVATE` mode populates only the non-`INTERFACE_` variant of the target property and the INTERFACE mode populates only the `INTERFACE_` variants. The `PUBLIC` mode populates both variants of the respective target property. Each command may be invoked with multiple uses of each keyword:

每个命令都有 `PRIVATE`、`PUBLIC` 和 `INTERFACE` 模式。`PRIVATE` 模式只填充目标属性的非`INTERFACE_` 变体，INTERFACE 模式只填充 `INTERFACE_` 变体。`PUBLIC` 模式会填充各自目标属性的两个变体。每个命令都可以通过多次使用每个关键字来调用。

```bash
target_compile_definitions(archive
  PRIVATE BUILDING_WITH_LZMA
  INTERFACE USING_ARCHIVE_LIB
)
```

Note that usage requirements are not designed as a way to make downstreams use particular `COMPILE_OPTIONS` or `COMPILE_DEFINITIONS` etc for convenience only. The contents of the properties must be requirements, not merely recommendations or convenience.

请注意，使用要求不是为了让下游使用特定的 `COMPILE_OPTIONS` 或 `COMPILE_DEFINITIONS` 等，只是为了方便。属性的内容必须是要求，而不仅仅是建议或方便。

See the `Creating Relocatable Packages` section of the `cmake-packages(7)` manual for discussion of additional care that must be taken when specifying usage requirements while creating packages for redistribution.

请参阅 `cmake-packages(7)` 手册中的 `Creating Relocatable Packages` 一节，以了解在创建重新发布的软件包时必须注意的其他问题。

### Target Properties
> The contents of the `INCLUDE_DIRECTORIES`, `COMPILE_DEFINITIONS` and `COMPILE_OPTIONS` target properties are used appropriately when compiling the source files of a binary target.

在编译二进制目标的源文件时，`INCLUDE_DIRECTORIES`、`COMPILE_DEFINITIONS` 和`COMPILE_OPTIONS` 目标属性的内容被适当使用。

> Entries in the `INCLUDE_DIRECTORIES` are added to the compile line with -I or -isystem prefixes and in the order of appearance in the property value.

`INCLUDE_DIRECTORIES` 中的条目以 -I 或 -isystem 为前缀，按照属性值的出现顺序添加到编译行中。

> Entries in the `COMPILE_DEFINITIONS` are prefixed with `-D` or `/D` and added to the compile line in an unspecified order. The `DEFINE_SYMBOL` target property is also added as a compile definition as a special convenience case for `SHARED` and `MODULE` library targets.

`COMPILE_DEFINITIONS` 中的条目以 `D` 或 `D` 为前缀，并以不指定的顺序添加到编译行中。`DEFINE_SYMBOL` 目标属性也被添加到编译定义中，作为 `SHARED` 和 `MODULE` 库目标的一个特殊便利情况。

> Entries in the `COMPILE_OPTIONS` are escaped for the shell and added in the order of appearance in the property value. Several compile options have special separate handling, such as `POSITION_INDEPENDENT_CODE`.

`COMPILE_OPTIONS` 中的条目对 shell 来说是转义的，并按照属性值中出现的顺序添加。有几个编译选项有特殊的单独处理，如 `POSITION_INDEPENDENT_CODE`。

> The contents of the `INTERFACE_INCLUDE_DIRECTORIES`, `INTERFACE_COMPILE_DEFINITIONS` and `INTERFACE_COMPILE_OPTIONS` target properties are Usage Requirements -- they specify content which consumers must use to correctly compile and link with the target they appear on. For any binary target, the contents of each INTERFACE_ property on each target specified in a `target_link_libraries()` command is consumed:

`INTERFACE_INCLUDE_DIRECTORIES` 、`INTERFACE_COMPILE_DEFINITIONS` 和 `INTERFACE_COMPILE_OPTIONS` 目标属性的内容是使用要求 -- 它们指定了消费者必须使用的内容，以正确编译和链接它们出现的目标。对于任何二进制目标，在 `target_link_libraries()` 命令中指定的每个目标上的每个 INTERFACE_ 属性的内容都被消耗。

```bash
set(srcs archive.cpp zip.cpp)
if (LZMA_FOUND)
  list(APPEND srcs lzma.cpp)
endif()
add_library(archive SHARED ${srcs})
if (LZMA_FOUND)
  # The archive library sources are compiled with -DBUILDING_WITH_LZMA
  target_compile_definitions(archive PRIVATE BUILDING_WITH_LZMA)
endif()
target_compile_definitions(archive INTERFACE USING_ARCHIVE_LIB)

add_executable(consumer)
# Link consumer to archive and consume its usage requirements. The consumer
# executable sources are compiled with -DUSING_ARCHIVE_LIB.
target_link_libraries(consumer archive)
```

> Because it is common to require that the source directory and corresponding build directory are added to the `INCLUDE_DIRECTORIES`, the `CMAKE_INCLUDE_CURRENT_DIR` variable can be enabled to conveniently add the corresponding directories to the `INCLUDE_DIRECTORIES` of all targets. The variable `CMAKE_INCLUDE_CURRENT_DIR_IN_INTERFACE` can be enabled to add the corresponding directories to the `INTERFACE_INCLUDE_DIRECTORIES` of all targets. This makes use of targets in multiple different directories convenient through use of the `target_link_libraries()` command.

因为通常要求将源目录和相应的构建目录加入到 "INCLUDE_DIRECTORIES" 中，所以可以启用  "CMAKE_INCLUDE_CURRENT_DIR" 变量，以方便地将相应的目录加入到所有目标的  "INCLUDE_DIRECTORIES" 中。变量 `CMAKE_INCLUDE_CURRENT_DIR_IN_INTERFACE` 可以被启用，以便将相应的目录添加到所有目标的 `INTERFACE_INCLUDE_DIRECTORIES`。这使得通过使用 `target_link_libraries()` 命令来使用多个不同目录中的目标更为方便。

### Transitive Usage Requirements
> The usage requirements of a target can transitively propagate to dependents. The `target_link_libraries()` command has `PRIVATE`, `INTERFACE` and `PUBLIC` keywords to control the propagation.

一个目标的使用要求可以过渡性地传播给附属物。`target_link_libraries()` 命令有 `PRIVATE`、`INTERFACE` 和 `PUBLIC` 关键字来控制传播。

```bash
add_library(archive archive.cpp)
target_compile_definitions(archive INTERFACE USING_ARCHIVE_LIB)

add_library(serialization serialization.cpp)
target_compile_definitions(serialization INTERFACE USING_SERIALIZATION_LIB)

add_library(archiveExtras extras.cpp)
target_link_libraries(archiveExtras PUBLIC archive)
target_link_libraries(archiveExtras PRIVATE serialization)
# archiveExtras is compiled with -DUSING_ARCHIVE_LIB
# and -DUSING_SERIALIZATION_LIB

add_executable(consumer consumer.cpp)
# consumer is compiled with -DUSING_ARCHIVE_LIB
target_link_libraries(consumer archiveExtras)
```

> Because `archive` is a `PUBLIC` dependency of `archiveExtras`, the usage requirements of it are propagated to `consumer` too. Because `serialization` is a `PRIVATE` dependency of `archiveExtras`, the usage requirements of it are not propagated to `consumer`.

因为 `archive` 是 `archiveExtras` 的 `PUBLIC` 依赖项，它的使用要求也会传播给`consumer`。因为 `serialization` 是 `archiveExtras` 的 `PRIVATE` 依赖关系，它的使用要求不会传播给 `consumer`。

> Generally, a dependency should be specified in a use of `target_link_libraries()` with the `PRIVATE` keyword if it is used by only the implementation of a library, and not in the header files. If a dependency is additionally used in the header files of a library (e.g. for class inheritance), then it should be specified as a `PUBLIC` dependency. A dependency which is not used by the implementation of a library, but only by its headers should be specified as an INTERFACE dependency. The `target_link_libraries()` command may be invoked with multiple uses of each keyword:

一般来说，如果一个依赖关系只在库的实现中使用，而不是在头文件中使用，那么应该在使用 `target_link_libraries()` 时使用 `PRIVATE` 关键字来指定。如果一个依赖关系在库的头文件中被额外使用（例如，用于类的继承），那么它应该被指定为 `PUBLIC` 依赖关系。一个不被库的实现使用，而只被其头文件使用的依赖应该被指定为 INTERFACE 依赖。`target_link_libraries()` 命令可以在多次使用每个关键字的情况下被调用。

```bash
target_link_libraries(archiveExtras
  PUBLIC archive
  PRIVATE serialization
)
```

> Usage requirements are propagated by reading the `INTERFACE_` variants of target properties from dependencies and appending the values to the non-`INTERFACE_` variants of the operand. For example, the `INTERFACE_INCLUDE_DIRECTORIES` of dependencies is read and appended to the `INCLUDE_DIRECTORIES` of the operand. In cases where order is relevant and maintained, and the order resulting from the `target_link_libraries()` calls does not allow correct compilation, use of an appropriate command to set the property directly may update the order.

使用要求是通过从依赖关系中读取目标属性的 `INTERFACE_` 变体并将其附加到操作数的非`INTERFACE_` 变体中来传播的。例如，依赖关系的 `INTERFACE_INCLUDE_DIRECTORIES` 被读取并附加到操作数的 `INCLUDE_DIRECTORIES`。如果顺序是相关的并被维护的，而由`target_link_libraries()` 调用产生的顺序不允许正确的编译，使用适当的命令直接设置属性可以更新顺序。

> For example, if the linked libraries for a target must be specified in the order `lib1` `lib2` `lib3` , but the include directories must be specified in the order `lib3` `lib1` `lib2`:

例如，如果一个目标的链接库必须按照 `lib1``lib2``lib3` 的顺序指定，但包含目录必须按照`lib3``lib1``lib2` 的顺序指定。

```bash
target_link_libraries(myExe lib1 lib2 lib3)
target_include_directories(myExe
  PRIVATE $<TARGET_PROPERTY:lib3,INTERFACE_INCLUDE_DIRECTORIES>)
```

> Note that care must be taken when specifying usage requirements for targets which will be exported for installation using the `install(EXPORT)` command. See `Creating Packages` for more.

请注意，在指定目标的使用要求时必须注意，这些目标将使用 `install(EXPORT)` 命令导出进行安装。参见 `Creating Packages` 以了解更多信息。

### Compatible Interface Properties
> Some target properties are required to be compatible between a target and the interface of each dependency. For example, the `POSITION_INDEPENDENT_CODE` target property may specify a boolean value of whether a target should be compiled as position-independent-code, which has platform-specific consequences. A target may also specify the usage requirement `INTERFACE_POSITION_INDEPENDENT_CODE` to communicate that consumers must be compiled as position-independent-code.

一些目标属性需要在目标和每个依赖关系的接口之间兼容。例如，`POSITION_INDEPENDENT_CODE` 目标属性可以指定一个布尔值，即目标是否应被编译为位置无关的代码，这具有平台特定的后果。一个目标也可以指定 `INTERFACE_POSITION_INDEPENDENT_CODE` 的使用要求，以告知消费者必须被编译为位置无关的代码。

```bash
add_executable(exe1 exe1.cpp)
set_property(TARGET exe1 PROPERTY POSITION_INDEPENDENT_CODE ON)

add_library(lib1 SHARED lib1.cpp)
set_property(TARGET lib1 PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE ON)

add_executable(exe2 exe2.cpp)
target_link_libraries(exe2 lib1)
```

> Here, both `exe1` and `exe2` will be compiled as position-independent-code. lib1 will also be compiled as position-independent-code because that is the default setting for `SHARED` libraries. If dependencies have conflicting, non-compatible requirements `cmake(1)` issues a diagnostic:

这里，`exe1` 和 `exe2` 都将被编译为独立于位置的代码。lib1 也将被编译为独立于位置的代码，因为那是 `SHARED` 库的默认设置。如果依赖关系有冲突的、不兼容的要求，`cmake(1)` 会发出诊断。

```bash
add_library(lib1 SHARED lib1.cpp)
set_property(TARGET lib1 PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE ON)

add_library(lib2 SHARED lib2.cpp)
set_property(TARGET lib2 PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE OFF)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1)
set_property(TARGET exe1 PROPERTY POSITION_INDEPENDENT_CODE OFF)

add_executable(exe2 exe2.cpp)
target_link_libraries(exe2 lib1 lib2)
```

> The `lib1` requirement `INTERFACE_POSITION_INDEPENDENT_CODE` is not "compatible" with the `POSITION_INDEPENDENT_CODE` property of the `exe1` target. The library requires that consumers are built as position-independent-code, while the executable specifies to not built as position-independent-code, so a diagnostic is issued.

`lib1` 要求 `INTERFACE_POSITION_INDEPENDENT_CODE` 与 `exe1` 目标的`POSITION_INDEPENDENT_CODE` 属性不兼容。库要求消费者以独立于位置的代码方式构建，而可执行文件则指定不以独立于位置的代码方式构建，因此发出了诊断。

> The `lib1` and `lib2` requirements are not "compatible". One of them requires that consumers are built as position-independent-code, while the other requires that consumers are not built as position-independent-code. Because `exe2` links to both and they are in conflict, a CMake error message is issued:

`lib1` 和 `lib2` 的要求是不 "兼容" 的。其中一个要求消费者被构建为独立于位置的代码，而另一个则要求消费者不被构建为独立于位置的代码。因为 `exe2` 链接到了这两个要求，而且它们之间有冲突，所以发出了 CMake 错误信息。

```bash
CMake Error: The INTERFACE_POSITION_INDEPENDENT_CODE property of "lib2" does
not agree with the value of POSITION_INDEPENDENT_CODE already determined
for "exe2".
```

> To be "compatible", the `POSITION_INDEPENDENT_CODE` property, if set must be either the same, in a boolean sense, as the `INTERFACE_POSITION_INDEPENDENT_CODE` property of all transitively specified dependencies on which that property is set.

为了 "兼容"，"POSITION_INDEPENDENT_CODE" 属性，如果被设置，必须在布尔意义上，与所有被设置的过渡性指定依赖的 "INTERFACE_POSITION_INDEPENDENT_CODE" 属性相同。

> This property of "compatible interface requirement" may be extended to other properties by specifying the property in the content of the `COMPATIBLE_INTERFACE_BOOL` target property. Each specified property must be compatible between the consuming target and the corresponding property with an `INTERFACE_` prefix from each dependency:

通过在 `COMPATIBLE_INTERFACE_BOOL` 目标属性的内容中指定属性，这个 "兼容接口要求" 的属性可以扩展到其他属性。每个指定的属性必须在消费目标和每个依赖关系中带有 `INTERFACE_` 前缀的相应属性之间兼容。

```bash
add_library(lib1Version2 SHARED lib1_v2.cpp)
set_property(TARGET lib1Version2 PROPERTY INTERFACE_CUSTOM_PROP ON)
set_property(TARGET lib1Version2 APPEND PROPERTY
  COMPATIBLE_INTERFACE_BOOL CUSTOM_PROP
)

add_library(lib1Version3 SHARED lib1_v3.cpp)
set_property(TARGET lib1Version3 PROPERTY INTERFACE_CUSTOM_PROP OFF)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1Version2) # CUSTOM_PROP will be ON

add_executable(exe2 exe2.cpp)
target_link_libraries(exe2 lib1Version2 lib1Version3) # Diagnostic
```

> Non-boolean properties may also participate in "compatible interface" computations. Properties specified in the `COMPATIBLE_INTERFACE_STRING` property must be either unspecified or compare to the same string among all transitively specified dependencies. This can be useful to ensure that multiple incompatible versions of a library are not linked together through transitive requirements of a target:

```bash
add_library(lib1Version2 SHARED lib1_v2.cpp)
set_property(TARGET lib1Version2 PROPERTY INTERFACE_LIB_VERSION 2)
set_property(TARGET lib1Version2 APPEND PROPERTY
  COMPATIBLE_INTERFACE_STRING LIB_VERSION
)

add_library(lib1Version3 SHARED lib1_v3.cpp)
set_property(TARGET lib1Version3 PROPERTY INTERFACE_LIB_VERSION 3)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1Version2) # LIB_VERSION will be "2"

add_executable(exe2 exe2.cpp)
target_link_libraries(exe2 lib1Version2 lib1Version3) # Diagnostic
```

> The `COMPATIBLE_INTERFACE_NUMBER_MAX` target property specifies that content will be evaluated numerically and the maximum number among all specified will be calculated:

```bash
add_library(lib1Version2 SHARED lib1_v2.cpp)
set_property(TARGET lib1Version2 PROPERTY INTERFACE_CONTAINER_SIZE_REQUIRED 200)
set_property(TARGET lib1Version2 APPEND PROPERTY
  COMPATIBLE_INTERFACE_NUMBER_MAX CONTAINER_SIZE_REQUIRED
)

add_library(lib1Version3 SHARED lib1_v3.cpp)
set_property(TARGET lib1Version3 PROPERTY INTERFACE_CONTAINER_SIZE_REQUIRED 1000)

add_executable(exe1 exe1.cpp)
# CONTAINER_SIZE_REQUIRED will be "200"
target_link_libraries(exe1 lib1Version2)

add_executable(exe2 exe2.cpp)
# CONTAINER_SIZE_REQUIRED will be "1000"
target_link_libraries(exe2 lib1Version2 lib1Version3)
```

> Similarly, the `COMPATIBLE_INTERFACE_NUMBER_MIN` may be used to calculate the numeric minimum value for a property from dependencies.

同样，`COMPATIBLE_INTERFACE_NUMBER_MIN' 可用于从依赖关系中计算一个属性的数字最小值。

> Each calculated "compatible" property value may be read in the consumer at generate-time using generator expressions.

每个计算出来的 "兼容 "属性值都可以在生成时使用生成器表达式在消费者中读取。

> Note that for each dependee, the set of properties specified in each compatible interface property must not intersect with the set specified in any of the other properties.

请注意，对于每个被依赖者，在每个兼容接口属性中指定的属性集不得与任何其他属性中指定的属性集相交。

### Property Origin Debugging
> Because build specifications can be determined by dependencies, the lack of locality of code which creates a target and code which is responsible for setting build specifications may make the code more difficult to reason about. `cmake(1)` provides a debugging facility to print the origin of the contents of properties which may be determined by dependencies. The properties which can be debugged are listed in the `CMAKE_DEBUG_TARGET_PROPERTIES` variable documentation:

因为构建规范可以由依赖关系决定，创建目标的代码和负责设置构建规范的代码缺乏地域性，这可能使代码更难推理。`cmake(1)` 提供了一个调试工具，可以打印可能由依赖关系决定的属性内容的来源。可以调试的属性在 `CMAKE_DEBUG_TARGET_PROPERTIES` 变量文档中列出。

```bash
set(CMAKE_DEBUG_TARGET_PROPERTIES
  INCLUDE_DIRECTORIES
  COMPILE_DEFINITIONS
  POSITION_INDEPENDENT_CODE
  CONTAINER_SIZE_REQUIRED
  LIB_VERSION
)
add_executable(exe1 exe1.cpp)
```

> In the case of properties listed in `COMPATIBLE_INTERFACE_BOOL` or `COMPATIBLE_INTERFACE_STRING`, the debug output shows which target was responsible for setting the property, and which other dependencies also defined the property. In the case of `COMPATIBLE_INTERFACE_NUMBER_MAX` and `COMPATIBLE_INTERFACE_NUMBER_MIN`, the debug output shows the value of the property from each dependency, and whether the value determines the new extreme.

在 `COMPATIBLE_INTERFACE_BOOL` 或 `COMPATIBLE_INTERFACE_STRING` 中列出的属性的情况下，调试输出显示哪个目标负责设置该属性，以及哪些其他依赖也定义了该属性。在`COMPATIBLE_INTERFACE_NUMBER_MAX` 和 `COMPATIBLE_INTERFACE_NUMBER_MIN` 的情况下，调试输出显示每个依赖关系的属性值，以及该值是否决定了新的极端。

### Build Specification with Generator Expressions
> Build specifications may use `generator expressions` containing content which may be conditional or known only at generate-time. For example, the calculated "compatible" value of a property may be read with the `TARGET_PROPERTY` expression:

构建规范可以使用 "生成器表达式"，其中包含的内容可能是有条件的或只在生成时知道的。例如，可以用 "TARGET_PROPERTY " 表达式读取一个属性的 "兼容 "计算值。

```bash
add_library(lib1Version2 SHARED lib1_v2.cpp)
set_property(TARGET lib1Version2 PROPERTY
  INTERFACE_CONTAINER_SIZE_REQUIRED 200)
set_property(TARGET lib1Version2 APPEND PROPERTY
  COMPATIBLE_INTERFACE_NUMBER_MAX CONTAINER_SIZE_REQUIRED
)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1Version2)
target_compile_definitions(exe1 PRIVATE
    CONTAINER_SIZE=$<TARGET_PROPERTY:CONTAINER_SIZE_REQUIRED>
)
```

In this case, the exe1 source files will be compiled with `-DCONTAINER_SIZE=200`.

在这种情况下，exe1 源文件将以 -DCONTAINER_SIZE=200 进行编译。

> The unary `TARGET_PROPERTY` generator expression and the TARGET_POLICY generator expression are evaluated with the consuming target context. This means that a usage requirement specification may be evaluated differently based on the consumer:

单元的 TARGET_PROPERTY 生成器表达式和 TARGET_POLICY 生成器表达式是通过消费的目标上下文进行评估的。这意味着一个使用要求规范可能会根据消费者的不同而被评估。

```bash
add_library(lib1 lib1.cpp)
target_compile_definitions(lib1 INTERFACE
  $<$<STREQUAL:$<TARGET_PROPERTY:TYPE>,EXECUTABLE>:LIB1_WITH_EXE>
  $<$<STREQUAL:$<TARGET_PROPERTY:TYPE>,SHARED_LIBRARY>:LIB1_WITH_SHARED_LIB>
  $<$<TARGET_POLICY:CMP0041>:CONSUMER_CMP0041_NEW>
)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1)

cmake_policy(SET CMP0041 NEW)

add_library(shared_lib shared_lib.cpp)
target_link_libraries(shared_lib lib1)
```

> The `exe1` executable will be compiled with `-DLIB1_WITH_EXE`, while the shared_lib shared library will be compiled with `-DLIB1_WITH_SHARED_LIB` and `-DCONSUMER_CMP0041_NEW`, because policy `CMP0041` is NEW at the point where the `shared_lib` target is created.

exe1 可执行文件将用 -DLIB1_WITH_EXE 编译，而 shared_lib 共享库将用-DLIB1_WITH_SHARED_LIB 和 -DCONSUMER_CMP0041_NEW 编译，因为策略 CMP0041 在 shared_lib 目标创建时是 NEW。

> The `BUILD_INTERFACE` expression wraps requirements which are only used when consumed from a target in the same buildsystem, or when consumed from a target exported to the build directory using the `export()` command. The `INSTALL_INTERFACE` expression wraps requirements which are only used when consumed from a target which has been installed and exported with the `install(EXPORT)` command:

`BUILD_INTERFACE` 表达式包装的需求，只在从同一构建系统中的目标消耗时使用，或者从使用 `export()` 命令导出到构建目录的目标消耗时使用。`INSTALL_INTERFACE` 表达式包装的需求，只在从已经安装并使用 `install(EXPORT)` 命令导出的目标中消耗时使用。

```bash
add_library(ClimbingStats climbingstats.cpp)
target_compile_definitions(ClimbingStats INTERFACE
  $<BUILD_INTERFACE:ClimbingStats_FROM_BUILD_LOCATION>
  $<INSTALL_INTERFACE:ClimbingStats_FROM_INSTALLED_LOCATION>
)
install(TARGETS ClimbingStats EXPORT libExport ${InstallArgs})
install(EXPORT libExport NAMESPACE Upstream::
        DESTINATION lib/cmake/ClimbingStats)
export(EXPORT libExport NAMESPACE Upstream::)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 ClimbingStats)
```

> In this case, the `exe1` executable will be compiled with `-DClimbingStats_FROM_BUILD_LOCATION`. The exporting commands generate `IMPORTED` targets with either the `INSTALL_INTERFACE` or the `BUILD_INTERFACE` omitted, and the `*_INTERFACE` marker stripped away. A separate project consuming the `ClimbingStats` package would contain:

在这种情况下，`exe1` 可执行文件将被编译为 `-DClimbingStats_FROM_BUILD_LOCATION`。导出命令生成 `IMPORTED` 目标，省略 `INSTALL_INTERFACE` 或 `BUILD_INTERFACE`，并剥离 `*_INTERFACE` 标记。一个消耗 `ClimbingStats` 包的独立项目将包含。

```bash
find_package(ClimbingStats REQUIRED)

add_executable(Downstream main.cpp)
target_link_libraries(Downstream Upstream::ClimbingStats)
```

> Depending on whether the `ClimbingStats` package was used from the build location or the install location, the `Downstream` target would be compiled with either `-DClimbingStats_FROM_BUILD_LOCATION` or `-DClimbingStats_FROM_INSTALL_LOCATION`. For more about packages and exporting see the `cmake-packages(7)` manual.

根据 `ClimbingStats` 包是来自构建位置还是安装位置，`Downstream` 目标将被编译为`-DClimbingStats_FROM_BUILD_LOCATION` 或 `-DClimbingStats_FROM_INSTALL_LOCATION`。关于包和导出的更多信息，请参见  `cmake-packages(7)` 手册。

#### Include Directories and Usage Requirements
> Include directories require some special consideration when specified as usage requirements and when used with generator expressions. The `target_include_directories()` command accepts both relative and absolute include directories:

当被指定为使用要求和与生成器表达式一起使用时，包含目录需要一些特别的考虑。 `target_include_directories()` 命令同时接受相对和绝对的包含目录。

```bash
add_library(lib1 lib1.cpp)
target_include_directories(lib1 PRIVATE
  /absolute/path
  relative/path
)
```

> Relative paths are interpreted relative to the source directory where the command appears. Relative paths are not allowed in the `INTERFACE_INCLUDE_DIRECTORIES` of `IMPORTED` targets.

相对路径的解释是相对于命令出现的源目录而言的。相对路径在 `IMPORTED` 目标的 `INTERFACE_INCLUDE_DIRECTORIES` 中是不允许的。

> In cases where a non-trivial generator expression is used, the `INSTALL_PREFIX` expression may be used within the argument of an `INSTALL_INTERFACE` expression. It is a replacement marker which expands to the installation prefix when imported by a consuming project.

在使用非琐碎的生成器表达式的情况下，`INSTALL_PREFIX` 表达式可以在 `INSTALL_INTERFACE` 表达式的参数中使用。它是一个替换标记，当被消费项目导入时，会扩展到安装前缀。

> Include directories usage requirements commonly differ between the build-tree and the install-tree. The `BUILD_INTERFACE` and `INSTALL_INTERFACE` generator expressions can be used to describe separate usage requirements based on the usage location. Relative paths are allowed within the `INSTALL_INTERFACE` expression and are interpreted relative to the installation prefix. For example:

包含目录的使用要求通常在构建树和安装树之间有所不同。`BUILD_INTERFACE` 和 `INSTALL_INTERFACE` 生成器表达式可以用来描述基于使用位置的单独使用要求。在 `INSTALL_INTERFACE` 表达式中允许相对路径，并且是相对于安装前缀解释的。比如说。

```bash
add_library(ClimbingStats climbingstats.cpp)
target_include_directories(ClimbingStats INTERFACE
  $<BUILD_INTERFACE:${CMAKE_CURRENT_BINARY_DIR}/generated>
  $<INSTALL_INTERFACE:/absolute/path>
  $<INSTALL_INTERFACE:relative/path>
  $<INSTALL_INTERFACE:$<INSTALL_PREFIX>/$<CONFIG>/generated>
)
```

> Two convenience APIs are provided relating to include directories usage requirements. The `CMAKE_INCLUDE_CURRENT_DIR_IN_INTERFACE` variable may be enabled, with an equivalent effect to:

提供了两个与include目录使用要求有关的便利API。可以启用 `CMAKE_INCLUDE_CURRENT_DIR_IN_INTERFACE` 变量，其效果等同于。

```bash
set_property(TARGET tgt APPEND PROPERTY INTERFACE_INCLUDE_DIRECTORIES
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR};${CMAKE_CURRENT_BINARY_DIR}>
)
```

for each target affected. The convenience for installed targets is an `INCLUDES` `DESTINATION` component with the `install(TARGETS)` command:

```bash
install(TARGETS foo bar bat EXPORT tgts ${dest_args}
  INCLUDES DESTINATION include
)
install(EXPORT tgts ${other_args})
install(FILES ${headers} DESTINATION include)
```

> This is equivalent to appending `${CMAKE_INSTALL_PREFIX}/include` to the `INTERFACE_INCLUDE_DIRECTORIES` of each of the installed `IMPORTED` targets when generated by `install(EXPORT)`.

这相当于在由 `install(EXPORT)` 生成时， 将 `${CMAKE_INSTALL_PREFIX}/include` 追加到每个已安装的 `IMPORTED` 目标的 `INTERFACE_INCLUDE_DIRECTORIES` 中。

> When the `INTERFACE_INCLUDE_DIRECTORIES` of an `imported target` is consumed, the entries in the property are treated as `SYSTEM` include directories, as if they were listed in the `INTERFACE_SYSTEM_INCLUDE_DIRECTORIES` of the dependency. This can result in omission of compiler warnings for headers found in those directories. This behavior for `Imported Targets` may be controlled by setting the `NO_SYSTEM_FROM_IMPORTED` target property on the consumers of imported targets, or by setting the `IMPORTED_NO_SYSTEM` target property on the imported targets themselves.

当 "导入目标 "的 "INTERFACE_INCLUDE_DIRECTORIES" 被消耗时，该属性中的条目被视为 "SYSTEM" 包含目录，就像它们被列在依赖关系的 "INTERFACE_SYSTEM_INCLUDE_DIRECTORIES" 中。这可能导致在这些目录中发现的头文件的编译器警告被省略。对于 "导入目标" 的这种行为，可以通过在导入目标的消费者身上设置 "NO_SYSTEM_FROM_IMPORTED" 目标属性，或在导入目标本身设置 "IMPORTED_NO_SYSTEM" 目标属性来控制。

> If a binary target is linked transitively to a macOS `FRAMEWORK`, the `Headers` directory of the framework is also treated as a usage requirement. This has the same effect as passing the framework directory as an include directory.

如果一个二进制目标链接到 macOS `FRAMEWORK`，框架的 `Headers` 目录也会被视为一个使用要求。这与将框架目录作为一个包含目录传递的效果相同。

### Link Libraries and Generator Expressions
> Like build specifications, `link libraries` may be specified with generator expression conditions. However, as consumption of usage requirements is based on collection from linked dependencies, there is an additional limitation that the link dependencies must form a "directed acyclic graph". That is, if linking to a target is dependent on the value of a target property, that target property may not be dependent on the linked dependencies:

像构建规范一样，"链接库" 可以用生成器表达条件来指定。然而，由于使用要求的消耗是基于从链接的依赖关系的收集，还有一个限制，即链接的依赖关系必须形成一个 "有向无环图"。也就是说，如果链接到一个目标取决于一个目标属性的值，该目标属性可能不依赖于链接的依赖关系。

```bash
add_library(lib1 lib1.cpp)
add_library(lib2 lib2.cpp)
target_link_libraries(lib1 PUBLIC
  $<$<TARGET_PROPERTY:POSITION_INDEPENDENT_CODE>:lib2>
)
add_library(lib3 lib3.cpp)
set_property(TARGET lib3 PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE ON)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 lib1 lib3)
```

> As the value of the `POSITION_INDEPENDENT_CODE` property of the `exe1` target is dependent on the linked libraries (`lib3`), and the edge of linking `exe1` is determined by the same `POSITION_INDEPENDENT_CODE` property, the dependency graph above contains a cycle. `cmake(1)` issues an error message.

由于 `exe1` 目标的 `POSITION_INDEPENDENT_CODE` 属性的值依赖于链接的库（`lib3`），而链接`exe1` 的边缘由相同的 `POSITION_INDEPENDENT_CODE` 属性决定，上面的依赖图包含一个循环。 `cmake(1)` 发出一个错误信息。

### Output Artifacts
> The buildsystem targets created by the `add_library()` and `add_executable()` commands create rules to create binary outputs. The exact output location of the binaries can only be determined at generate-time because it can depend on the build-configuration and the link-language of linked dependencies etc. `TARGET_FILE`, `TARGET_LINKER_FILE` and related expressions can be used to access the name and location of generated binaries. These expressions do not work for `OBJECT` libraries however, as there is no single file generated by such libraries which is relevant to the expressions.

由 "add_library() "和 "add_executable()" 命令创建的构建系统目标将创建二进制输出规则。二进制文件的确切输出位置只能在生成时确定，因为它可能取决于构建配置和链接依赖的链接语言等。`TARGET_FILE`, `TARGET_LINKER_FILE` 和相关表达式可以用来访问生成的二进制文件的名称和位置。然而，这些表达式对 `OBJECT` 库不起作用，因为这种库生成的文件没有一个与表达式相关。

> There are three kinds of output artifacts that may be build by targets as detailed in the following sections. Their classification differs between DLL platforms and non-DLL platforms. All Windows-based systems including Cygwin are DLL platforms.

有三种可能由目标建立的输出工件，详见以下章节。它们的分类在DLL平台和非DLL平台之间有所不同。所有基于Windows的系统包括Cygwin都是DLL平台。

#### Runtime Output Artifacts
A runtime output artifact of a buildsystem target may be:
+ The executable file (e.g. `.exe`) of an executable target created by the `add_executable()` command.
+ On DLL platforms: the executable file (e.g. `.dll`) of a shared library target created by the `add_library()` command with the `SHARED` option.

The `RUNTIME_OUTPUT_DIRECTORY` and `RUNTIME_OUTPUT_NAME` target properties may be used to control runtime output artifact locations and names in the build tree.

#### Library Output Artifacts
A library output artifact of a buildsystem target may be:

一个构建系统目标的库输出工件可能是。

+ The loadable module file (e.g. `.dll` or `.so`) of a module library target created by the `add_library()` command with the `MODULE` option.
+ On non-DLL platforms: the shared library file (e.g. `.so` or `.dylib`) of a shared library target created by the `add_library()` command with the `SHARED` option.

The `LIBRARY_OUTPUT_DIRECTORY` and `LIBRARY_OUTPUT_NAME` target properties may be used to control library output artifact locations and names in the build tree.

`LIBRARY_OUTPUT_DIRECTORY` 和 `LIBRARY_OUTPUT_NAME` 目标属性可用于控制构建树中库输出工件的位置和名称。

#### Archive Output Artifacts
An archive output artifact of a buildsystem target may be:
+ The static library file (e.g. `.lib` or `.a`) of a static library target created by the `add_library()` command with the `STATIC` option.
+ On DLL platforms: the import library file (e.g. `.lib`) of a shared library target created by the `add_library()` command with the `SHARED` option. This file is only guaranteed to exist if the library exports at least one unmanaged symbol.
+ On DLL platforms: the import library file (e.g. `.lib`) of an executable target created by the `add_executable()` command when its ENABLE_EXPORTS target property is set.
+ On AIX: the linker import file (e.g. `.imp`) of an executable target created by the `add_executable()` command when its `ENABLE_EXPORTS` target property is set.

> The `ARCHIVE_OUTPUT_DIRECTORY` and `ARCHIVE_OUTPUT_NAME` target properties may be used to control archive output artifact locations and names in the build tree.

`ARCHIVE_OUTPUT_DIRECTORY` 和 `ARCHIVE_OUTPUT_NAME` 目标属性可用于控制构建树中归档输出工件的位置和名称。

### Directory-Scoped Commands
> The `target_include_directories()`, `target_compile_definitions()` and `target_compile_options()` commands have an effect on only one target at a time. The commands `add_compile_definitions()`, `add_compile_options()` and `include_directories()` have a similar function, but operate at directory scope instead of target scope for convenience.

`target_include_directories()`, `target_compile_definitions()`和`target_compile_options()`命令一次只能对一个目标产生影响。命令`add_compile_definitions()`、`add_compile_options()`和`include_directories()`有类似的功能，但为了方便，在目录范围而不是目标范围操作。

## Build Configurations
> Configurations determine specifications for a certain type of build, such as `Release` or `Debug`. The way this is specified depends on the type of `generator` being used. For single configuration generators like `Makefile Generators` and `Ninja`, the configuration is specified at configure time by the `CMAKE_BUILD_TYPE` variable. For multi-configuration generators like `Visual Studio`, `Xcode`, and `Ninja Multi-Config`, the configuration is chosen by the user at build time and `CMAKE_BUILD_TYPE` is ignored. In the multi-configuration case, the set of available configurations is specified at configure time by the `CMAKE_CONFIGURATION_TYPES` variable, but the actual configuration used cannot be known until the build stage. This difference is often misunderstood, leading to problematic code like the following:

配置决定了某种类型的构建规格，如 "发布 "或 "调试"。指定的方式取决于正在使用的 "生成器 "的类型。对于像 "Makefile Generators "和 "Ninja "这样的单配置生成器，配置是在配置时由 "CMAKE_BUILD_TYPE "变量指定。对于多配置生成器，如`Visual Studio'、`Xcode'和`Ninja Multi-Config'，配置由用户在构建时选择，`CMAKE_BUILD_TYPE'被忽略。在多配置的情况下，可用的配置集在配置时由`CMAKE_CONFIGURATION_TYPES`变量指定，但实际使用的配置在构建阶段才能知道。这种差异经常被误解，导致出现如下的问题代码。

```bash
# WARNING: This is wrong for multi-config generators because they don't use
#          and typically don't even set CMAKE_BUILD_TYPE
string(TOLOWER ${CMAKE_BUILD_TYPE} build_type)
if (build_type STREQUAL debug)
  target_compile_definitions(exe1 PRIVATE DEBUG_BUILD)
endif()
```

> `Generator expressions` should be used instead to handle configuration-specific logic correctly, regardless of the generator used. For example:

应该使用 "生成器表达式"，以正确处理特定的配置逻辑，无论使用何种生成器。比如说。

```bash
# Works correctly for both single and multi-config generators
target_compile_definitions(exe1 PRIVATE
  $<$<CONFIG:Debug>:DEBUG_BUILD>
)
```

> In the presence of `IMPORTED` targets, the content of `MAP_IMPORTED_CONFIG_DEBUG` is also accounted for by the above `$<CONFIG:Debug>` expression.

在存在 "IMPORTED "目标的情况下， `MAP_IMPORTED_CONFIG_DEBUG` 的内容也由上述`$<CONFIG:Debug>` 表达式来说明。

### Case Sensitivity
`CMAKE_BUILD_TYPE` and `CMAKE_CONFIGURATION_TYPES` are just like other variables in that any string comparisons made with their values will be case-sensitive. The `$<CONFIG>` generator expression also preserves the casing of the configuration as set by the user or CMake defaults. For example:

`CMAKE_BUILD_TYPE` 和 `CMAKE_CONFIGURATION_TYPES` 与其他变量一样，与它们的值进行的任何字符串比较都是大小写敏感的。`$<CONFIG>` 生成器表达式还保留了用户或 CMake 默认设置的配置的大小写。例如。

```bash
# NOTE: Don't use these patterns, they are for illustration purposes only.

set(CMAKE_BUILD_TYPE Debug)
if(CMAKE_BUILD_TYPE STREQUAL DEBUG)
  # ... will never get here, "Debug" != "DEBUG"
endif()
add_custom_target(print_config ALL
  # Prints "Config is Debug" in this single-config case
  COMMAND ${CMAKE_COMMAND} -E echo "Config is $<CONFIG>"
  VERBATIM
)

set(CMAKE_CONFIGURATION_TYPES Debug Release)
if(DEBUG IN_LIST CMAKE_CONFIGURATION_TYPES)
  # ... will never get here, "Debug" != "DEBUG"
endif()
```

> In contrast, CMake treats the configuration type case-insensitively when using it internally in places that modify behavior based on the configuration. For example, the `$<CONFIG:Debug>` generator expression will evaluate to 1 for a configuration of not only `Debug`, but also `DEBUG`, `debug` or even `DeBuG`. Therefore, you can specify configuration types in `CMAKE_BUILD_TYPE` and `CMAKE_CONFIGURATION_TYPES` with any mixture of upper and lowercase, although there are strong conventions (see the next section). If you must test the value in string comparisons, always convert the value to upper or lowercase first and adjust the test accordingly.

相比之下，CMake 在内部使用配置类型时，对其进行不敏感的处理，这些地方根据配置来修改行为。例如，"$<CONFIG:Debug>" 生成器表达式在配置为 "Debug"，以及 "DEBUG"、"debug "甚至 "DeBuG "的情况下都会评估为 1。因此，你可以在 `CMAKE_BUILD_TYPE` 和 `CMAKE_CONFIGURATION_TYPES` 中用大写和小写的任何混合物来指定配置类型，尽管有强烈的约定（见下一节）。如果你必须在字符串比较中测试数值，总是先将数值转换成大写或小写，并相应地调整测试。

### Default And Custom Configurations
By default, CMake defines a number of standard configurations:

+ `Debug`
+ `Release`
+ `RelWithDebInfo`
+ `MinSizeRel`

> In multi-config generators, the `CMAKE_CONFIGURATION_TYPES` variable will be populated with (potentially a subset of) the above list by default, unless overridden by the project or user. The actual configuration used is selected by the user at build time.

在多配置生成器中，"CMAKE_CONFIGURATION_TYPES" 变量将默认填充上述列表（可能是其中的一个子集），除非被项目或用户重写。实际使用的配置是由用户在构建时选择的。

For single-config generators, the configuration is specified with the `CMAKE_BUILD_TYPE` variable at configure time and cannot be changed at build time. The default value will often be none of the above standard configurations and will instead be an empty string. A common misunderstanding is that this is the same as `Debug`, but that is not the case. Users should always explicitly specify the build type instead to avoid this common problem.

对于单配置生成器，配置是在配置时用 `CMAKE_BUILD_TYPE` 变量指定的，在构建时不能改变。默认值通常不是上述标准配置，而是一个空字符串。一个常见的误解是，这与 `Debug` 相同，但事实并非如此。用户应该明确指定构建类型，以避免这个常见问题。

The above standard configuration types provide reasonable behavior on most platforms, but they can be extended to provide other types. Each configuration defines a set of compiler and linker flag variables for the language in use. These variables follow the convention `CMAKE_<LANG>_FLAGS_<CONFIG>`, where `<CONFIG>` is always the uppercase configuration name. When defining a custom configuration type, make sure these variables are set appropriately, typically as cache variables.

上述标准配置类型在大多数平台上提供了合理的行为，但它们可以被扩展以提供其他类型。每个配置都为所使用的语言定义了一组编译器和链接器的标志变量。这些变量遵循惯例`CMAKE_<LANG>_FLAGS_<CONFIG>`，其中 `<CONFIG>` 总是大写的配置名称。当定义一个自定义的配置类型时，确保这些变量被适当地设置，通常是作为缓存变量。

## Pseudo Targets
Some target types do not represent outputs of the buildsystem, but only inputs such as external dependencies, aliases or other non-build artifacts. Pseudo targets are not represented in the generated buildsystem.

有些目标类型并不代表构建系统的输出，而只是输入，如外部依赖、别名或其他非构建工件。伪目标在生成的构建系统中不被表示。

### Imported Targets
> An `IMPORTED` target represents a pre-existing dependency. Usually such targets are defined by an upstream package and should be treated as immutable. After declaring an `IMPORTED` target one can adjust its target properties by using the customary commands such as `target_compile_definitions()`, `target_include_directories()`, `target_compile_options()` or `target_link_libraries()` just like with any other regular target.

一个 `IMPORTED` 目标代表一个预先存在的依赖关系。通常这样的目标是由上游软件包定义的，应该被视为不可改变的。在声明了 `IMPORTED` 目标后，可以通过使用常规的命令来调整它的属性，例如 `target_compile_definitions()`, `target_include_directories()`, `target_compile_options()` 或 `target_link_libraries()` 就像其他常规目标那样。

> `IMPORTED` targets may have the same usage requirement properties populated as binary targets, such as `INTERFACE_INCLUDE_DIRECTORIES`, `INTERFACE_COMPILE_DEFINITIONS`, `INTERFACE_COMPILE_OPTIONS`, `INTERFACE_LINK_LIBRARIES`, and `INTERFACE_POSITION_INDEPENDENT_CODE`.

`IMPORTED`目标可以拥有与二进制目标相同的使用要求属性，例如`INTERFACE_INCLUDE_DIRECTORIES`、`INTERFACE_COMPILE_DEFINITIONS`、`INTERFACE_COMPILE_OPTIONS`、`INTERFACE_LINK_LIBRARIES`和`INTERFACE_POSITION_INDEPENDENT_CODE`。

> The `LOCATION` may also be read from an IMPORTED target, though there is rarely reason to do so. Commands such as `add_custom_command()` can transparently use an `IMPORTED EXECUTABLE` target as a `COMMAND` executable.

`LOCATION` 也可以从一个导入的目标中读取，尽管很少有理由这样做。诸如 `add_custom_command()`这样的命令可以透明地使用一个 `IMPORTED EXECUTABLE` 目标作为 `COMMAND` 可执行文件。

> The scope of the definition of an `IMPORTED` target is the directory where it was defined. It may be accessed and used from subdirectories, but not from parent directories or sibling directories. The scope is similar to the scope of a cmake variable.

`IMPORTED` 目标的定义范围是定义它的目录。它可以从子目录中被访问和使用，但不能从父目录或同级目录中被访问。这个范围类似于 cmake 变量的范围。

> It is also possible to define a `GLOBAL` `IMPORTED` target which is accessible globally in the buildsystem.

也可以定义一个 "GLOBAL "的 "IMPORTED "目标，可以在构建系统中全局访问。

See the `cmake-packages(7)` manual for more on creating packages with `IMPORTED` targets.

### Alias Targets
> An `ALIAS` target is a name which may be used interchangeably with a binary target name in read-only contexts. A primary use-case for `ALIAS` targets is for example or unit test executables accompanying a library, which may be part of the same buildsystem or built separately based on user configuration.

`ALIAS` 目标是一个名称，在只读的情况下可以与二进制目标名称互换使用。`ALIAS` 目标的一个主要用途是伴随着库的例子或单元测试可执行文件，这可能是同一个构建系统的一部分，也可能是基于用户配置的单独构建。

```bash
add_library(lib1 lib1.cpp)
install(TARGETS lib1 EXPORT lib1Export ${dest_args})
install(EXPORT lib1Export NAMESPACE Upstream:: ${other_args})

add_library(Upstream::lib1 ALIAS lib1)
```

> In another directory, we can link unconditionally to the `Upstream::lib1` target, which may be an `IMPORTED` target from a package, or an ALIAS target if built as part of the same buildsystem.

在另一个目录中，我们可以无条件地链接到`Upstream::lib1`目标，它可能是一个软件包的`IMPORTED`目标，或者是一个ALIAS目标，如果作为同一构建系统的一部分构建的话。

```bash
if (NOT TARGET Upstream::lib1)
  find_package(lib1 REQUIRED)
endif()
add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 Upstream::lib1)
```

> `ALIAS` targets are not mutable, installable or exportable. They are entirely local to the buildsystem description. A name can be tested for whether it is an `ALIAS` name by reading the `ALIASED_TARGET` property from it:

`ALIAS`目标是不可变的，不可安装的，也不可导出的。它们完全是构建系统的本地描述。一个名字可以通过读取`ALIASED_TARGET`属性来测试它是否是`ALIAS`的名字。

```bash
get_target_property(_aliased Upstream::lib1 ALIASED_TARGET)
if(_aliased)
  message(STATUS "The name Upstream::lib1 is an ALIAS for ${_aliased}.")
endif()
```

### Interface Libraries
> An `INTERFACE` library target does not compile sources and does not produce a library artifact on disk, so it has no `LOCATION`.

一个 "INTERFACE" 库目标不编译源代码，也不在磁盘上产生一个库工件，所以它没有 "LOCATION"。

It may specify usage requirements such as `INTERFACE_INCLUDE_DIRECTORIES`, `INTERFACE_COMPILE_DEFINITIONS`, `INTERFACE_COMPILE_OPTIONS`, `INTERFACE_LINK_LIBRARIES`, `INTERFACE_SOURCES`, and `INTERFACE_POSITION_INDEPENDENT_CODE`. Only the `INTERFACE` modes of the `target_include_directories()`, `target_compile_definitions()`, `target_compile_options()`, `target_sources()`, and `target_link_libraries()` commands may be used with `INTERFACE` libraries.

它可以指定诸如 `INTERFACE_INCLUDE_DIRECTORIES`、`INTERFACE_COMPILE_DEFINITIONS`、`INTERFACE_COMPILE_OPTIONS`、`INTERFACE_LINK_LIBRARIES`、`INTERFACE_SOURCES` 和 `INTERFACE_POSITION_INDEPENDENT_CODE` 等使用要求。只有 `target_include_directories()`、`target_compile_definitions()`、`target_compile_options()`、`target_sources()` 和 `target_link_libraries()` 命令的 `INTERFACE` 模式可以用于 `INTERFACE` 库。

> Since CMake 3.19, an `INTERFACE` library target may optionally contain source files. An interface library that contains source files will be included as a build target in the generated buildsystem. It does not compile sources, but may contain custom commands to generate other sources. Additionally, IDEs will show the source files as part of the target for interactive reading and editing.

从 CMake 3.19 开始，`INTERFACE` 库目标可以选择包含源文件。一个包含源文件的接口库将作为构建目标被包含在生成的构建系统中。它不编译源代码，但可能包含生成其他源代码的自定义命令。此外，IDE 将显示源文件作为目标的一部分，以便进行交互式阅读和编辑。

> A primary use-case for `INTERFACE` libraries is header-only libraries.

`INTERFACE` 库的一个主要使用情况是只用头的库。

```bash
add_library(Eigen INTERFACE
  src/eigen.h
  src/vector.h
  src/matrix.h
  )
target_include_directories(Eigen INTERFACE
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/src>
  $<INSTALL_INTERFACE:include/Eigen>
)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 Eigen)
```

> Here, the usage requirements from the `Eigen` target are consumed and used when compiling, but it has no effect on linking.

在这里，编译时消耗并使用来自`Eigen`目标的使用要求，但对链接没有影响。

> Another use-case is to employ an entirely target-focussed design for usage requirements:

另一个用例是采用一个完全以目标为中心的设计来满足使用要求。

```bash
add_library(pic_on INTERFACE)
set_property(TARGET pic_on PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE ON)
add_library(pic_off INTERFACE)
set_property(TARGET pic_off PROPERTY INTERFACE_POSITION_INDEPENDENT_CODE OFF)

add_library(enable_rtti INTERFACE)
target_compile_options(enable_rtti INTERFACE
  $<$<OR:$<COMPILER_ID:GNU>,$<COMPILER_ID:Clang>>:-rtti>
)

add_executable(exe1 exe1.cpp)
target_link_libraries(exe1 pic_on enable_rtti)
```

> This way, the build specification of `exe1` is expressed entirely as linked targets, and the complexity of compiler-specific flags is encapsulated in an `INTERFACE` library target.

这样，"exe1" 的构建规范完全以链接目标的形式表达，而编译器特定标志的复杂性被封装在一个 "INTERFACE" 库目标中。

> `INTERFACE` libraries may be installed and exported. Any content they refer to must be installed separately:

`INTERFACE` 库可以被安装和导出。它们所引用的任何内容必须单独安装。

```bash
set(Eigen_headers
  src/eigen.h
  src/vector.h
  src/matrix.h
  )
add_library(Eigen INTERFACE ${Eigen_headers})
target_include_directories(Eigen INTERFACE
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/src>
  $<INSTALL_INTERFACE:include/Eigen>
)

install(TARGETS Eigen EXPORT eigenExport)
install(EXPORT eigenExport NAMESPACE Upstream::
  DESTINATION lib/cmake/Eigen
)
install(FILES ${Eigen_headers}
  DESTINATION include/Eigen
)
```