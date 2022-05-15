# User Interaction Guide

[TOC]

## Introduction
Where a software package supplies a CMake-based buildsystem with the source of their software, the consumer of the software is required to run a CMake user interaction tool in order to build it.

当一个软件包将其软件的源代码提供给基于 CMake 的构建系统时，该软件的使用者需要运行 CMake 用户交互工具来构建它。

Well-behaved CMake-based buildsystems do not create any output in the source directory, so typically, the user performs an out-of-source build and performs the build there. First, CMake must be instructed to generate a suitable buildsystem, then the user invokes a build tool to process that generated buildsystem. The generated buildsystem is specific to the machine used to generate it and is not redistributable. Each consumer of a provided source software package is required to use CMake to generate a buildsystem specific to their system.

行为良好的基于 CMake 的构建系统不会在源码目录中创建任何输出，所以通常情况下，用户执行源外构建并在那里进行构建。首先，CMake 必须被指示生成一个合适的构建系统，然后用户调用一个构建工具来处理这个生成的构建系统。生成的构建系统是特定于用于生成它的机器的，并且不可重新分配。每个使用所提供的源代码软件包的用户都需要使用 CMake 来生成一个针对其系统的构建系统。

Generated buildsystems should generally be treated as read-only. The CMake files as a primary artifact should completely specify the buildsystem and there should be no reason to populate properties manually in an IDE for example after generating the buildsystem. CMake will periodically rewrite the generated buildsystem, so modifications by users will be overwritten.

生成的构建系统通常应被视为只读。作为主要工件的 CMake 文件应该完全指定构建系统，没有理由在生成构建系统后在 IDE 中手动填充属性。CMake 会定期重写生成的构建系统，所以用户的修改会被覆盖。

The features and user interfaces described in this manual are available for all CMake-based build systems by virtue of providing CMake files.

本手册中描述的功能和用户界面，通过提供 CMake 文件，可用于所有基于 CMake 的构建系统。

The CMake tooling may report errors to the user when processing provided CMake files, such as reporting that the compiler is not supported, or the compiler does not support a required compile option, or a dependency can not be found. These errors must be resolved by the user by choosing a different compiler, `installing dependencies`, or instructing CMake where to find them, etc.

在处理所提供的 CMake 文件时，CMake 工具可能会向用户报告错误，例如报告编译器不被支持，或者编译器不支持所需的编译选项，或者找不到某个依赖关系。这些错误必须由用户通过选择不同的编译器、"安装依赖项 "或指示 CMake 在哪里找到它们等方式解决。

### Command Line cmake tool
A simple but typical use of `cmake(1)` with a fresh copy of software source code is to create a build directory and invoke cmake there:

cmake(1) 的一个简单而典型的使用方法是，在一份新的软件源代码副本中创建一个联编目录并在那里调用 cmake。

```bash
cd some_software-1.4.2
mkdir build
cd build
cmake .. -DCMAKE_INSTALL_PREFIX=/opt/the/prefix
cmake --build .
cmake --build . --target install
```

It is recommended to build in a separate directory to the source because that keeps the source directory pristine, allows for building a single source with multiple toolchains, and allows easy clearing of build artifacts by simply deleting the build directory.

建议在与源码分开的目录下进行构建，因为这样可以保持源码目录的原始性，允许用多个工具链来构建一个源码，并且可以通过简单地删除构建目录来轻松清除构建工件。

The CMake tooling may report warnings which are intended for the provider of the software, not intended for the consumer of the software. Such warnings end with "This warning is for project developers". Users may disable such warnings by passing the `-Wno-dev` flag to `cmake(1)`.

CMake 工具可能会报告针对软件提供者的警告，而不是针对软件的消费者的警告。这样的警告以 "此警告是针对项目开发者的" 结尾。用户可以通过向 cmake(1) 传递 -Wno-dev 标志来禁用此类警告。

### cmake-gui tool
Users more accustomed to GUI interfaces may use the cmake-gui(1) tool to invoke CMake and generate a buildsystem.

更习惯于 GUI 界面的用户可以使用 `cmake-gui(1)` 工具来调用 CMake 并生成一个构建系统。

The source and binary directories must first be populated. It is always advised to use different directories for the source and the build.

首先必须填入源码和二进制目录。我们总是建议为源码和构建使用不同的目录。

## Generating a Buildsystem
There are several user interface tools which may be used to generate a buildsystem from CMake files. The `ccmake(1)` and `cmake-gui(1)` tools guide the user through setting the various necessary options. The `cmake(1)` tool can be invoked to specify options on the command line. This manual describes options which may be set using any of the user interface tools, though the mode of setting an option is different for each tool.

有几个用户界面工具可以用来从 CMake 文件生成构建系统。`ccmake(1)` 和 `cmake-gui(1)` 工具引导用户设置各种必要的选项。`cmake(1)` 工具可以被调用来指定命令行上的选项。本手册描述了可以使用任何一个用户界面工具设置的选项，尽管每个工具设置选项的模式都不一样。

### Command line enviroment
When invoking `cmake(1)` with a command line buildsystem such as `Makefiles` or `Ninja`, it is necessary to use the correct build environment to ensure that build tools are available. CMake must be able to find the appropriate `build tool`, compiler, linker and other tools as needed.

当用命令行构建系统如 `Makefiles` 或 `Ninja` 调用 `cmake(1)` 时，有必要使用正确的构建环境以确保构建工具的可用性。CMake 必须能够找到适当的`构建工具`、编译器、链接器和其他需要的工具。

On Linux systems, the appropriate tools are often provided in system-wide locations and may be readily installed through the system package manager. Other toolchains provided by the user or installed in non-default locations can also be used.

在 Linux 系统中，适当的工具通常提供在系统范围内的位置，并且可以通过系统软件包管理器随时安装。也可以使用由用户提供的或安装在非默认位置的其他工具链。

When cross-compiling, some platforms may require environment variables to be set or may provide scripts to set the environment.

在交叉编译时，一些平台可能需要设置环境变量，或者提供脚本来设置环境。

Visual Studio ships multiple command prompts and `vcvarsall.bat` scripts for setting up the correct environments for command line buildsystems. While not strictly necessary to use a corresponding command line environment when using a Visual Studio generator, doing so has no disadvantages.

Visual Studio 提供了多个命令提示和 `vcvarsall.bat` 脚本，用于为命令行构建系统设置正确的环境。虽然在使用 Visual Studio 生成器时，严格来说没有必要使用相应的命令行环境，但这样做并没有什么坏处。

When using Xcode, there can be more than one Xcode version installed. Which one to use can be selected in a number of different ways, but the most common methods are:

当使用 Xcode 时，可以安装一个以上的 Xcode 版本。使用哪一个可以通过一些不同的方式来选择，但最常见的方法是。

+ Setting the default version in the preferences of the Xcode IDE. (在 Xcode IDE 的首选项中设置默认版本)
+ Setting the default version via the `xcode-select` command line tool.

+ Overriding the default version by setting the `DEVELOPER_DIR` environment variable when running CMake and the build tool. (在运行 CMake 和构建工具时，通过设置 `DEVELOPER_DIR` 环境变量覆盖默认版本。)

For convenience, `cmake-gui(1)` provides an environment variable editor.

为了方便，`cmake-gui(1)` 提供了一个环境变量编辑器。

### Command line -G option
CMake chooses a generator by default based on the platform. Usually, the default generator is sufficient to allow the user to proceed to build the software.

CMake 默认会根据平台选择一个生成器。通常情况下，默认的生成器足以让用户继续构建软件。

The user may override the default generator with the -G option:

用户可以用 -G 选项覆盖默认的发生器。

```bash
$  cmake .. -G Ninja
```

The output of cmake --help includes a list of generators available for the user to choose from. Note that generator names are case sensitive.

cmake --help 的输出包括一个可供用户选择的生成器列表。注意，生成器的名字是区分大小写的。

On Unix-like systems (including Mac OS X), the Unix Makefiles generator is used by default. A variant of that generator can also be used on Windows in various environments, such as the NMake Makefiles and MinGW Makefiles generator. These generators generate a Makefile variant which can be executed with make, gmake, nmake or similar tools. See the individual generator documentation for more information on targeted environments and tools.

在类 Unix 系统（包括 Mac OS X）上，默认使用 Unix Makefiles 生成器。该生成器的一个变体也可以在 Windows 的各种环境中使用，如 NMake Makefiles 和 MinGW Makefiles 生成器。这些生成器生成的 Makefile 变体可以用 make、gmake、nmake 或类似工具执行。有关目标环境和工具的更多信息，请参见各个生成器的文档。

The `Ninja` generator is available on all major platforms. `ninja` is a build tool similar in use-cases to `make`, but with a focus on performance and efficiency.

"Ninja" 生成器在所有主要平台上都可用。ninja 是一个构建工具，其使用情况与 make 相似，但更聚焦于性能和效率。

On Windows, cmake(1) can be used to generate solutions for the Visual Studio IDE. Visual Studio versions may be specified by the product name of the IDE, which includes a four-digit year. Aliases are provided for other means by which Visual Studio versions are sometimes referred to, such as two digits which correspond to the product version of the VisualC++ compiler, or a combination of the two:

在 Windows 上，cmake(1) 可以用来为 Visual Studio IDE 生成解决方案。Visual Studio 版本可以通过 IDE 的产品名称来指定，其中包括一个四位数的年份。为 Visual Studio 版本的其他方式提供了别名，例如与 VisualC++ 编译器的产品版本相对应的两个数字，或两者的组合。

```bash
cmake .. -G "Visual Studio 2019"
cmake .. -G "Visual Studio 16"
cmake .. -G "Visual Studio 16 2019"
```

Visual Studio generators can target different architectures. One can specify the target architecture using the -A option:

Visual Studio 生成器可以针对不同的架构。人们可以使用 -A 选项指定目标架构。

```
cmake .. -G "Visual Studio 2019" -A x64
cmake .. -G "Visual Studio 16" -A ARM
cmake .. -G "Visual Studio 16 2019" -A ARM64
```

On Apple, the Xcode generator may be used to generate project files for the Xcode IDE.

在苹果上，Xcode 生成器可以用来为 Xcode IDE 生成项目文件。

Some IDEs such as KDevelop4, QtCreator and CLion have native support for CMake-based buildsystems. Those IDEs provide user interface for selecting an underlying generator to use, typically a choice between a Makefile or a Ninja based generator.

一些 IDE，如 KDevelop4、QtCreator 和 CLion，对基于 CMake 的构建系统有本地支持。这些 IDE 提供了选择使用底层生成器的用户界面，通常可以在 Makefile 或 Ninja 生成器之间进行选择。

Note that it is not possible to change the generator with -G after the first invocation of CMake. To change the generator, the build directory must be deleted and the build must be started from scratch.

注意，在第一次调用 CMake 之后，不可能用 -G 来改变生成器。要改变生成器，必须删除构建目录，并且必须从头开始构建。

When generating Visual Studio project and solutions files several other options are available to use when initially running cmake(1).

在生成 Visual Studio 项目和解决方案文件时，在最初运行 cmake(1) 时有几个其他选项可以使用。

The Visual Studio toolset can be specified with the -T option:

Visual Studio 工具集可以用 -T 选项来指定。

```bash
# Build with the clang-cl toolset
cmake.exe .. -G "Visual Studio 16 2019" -A x64 -T ClangCL
# Build targeting Windows XP
cmake.exe .. -G "Visual Studio 16 2019" -A x64 -T v120_xp
```

Whereas the -A option specifies the _target_ architecture, the `-T` option can be used to specify details of the toolchain used. For example, -Thost=x64 can be given to select the 64-bit version of the host tools. The following demonstrates how to use 64-bit tools and also build for a 64-bit target architecture:

而 -A 选项指定了 _target，-T选项可以用来指定所使用的工具链的细节。例如，可以给出-Thost=x64来选择64位的主机工具。下面演示了如何使用64位工具，同时为64位目标架构进行构建。

```bash
cmake .. -G "Visual Studio 16 2019" -A x64 -Thost=x64
```

### Choosing a generator in cmake-gui
[请看此处](https://cmake.org/cmake/help/latest/guide/user-interaction/index.html)

## Setting Build Variables
Software projects often require variables to be set on the command line when invoking CMake. Some of the most commonly used CMake variables are listed in the table below:

软件项目在调用 CMake 时经常需要在命令行上设置一些变量。一些最常用的 CMake 变量被列在下面的表格中。

Variable          | Meaning
----------------- | ----------
CMAKE_PREFIX_PATH | Path to search for dependent packages
CMAKE_MODULE_PATH | Path to search for additional CMake modules
CMAKE_BUILD_TYPE  | Build configuration, such as Debug or Release, determining debug/optimization flags. This is only relevant for single-configuration buildsystems such as Makefile and Ninja. Multi-configuration buildsystems such as those for Visual Studio and Xcode ignore this setting.
CMAKE_INSTALL_PREFIX | Location to install the software to with the install build target
CMAKE_TOOLCHAIN_FILE | File containing cross-compiling data such as toolchains and sysroots.
BUILD_SHARED_LIBS |  Whether to build shared instead of static libraries for add_library() commands used without a type
CMAKE_EXPORT_COMPILE_COMMANDS | Generate a compile_commands.json file for use with clang-based tools

Other project-specific variables may be available to control builds, such as enabling or disabling components of the project.

其他项目特定的变量可以用来控制构建，例如启用或禁用项目的组件。

There is no convention provided by CMake for how such variables are named between different provided buildsystems, except that variables with the prefix `CMAKE_` usually refer to options provided by CMake itself and should not be used in third-party options, which should use their own prefix instead. The `cmake-gui(1)` tool can display options in groups defined by their prefix, so it makes sense for third parties to ensure that they use a self-consistent prefix.

CMake 并没有提供关于这些变量在不同的构建系统之间如何命名的约定，只是带有前缀 `CMAKE_` 的变量通常指的是 CMake 本身提供的选项，不应该用于第三方选项，而应该使用它们自己的前缀。`cmake-gui(1)` 工具可以将选项按其前缀定义的组来显示，所以第三方确保他们使用一个自洽的前缀是有意义的。

### Setting variables on the command line
CMake variables can be set on the command line either when creating the initial build:

CMake 变量可以在创建初始构建时在命令行上设置。

```bash
mkdir build
cd build
cmake .. -G Ninja -DCMAKE_BUILD_TYPE=Debug
```

or later on a subsequent invocation of cmake(1):

```bash
cd build
cmake . -DCMAKE_BUILD_TYPE=Debug
```

The -U flag may be used to unset variables on the cmake(1) command line:
```bash 
cd build
cmake . -UMyPackage_DIR
```

A CMake buildsystem which was initially created on the command line can be modified using the `cmake-gui(1)` and vice-versa.

最初在命令行上创建的 CMake 构建系统可以使用 `cmake-gui(1)` 进行修改，反之亦然。

The `cmake(1)` tool allows specifying a file to use to populate the initial cache using the -C option. This can be useful to simplify commands and scripts which repeatedly require the same cache entries.

`cmake(1)` 工具允许使用 -C 选项来指定一个文件来填充初始缓存。这对于简化重复需要相同缓存条目的命令和脚本很有用。

### Setting variables with cmake-gui
[略]

### The CMake Cache
When CMake is executed, it needs to find the locations of compilers, tools and dependencies. It also needs to be able to consistently re-generate a buildsystem to use the same compile/link flags and paths to dependencies. Such parameters are also required to be configurable by the user because they are paths and options specific to the users system.

当 CMake 被执行时，它需要找到编译器、工具和依赖项的位置。它还需要能够一致地重新生成一个构建系统，以使用相同的编译/链接标志和依赖关系的路径。这些参数也需要由用户来配置，因为它们是用户系统的特定路径和选项。

When it is first executed, CMake generates a `CMakeCache.txt` file in the build directory containing key-value pairs for such artifacts. The cache file can be viewed or edited by the user by running the `cmake-gui(1)` or `ccmake(1)` tool. The tools provide an interactive interface for re-configuring the provided software and re-generating the buildsystem, as is needed after editing cached values. Each cache entry may have an associated short help text which is displayed in the user interface tools.

当它第一次被执行时，CMake 会在构建目录下生成一个 `CMakeCache.txt` 文件，其中包含此类工件的键值对。用户可以通过运行 `cmake-gui(1)` 或 `ccmake(1)` 工具查看或编辑该缓存文件。这些工具提供了一个交互式界面，用于重新配置所提供的软件和重新生成构建系统，这是编辑缓存值后的需要。每个缓存条目都可以有一个相关的简短帮助文本，显示在用户界面工具中。

The cache entries may also have a type to signify how it should be presented in the user interface. For example, a cache entry of type `BOOL` can be edited by a checkbox in a user interface, a STRING can be edited in a text field, and a `FILEPATH` while similar to a `STRING` should also provide a way to locate filesystem paths using a file dialog. An entry of type `STRING` may provide a restricted list of allowed values which are then provided in a drop-down menu in the `cmake-gui(1)` user interface (see the `STRINGS` cache property).

缓存条目也可以有一个类型来表示它应该如何在用户界面上呈现。例如，"BOOL" 类型的缓存条目可以在用户界面中通过复选框进行编辑，"STRING" 可以在文本字段中进行编辑，而 "FILEPATH" 虽然与 "STRING" 类似，但也应该提供一种使用文件对话框定位文件系统路径的方法。`STRING` 类型的条目可以提供一个允许值的限制性列表，然后在 `cmake-gui(1)` 用户界面的下拉菜单中提供（见 `STRINGS` 缓存属性）。

The CMake files shipped with a software package may also define boolean toggle options using the `option()` command. The command creates a cache entry which has a help text and a default value. Such cache entries are typically specific to the provided software and affect the configuration of the build, such as whether tests and examples are built, whether to build with exceptions enabled etc.

软件包中的 CMake 文件也可以使用 `option()` 命令来定义布尔式切换选项。该命令创建一个缓存条目，其中有帮助文本和默认值。这样的缓存条目通常是针对所提供的软件的，并影响构建的配置，例如是否构建测试和示例，是否启用异常构建等。

## Presets
CMake understands a file, `CMakePresets.json`, and its user-specific counterpart, `CMakeUserPresets.json`, for saving presets for commonly-used configure settings. These presets can set the build directory, generator, cache variables, environment variables, and other command-line options. All of these options can be overridden by the user. The full details of the `CMakePresets.json` format are listed in the `cmake-presets(7)` manual.

CMake 理解一个文件，`CMakePresets.json`，以及它的特定用户对应文件，`CMakeUserPresets.json`，用于保存常用配置的预置。这些预设可以设置构建目录、生成器、缓存变量、环境变量和其他命令行选项。所有这些选项都可以被用户覆盖。`CMakePresets.json` 格式的全部细节在 `cmake-presets(7)` 手册中列出。

### Using presets on the command-line
When using the cmake(1) command line tool, a preset can be invoked by using the --preset option. If --preset is specified, the generator and build directory are not required, but can be specified to override them. For example, if you have the following CMakePresets.json file:

当使用 cmake(1) 命令行工具时， 可以通过使用 --preset 选项来调用一个预设。如果指定了 --preset，则不需要生成器和构建目录，但可以指定它们来覆盖。例如，如果你有以下 CMakePresets.json文件。

```json
{
  "version": 1,
  "configurePresets": [
    {
      "name": "ninja-release",
      "binaryDir": "${sourceDir}/build/${presetName}",
      "generator": "Ninja",
      "cacheVariables": {
        "CMAKE_BUILD_TYPE": "Release"
      }
    }
  ]
}
```

and you run the following:

```
cmake -S /path/to/source --preset=ninja-release
```

This will generate a build directory in `/path/to/source/build/ninja-release` with the `Ninja` generator, and with `CMAKE_BUILD_TYPE` set to `Release`.

这将在 `/path/to/source/build/ninja-release` 中生成一个使用 `Ninja` 生成器的构建目录，并将 `CMAKE_BUILD_TYPE` 设置为 `Release`。

If you want to see the list of available presets, you can run:

```bash
cmake -S /path/to/source --list-presets
```

This will list the presets available in `/path/to/source/CMakePresets.json` and `/path/to/source/CMakeUsersPresets.json` without generating a build tree.

### Using presets in cmake-gui
If a project has presets available, either through `CMakePresets.json` or `CMakeUserPresets.json`, the list of presets will appear in a drop-down menu in `cmake-gui(1)` between the source directory and the binary directory. Choosing a preset sets the binary directory, generator, environment variables, and cache variables, but all of these options can be overridden after a preset is selected.

如果一个项目有可用的预设，无论是通过 `CMakePresets.json` 还是 `CMakeUserPresets.json`，预设列表将出现在源目录和二进制目录之间的 `cmake-gui(1)` 下拉菜单里。选择一个预设会设置二进制目录、生成器、环境变量和缓存变量，但所有这些选项都可以在选择预设后被覆盖。

## Invoking the Buildsystem
After generating the buildsystem, the software can be built by invoking the particular build tool. In the case of the IDE generators, this can involve loading the generated project file into the IDE to invoke the build.

生成构建系统后，可以通过调用特定的构建工具来构建软件。在 IDE 生成器的情况下，这可能涉及到将生成的项目文件加载到 IDE 来调用构建。

CMake is aware of the specific build tool needed to invoke a build so in general, to build a buildsystem or project from the command line after generating, the following command may be invoked in the build directory:

CMake 知道调用构建工具所需的特定构建工具，所以一般来说，要在生成后从命令行构建一个构建系统或项目，可以在构建目录下调用以下命令。

```bash
cmake --build .
```

The `--build` flag enables a particular mode of operation for the `cmake(1)` tool. It invokes the `CMAKE_MAKE_PROGRAM` command associated with the `generator`, or the build tool configured by the user.

`--build` 标志使 `cmake(1)` 工具有一个特定的操作模式。它调用与 "生成器" 相关的 "CMAKE_MAKE_PROGRAM" 命令，或由用户配置的构建工具。

The `--build` mode also accepts the parameter `--target` to specify a particular target to build, for example a particular library, executable or custom target, or a particular special target like `install`:

`--build` 模式也接受参数 `--target` 来指定一个特定的构建目标，例如一个特定的库、可执行文件或自定义目标，或者一个特殊的目标，如 "安装"。

```bash
cmake --build . --target myexe
```

The `--build` mode also accepts a `--config` parameter in the case of multi-config generators to specify which particular configuration to build:

在多配置生成器的情况下，`--build` 模式也接受一个 `--config` 参数，以指定要构建的特定配置。

```bash
cmake --build . --target myexe --config Release
```

The `--config` option has no effect if the generator generates a buildsystem specific to a configuration which is chosen when invoking cmake with the `CMAKE_BUILD_TYPE` variable.

如果生成器生成的构建系统是在使用 `CMAKE_BUILD_TYPE` 变量调用 cmake 时选择的特定配置， 则 `--config` 选项没有作用。

Some buildsystems omit details of command lines invoked during the build. The `--verbose` flag can be used to cause those command lines to be shown:

一些构建系统省略了构建过程中调用的命令行的细节。可以使用 `--verbose` 标志来显示这些命令行。

```bash
cmake --build . --target myexe --verbose
```

The `--build` mode can also pass particular command line options to the underlying build tool by listing them after `--`. This can be useful to specify options to the build tool, such as to continue the build after a failed job, where CMake does not provide a high-level user interface.

`--build` 模式也可以通过在`--` 后面列出特定的命令行选项来传递给底层构建工具。这对指定构建工具的选项很有用，例如在 CMake 不提供高级用户界面的情况下，在工作失败后继续构建。

For all generators, it is possible to run the underlying build tool after invoking CMake. For example, `make` may be executed after generating with the `Unix Makefiles` generator to invoke the build, or `ninja` after generating with the `Ninja` generator etc. The IDE buildsystems usually provide command line tooling for building a project which can also be invoked.

对于所有的生成器，都可以在调用 CMake 后运行底层构建工具。例如，在使用 "Unix Makefiles" 生成器生成后，可以执行 "make" 来调用构建，或者在使用 "Ninja" 生成器生成后执行 "ninja" 等。集成开发环境的构建系统通常提供命令行工具来构建项目，这些工具也可以被调用。

### Selecting a Target
Each executable and library described in the CMake files is a build target, and the buildsystem may describe custom targets, either for internal use, or for user consumption, for example to create documentation.

CMake 文件中描述的每个可执行文件和库都是一个构建目标，构建系统可以描述自定义的目标，或者供内部使用，或者供用户使用，例如用于创建文档。

CMake provides some built-in targets for all buildsystems providing CMake files.

CMake 为所有提供 CMake 文件的构建系统提供了一些内置目标。

**all**
The default target used by `Makefile` and `Ninja` generators. Builds all targets in the buildsystem, except those which are excluded by their `EXCLUDE_FROM_ALL` target property or `EXCLUDE_FROM_ALL` directory property. The name ALL_BUILD is used for this purpose for the Xcode and Visual Studio generators.

由 `Makefile` 和 `Ninja` 生成器使用的默认目标。构建系统中的所有目标，除了那些被其`EXCLUDE_FROM_ALL` 目标属性或 `EXCLUDE_FROM_ALL` 目录属性排除的目标。在 Xcode 和 Visual Studio 生成器中，ALL_BUILD 这个名字就是用于此目的。

**help**
Lists the targets available for build. This target is available when using the `Unix Makefiles` or `Ninja` generator, and the exact output is tool-specific.

列出可用于构建的目标。这个目标在使用 "Unix Makefiles"或 "Ninja" 生成器时是可用的，确切的输出是由工具决定的。

**clean**
Delete built object files and other output files. The `Makefile` based generators create a `clean` target per directory, so that an individual directory can be cleaned. The `Ninja` tool provides its own granular` -t clean` system.

删除构建的对象文件和其他输出文件。基于 "Makefile" 的生成器为每个目录创建一个 "clean" 目标，这样就可以对单个目录进行清理。Ninja 工具提供了它自己的细化 "-t clean" 系统。

**test**
Runs tests. This target is only automatically available if the CMake files provide CTest-based tests. See also Running Tests.

运行测试。这个目标只有在 CMake 文件提供基于 CTest 的测试时才会自动出现。请参见运行测试。

**install**
Installs the software. This target is only automatically available if the software defines install rules with the `install()` command. See also Software Installation.

安装该软件。这个目标只有在软件用 `install()` 命令定义了安装规则时才会自动可用。请参见软件安装。

**package**
Creates a binary package. This target is only automatically available if the CMake files provide CPack-based packages.

创建一个二进制包。这个目标只有在 CMake 文件提供基于 CPack 的包时才会自动出现。

**package_source**
Creates a source package. This target is only automatically available if the CMake files provide CPack-based packages.

创建一个源码包。这个目标只有在 CMake 文件提供基于 CPack 的包时才会自动出现。

For `Makefile` based systems, `/fast` variants of binary build targets are provided. The `/fast` variants are used to build the specified target without regard for its dependencies. The dependencies are not checked and are not rebuilt if out of date. The `Ninja` generator is sufficiently fast at dependency checking that such targets are not provided for that generator.

对于基于 `Makefile` 的系统，提供了二进制构建目标的 `/fast` 变体。`/fast` 变体被用来构建指定的目标，而不考虑其依赖关系。不检查依赖关系，如果过期也不重建。Ninja 生成器在依赖性检查方面足够快，所以没有为该生成器提供这样的目标。

`Makefile` based systems also provide build-targets to preprocess, assemble and compile individual files in a particular directory.

基于 Makefile 的系统也提供了构建目标，以预处理、组装和编译特定目录下的单个文件。

```bash
make foo.cpp.i
make foo.cpp.s
make foo.cpp.o
```

The file extension is built into the name of the target because another file with the same name but a different extension may exist. However, build-targets without the file extension are also provided.

文件扩展名被内置到目标的名称中，因为可能存在另一个具有相同名称但不同扩展名的文件。然而，我们也提供没有文件扩展名的构建目标。

```bash
make foo.i
make foo.s
make foo.o
```

In buildsystems which contain `foo.c` and `foo.cpp`, building the `foo.i` target will preprocess both files.

在包含 `foo.c` 和 `foo.cpp` 的构建系统中，构建 `foo.i` 目标将预处理这两个文件。

### Specifying a Build Program
The program invoked by the --build mode is determined by the CMAKE_MAKE_PROGRAM variable. For most generators, the particular program does not need to be configured.

由 -build 模式调用的程序是由 CMAKE_MAKE_PROGRAM 变量决定的。对于大多数生成器来说，不需要配置特定的程序。


Generator   | Default make program  | Alternatives
----------- | --------------------- | ------------
XCode       | xcodebuild            | -
Unix        | make                  | -
NMake Makefiles JOM | jom           | nmake
MinGW Makefiles | mingw32-make      | -
MSYS Makefiles | make               | -
Ninja       | ninja                 | -
Visual Studio | msbuild             | -
Watcom WMake | wmake                | -

The jom tool is capable of reading makefiles of the NMake flavor and building in parallel, while the nmake tool always builds serially. After generating with the NMake Makefiles generator a user can run jom instead of nmake. The --build mode would also use jom if the CMAKE_MAKE_PROGRAM was set to jom while using the NMake Makefiles generator, and as a convenience, the NMake Makefiles JOM generator is provided to find jom in the normal way and use it as the CMAKE_MAKE_PROGRAM. For completeness, nmake is an alternative tool which can process the output of the NMake Makefiles JOM generator, but doing so would be a pessimisation.

jom 工具能够读取 NMake 风味的 Makefile 并进行并行构建，而 nmake 工具总是以串行方式构建。在用 NMake Makefiles 生成器生成后，用户可以运行 jom 而不是 nmake。如果在使用 NMake Makefiles 生成器时将 CMAKE_MAKE_PROGRAM 设置为 jom，那么 --build 模式也会使用 jom，作为一种方便，提供了 NMake Makefiles JOM 生成器来以正常方式找到 jom 并将其作为 CMAKE_MAKE_PROGRAM。为了完整起见，nmake是一个替代工具，它可以处理 NMake Makefiles JOM 生成器的输出，但这样做将是一种悲观的做法。


## Software Installation
The CMAKE_INSTALL_PREFIX variable can be set in the CMake cache to specify where to install the provided software. If the provided software has install rules, specified using the install() command, they will install artifacts into that prefix. On Windows, the default installation location corresponds to the ProgramFiles system directory which may be architecture specific. On Unix hosts, /usr/local is the default installation location.

CMAKE_INSTALL_PREFIX 变量可以在 CMake 缓存中设置，以指定所提供软件的安装位置。如果提供的软件有安装规则，使用 install() 命令指定，它们将把工件安装到该前缀。在 Windows 上，默认的安装位置对应于 ProgramFiles 系统目录，该目录可能与架构有关。在 Unix 主机上，/usr/local 是默认的安装位置。

The CMAKE_INSTALL_PREFIX variable always refers to the installation prefix on the target filesystem.

CMAKE_INSTALL_PREFIX 变量总是指目标文件系统上的安装前缀。

In cross-compiling or packaging scenarios where the sysroot is read-only or where the sysroot should otherwise remain pristine, the CMAKE_STAGING_PREFIX variable can be set to a location to actually install the files.

在交叉编译或打包的情况下，如果系统根是只读的，或者系统根应该保持原始状态，CMAKE_STAGING_PREFIX 变量可以被设置为一个实际安装文件的位置。

The commands:

```bash
cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DCMAKE_SYSROOT=$HOME/root \
  -DCMAKE_STAGING_PREFIX=/tmp/package
cmake --build .
cmake --build . --target install
```

result in files being installed to paths such as /tmp/package/lib/libfoo.so on the host machine. The /usr/local location on the host machine is not affected.

导致文件被安装到主机上的 /tmp/package/lib/libfoo.so 等路径。主机上的 /usr/local 位置不受影响。

Some provided software may specify uninstall rules, but CMake does not generate such rules by default itself.

一些提供的软件可能会指定卸载规则，但 CMake 本身并不默认生成这种规则。

## Running Tests
The ctest(1) tool is shipped with the CMake distribution to execute provided tests and report results. The test build-target is provided to run all available tests, but the ctest(1) tool allows granular control over which tests to run, how to run them, and how to report results. Executing ctest(1) in the build directory is equivalent to running the test target:

ctest(1) 工具随 CMake 发行版一起提供，用于执行提供的测试并报告结果。提供的测试构建目标可以运行所有可用的测试，但 ctest(1) 工具允许对运行哪些测试、如何运行它们以及如何报告结果进行细化控制。在构建目录中执行 ctest(1) 相当于运行测试目标。

```
ctest
```

A regular expression can be passed to run only tests which match the expression. To run only tests with Qt in their name:

可以通过一个正则表达式，只运行与表达式相匹配的测试。只运行名称中含有 Qt 的测试。

```
ctest -R Qt
```

Tests can be excluded by regular expression too. To run only tests without Qt in their name:

测试也可以通过正则表达式排除。只运行名称中没有 Qt 的测试。

```
ctest -E Qt
```

Tests can be run in parallel by passing -j arguments to ctest(1):

通过向 ctest(1) 传递 -j 参数，测试可以被并行运行。

```
ctest -R Qt -j8
```

The environment variable `CTEST_PARALLEL_LEVEL` can alternatively be set to avoid the need to pass `-j`.

环境变量 `CTEST_PARALLEL_LEVEL` 可以被设置以避免传递`-j`。

By default `ctest(1)` does not print the output from the tests. The command line argument `-V` (or `--verbose`) enables verbose mode to print the output from all tests. The `--output-on-failure` option prints the test output for failing tests only. The environment variable `CTEST_OUTPUT_ON_FAILURE` can be set to `1` as an alternative to passing the `--output-on-failure` option to `ctest(1)`.

默认情况下，`ctest(1)` 不打印测试的输出。命令行参数`-V`（或`--verbose`）启用 verbose 模式来打印所有测试的输出。`--output-on-failure` 仅在失败的时候输出测试。环境变量` CTEST_OUTPUT_ON_FAILURE` 可以被设置为 `1`，以替代向 `ctest(1)` 传递`--output-on-failure` 选项。
