# CMake Tutorial

## Introduction
The CMake tutorial provides a step-by-step guide that covers common build system issues that CMake helps address. Seeing how various topics all work together in an example project can be very helpful.

CMake 教程提供了一个分步指南，涵盖 CMake 帮助解决的常见构建系统问题。在一个示例项目中，了解各种主题是如何协同工作的，这会非常有帮助。

## Steps
The tutorial documentation and source code for examples can be found in the Help/guide/tutorial directory of the CMake source code tree. Each step has its own subdirectory containing code that may be used as a starting point. The tutorial examples are progressive so that each step provides the complete solution for the previous step.

示例的教程文档和源代码可以在 CMake 源代码树的 Help/guide/tutorial 目录中找到。每个步骤都有自己的子目录，其中包含可以用作起点的代码。本教程的示例是渐进式的，因此每一步都提供了前一步的完整解决方案。

[TOC]

## Step 1: A basic Starting Point
> The most basic project is an executable built from source code files. For simple projects, a three line `CMakeLists.txt` file is all that is required. This will be the starting point for our tutorial. Create a `CMakeLists.txt` file in the `Step1` directory that looks like:

最基本的项目是从源代码文件构建的可执行文件。对于简单的项目，可以使用三行 CMakeLists.txt 文件是所有需要的。这将是我们教程的起点。在 Step1 目录中创建一个 CMakeLists.txt 文件，如下所示：

```makefile
#!CmakeLists.txt

cmake_minimum_required(VERSION 3.10)

# set the project name
project(Tutorial)

# add the executable
add_executable(Tutorial tutorial.cxx)
```

> Note that this example uses lower case commands in the `CMakeLists.txt` file. Upper, lower, and mixed case commands are supported by CMake. The source code for `tutorial.cxx` is provided in the Step1 directory and can be used to compute the square root of a number.

请注意，本例在 CMakeLists.txt 文件中使用小写命令。CMake 支持大小写混合命令。教程的 tutorial.cxx 在 Step1 目录中提供，可用于计算数字的平方根。

### Build and Run
> That's all that is needed - we can build and run our project now! First, run the `cmake` executable or the `cmake-gui` to configure the project and then build it with your chosen build tool.

这就是我们所需要的——我们现在就可以构建和运行我们的项目了！首先，运行 “cmake” 可执行文件或“cmake gui” 来配置项目，然后使用您选择的构建工具构建它。

> For example, from the command line we could navigate to the `Help/guide/tutorial` directory of the CMake source code tree and create a build directory:

例如，我们可以从命令行导航到 CMake 源代码树的 “Help/guide/tutorial” 目录，并创建一个构建目录：

```bash
mkdir Step1_build
```

Next, navigate to the build directory and run CMake to configure the project and generate a native build system:

接下来，导航到 build 目录并运行 CMake 来配置项目并生成本机构建系统：

```bash
cd Step1_build
cmake ../Step1
```

> Then call that build system to actually compile/link the project:

然后调用构建系统来实际编译/链接项目：

```makefile
cmake --build .
```

> Finally, try to use the newly built `Tutorial` with these commands:

最后，尝试将新构建的 “Tutorial” 与以下命令结合使用：

```bash
Tutorial 4294967296
Tutorial 10
Tutorial
```

### Adding a Version Number and Configured Header File
> The first feature we will add is to provide our executable and project with a version number. While we could do this exclusively in the source code, using `CMakeLists.txt` provides more flexibility.

我们将添加的第一个功能是为我们的可执行文件和项目提供一个版本号。虽然我们可以在源代码中这么做，但是使用“CMakeLists.txt” 提供了更大的灵活性。

> First, modify the `CMakeLists.txt` file to use the `project()` command to set the project name and version number.

首先，使用 `project()` 命令来修改 CMakeLists.txt 文件设置项目名称和版本号。

```makefile
#!CMakeLists.txt

cmake_minimum_required(VERSION 3.10)

# set the project name and version
project(Tutorial VERSION 1.0)
```

> Then, configure a header file to pass the version number to the source code:

然后，配置头文件以将版本号传递给源代码：

```makefile
#!CMakeLists.txt

configure_file(TutorialConfig.h.in TutorialConfig.h)
```

> Since the configured file will be written into the binary tree, we must add that directory to the list of paths to search for include files. Add the following lines to the end of the `CMakeLists.txt` file:

由于配置的文件将写入二进制树，因此我们必须将该目录添加到搜索包含文件的路径列表中。在“CMakeLists.txt” 文件的末尾添加以下行：

```makefile
#!CMakeLists.txt

target_include_directories(Tutorial PUBLIC
                           "${PROJECT_BINARY_DIR}"
                           )
```

Using your favorite editor, create `TutorialConfig.h.in` in the source directory with the following contents:

```makefile
#!TutorialConfig.h.in

// the configured options and settings for Tutorial
#define Tutorial_VERSION_MAJOR @Tutorial_VERSION_MAJOR@
#define Tutorial_VERSION_MINOR @Tutorial_VERSION_MINOR@
```

When CMake configures this header file the values for `@Tutorial_VERSION_MAJOR@` and `@Tutorial_VERSION_MINOR@` will be replaced.

Next modify `tutorial.cxx` to include the configured header file, `TutorialConfig.h`.

Finally, let's print out the executable name and version number by updating `tutorial.cxx` as follows:

```makefile
#!tutorial.cxx

    if (argc < 2) {
        // report version
        std::cout << argv[0] << " Version " << Tutorial_VERSION_MAJOR << "." << Tutorial_VERSION_MINOR << std::endl;
        std::cout << "Usage: " << argv[0] << " number" << std::endl;
        return 1;
    }
```

### Specify the C++ Standard
Next let's add some C++11 features to our project by replacing `atof` with `std::stod` in `tutorial.cxx`. At the same time, remove `#include <cstdlib>`.

```makefile
#!tutorial.cxx

const double inputValue = std::stod(argv[1]);
```

We will need to explicitly state in the CMake code that it should use the correct flags. The easiest way to enable support for a specific C++ standard in CMake is by using the `CMAKE_CXX_STANDARD` variable. For this tutorial, set the `CMAKE_CXX_STANDARD` variable in the `CMakeLists.txt` file to `11` and `CMAKE_CXX_STANDARD_REQUIRED` to True. Make sure to add the `CMAKE_CXX_STANDARD` declarations above the call to `add_executable`.

我们需要在 CMake 代码中明确声明它应该使用正确的标志。在 CMake 中支持特定 C++ 标准的最简单方法是使用 `CMAKE_CXX_STANDARD` 变量。对于本教程，请在 “CmakeList.txt” 文件中 `CMAKE_CXX_STANDARD` 变量设置为 `11` 和 `CMAKE_CXX_STANDARD_REQUIRED` 设置为 True。确保在调用 “add_executable” 之前添加 “CMAKE_CXX_STANDARD” 声明。

```makefile
#!CMakeLists.txt

cmake_minimum_required(VERSION 3.10)

# set the project name and version
project(Tutorial VERSION 1.0)

# specify the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)
```

### Rebuild
Let's build our project again. We already created a build directory and ran CMake, so we can skip to the build step:

```makefile
cd Step1_build
cmake --build .
```

Now we can try to use the newly built Tutorial with same commands as before:

```makefile
Tutorial 4294967296
Tutorial 10
Tutorial
```

Check that the version number is now reported when running the executable without any arguments.

检查在没有任何参数的情况下运行可执行文件时是否报告了版本号。

## Step 2: Adding a Library
> Now we will add a library to our project. This library will contain our own implementation for computing the square root of a number. The executable can then use this library instead of the standard square root function provided by the compiler.

现在我们将向我们的项目中添加一个库。这个库将包含我们自己计算数字平方根的实现。然后，可执行文件可以使用这个库，而不是编译器提供的标准平方根函数。

> For this tutorial we will put the library into a subdirectory called `MathFunctions`. This directory already contains a header file, `MathFunctions.h`, and a source file `mysqrt.cxx`. The source file has one function called `mysqrt` that provides similar functionality to the compiler's `sqrt` function.

在本教程中，我们将把库放在名为 MathFunctions 的子目录中。此目录已包含头文件 MathFunctions.h、 还有一个源文件 mysqrt.cxx。该源文件有一个名为 mysqrt 的函数，它提供了与编译器的 sqrt 函数类似的功能。

> Add the following one line CMakeLists.txt file to the MathFunctions directory:

在 MathFunctions 目录的 CMakeLists.txt 文件添加以下一行: 

```makefile
#!MathFuncitions/CMakeLists.txt

add_library(MathFunctions mysqrt.cxx)
```

> To make use of the new library we will add an` add_subdirectory()` call in the top-level `CMakeLists.txt` file so that the library will get built. We add the new library to the executable, and add `MathFunctions` as an include directory so that the `mysqrt.h` header file can be found. The last few lines of the top-level `CMakeLists.txt` file should now look like:

为了利用新的库，我们将在顶级的 `CMakeLists.txt` 文件中添加一个 `add_subdirectory()` 调用, 以便构建库。我们将新的库添加到可执行文件中，并将 `MathFunctions` 添加为 include 目录，以便找到 `mysqrt.h` 头文件。顶级的 `CMakeLists。txt` 文件的最后几行现在应该如下所示：

```makefile
#!CMakeLists.txt

# add the MathFunctions library
add_subdirectory(MathFunctions)

# add the executable
add_executable(Tutorial tutorial.cxx)

target_link_libraries(Tutorial PUBLIC MathFunctions)

# add the binary tree to the search path for include files
# so that we will find TutorialConfig.h
target_include_directories(Tutorial PUBLIC
                          "${PROJECT_BINARY_DIR}"
                          "${PROJECT_SOURCE_DIR}/MathFunctions"
                          )
```

> Now let us make the `MathFunctions` library optional. While for the tutorial there really isn't any need to do so, for larger projects this is a common occurrence. The first step is to add an option to the top-level `CMakeLists.txt` file.

现在让我们把 "MathFunctions" 库作为可选项。虽然对于本教程来说，确实没有必要这样做，但对于大型项目来说，这是一种常见的情况。第一步是在顶层的 `CMakeLists.txt` 文件中添加一个选项。

```makefile
#!CMakeLists.txt

option(USE_MYMATH "Use tutorial provided math implementation" ON)

# configure a header file to pass some of the CMake settings
# to the source code
configure_file(TutorialConfig.h.in TutorialConfig.h)
```

> This option will be displayed in the `cmake-gui` and `ccmake` with a default value of ON that can be changed by the user. This setting will be stored in the cache so that the user does not need to set the value each time they run CMake on a build directory.

这个选项将显示在 cmake-gui 和 ccmake 中，默认值为 ON，用户可以更改。这个设置将被存储在缓存中，这样用户就不需要在每次在构建目录上运行 CMake 时设置这个值。

> The next change is to make building and linking the `MathFunctions` library conditional. To do this, we will create an if statement which checks the value of the option. Inside the `if` block, put the `add_subdirectory()` command from above with some additional list commands to store information needed to link to the library and add the subdirectory as an include directory in the `Tutorial` target. The end of the top-level `CMakeLists.txt` file will now look like the following:

下一个变化是使构建和链接 `MathFunctions` 库成为条件。为了做到这一点，我们将创建一个 if 语句，检查选项的值。在 "if" 块内，将上面的 "add_subdirectory() "命令与一些额外的列表命令放在一起，以存储链接到库所需的信息，并将子目录作为 "Tutorial" 目标中的包含目录。顶层的`CMakeLists.txt` 文件的结尾现在看起来会像下面这样。

```makefile
#!CMakeLists.txt

if(USE_MYMATH)
  add_subdirectory(MathFunctions)
  list(APPEND EXTRA_LIBS MathFunctions)
  list(APPEND EXTRA_INCLUDES "${PROJECT_SOURCE_DIR}/MathFunctions")
endif()

# add the executable
add_executable(Tutorial tutorial.cxx)

target_link_libraries(Tutorial PUBLIC ${EXTRA_LIBS})

# add the binary tree to the search path for include files
# so that we will find TutorialConfig.h
target_include_directories(Tutorial PUBLIC
                           "${PROJECT_BINARY_DIR}"
                           ${EXTRA_INCLUDES}
                           )
```

> Note the use of the variable `EXTRA_LIBS` to collect up any optional libraries to later be linked into the executable. The variable `EXTRA_INCLUDES` is used similarly for optional header files. This is a classic approach when dealing with many optional components, we will cover the modern approach in the next step.

注意使用变量 `EXTRA_LIBS` 来收集任何可选的库，以便以后连接到可执行文件中。变量`EXTRA_INCLUDES` 也同样用于可选头文件。这是处理许多可选组件时的经典方法，我们将在下一步介绍现代方法。

> The corresponding changes to the source code are fairly straightforward. First, in `tutorial.cxx`, include the `MathFunctions.h` header if we need it:

对源代码的相应修改是相当直接的。首先，在 `tutorial.cxx` 中，包括 `MathFunctions.h` 头，如果我们需要它的话。

```makefile
## tutorial.cxx

#ifdef USE_MYMATH
#  include "MathFunctions.h"
#endif
```

> Then, in the same file, make `USE_MYMATH` control which square root function is used:

然后，在同一个文件中，让 `USE_MYMATH` 控制使用哪个平方根函数。

```makefile
#!tutorial.cxx

#ifdef USE_MYMATH
  const double outputValue = mysqrt(inputValue);
#else
  const double outputValue = sqrt(inputValue);
#endif
```

> Since the source code now requires `USE_MYMATH` we can add it to `TutorialConfig.h.in` with the following line:

由于源代码现在需要 "USE_MYMATH"，我们可以用以下一行将其添加到 "TutorialConfig.h.in "中。

```makefile
#!TutorialConfig.h.in

#cmakedefine USE_MYMATH
```

> Exercise: Why is it important that we configure `TutorialConfig.h.in` after the option for `USE_MYMATH`? What would happen if we inverted the two?

练习。为什么我们要把 `TutorialConfig.h.in` 配置在 `USE_MYMATH` 的选项之后？如果我们将两者颠倒过来，会发生什么？

> Run the `cmake` executable or the `cmake-gui` to configure the project and then build it with your chosen build tool. Then run the built Tutorial executable.

运行 `cmake` 可执行文件或 `cmake-gui` 来配置项目，然后用你选择的构建工具进行构建。然后运行构建好的教程可执行文件。

> Now let's update the value of USE_MYMATH. The easiest way is to use the `cmake-gui` or `ccmake` if you're in the terminal. Or, alternatively, if you want to change the option from the command-line, try:

现在我们来更新 USE_MYMATH 的值。最简单的方法是使用 `cmake-gui` 或 `ccmake` 如果你是在终端。或者，如果你想在命令行中改变这个选项，可以试试。

```makefile
cmake ../Step2 -DUSE_MYMATH=OFF
```

Rebuild and run the tutorial again.

Which function gives better results, `sqrt` or `mysqrt`?

## Step 3: Adding Usage Requirements for a Library
> Usage requirements allow for far better control over a library or executable's link and include line while also giving more control over the transitive property of targets inside CMake. The primary commands that leverage usage requirements are:

使用要求允许更好地控制一个库或可执行文件的链接和包含行，同时也能对 CMake 中目标的传递属性进行更多控制。利用使用要求的主要命令是。

+ target_compile_definitions()
+ target_compile_options()
+ target_include_directories()
+ target_link_libraries()

> Let's refactor our code from Adding a Library to use the modern CMake approach of usage requirements. We first state that anybody linking to MathFunctions needs to include the current source directory, while MathFunctions itself doesn't. So this can become an INTERFACE usage requirement.

让我们重构我们的代码，从添加一个库到使用现代 CMake 的使用要求的方法。我们首先说明，任何链接到 MathFunctions 的人都需要包括当前的源目录，而 MathFunctions 本身不需要。所以这可以成为一个 INTERFACE 的使用要求。

> Remember INTERFACE means things that consumers require but the producer doesn't. Add the following lines to the end of MathFunctions/CMakeLists.txt:

记住 INTERFACE 是指消费者需要但生产者不需要的东西。在 MathFunctions/CMakeLists.txt 的末尾添加以下几行: 

```Makefile
#! MathFunctions/CMakeLists.txt

target_include_directories(MathFunctions
          INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}
          )
```

> Now that we've specified usage requirements for MathFunctions we can safely remove our uses of the EXTRA_INCLUDES variable from the top-level CMakeLists.txt, here:

现在我们已经指定了 MathFunctions 的使用要求，我们可以安全地从顶级的 CMakeLists.txt 中删除我们对 EXTRA_INCLUDES 变量的使用，这里。

```Makefile
#! CMakeLists.txt

if(USE_MYMATH)
  add_subdirectory(MathFunctions)
  list(APPEND EXTRA_LIBS MathFunctions)
endif()
```

And here:

```Makefile
#! CMakeLists.txt

target_include_directories(Tutorial PUBLIC
                           "${PROJECT_BINARY_DIR}"
                           )
```

> Once this is done, run the `cmake` executable or the `cmake-gui` to configure the project and then build it with your chosen build tool or by using `cmake --build .` from the build directory.

一旦完成，运行 "cmake "可执行文件或 "cmake-gui "来配置项目，然后用你选择的构建工具或从构建目录使用 "cmake --build . "来构建它。

## Step 4: Installing and Testing
Now we can start adding install rules and testing support to our project.

现在我们可以开始向我们的项目添加安装规则和测试支持。

### Install Rules
> The install rules are fairly simple: for `MathFunctions` we want to install the library and header file and for the application we want to install the executable and configured header.

安装规则相当简单：对于 "MathFunctions" 我们要安装库和头文件，对于应用程序我们要安装可执行文件和配置的头文件。

So to the end of `MathFunctions/CMakeLists.txt` we add:

```Makefile
#! MathFunctions/CMakeLists.txt

install(TARGETS MathFunctions DESTINATION lib)
install(FILES MathFunctions.h DESTINATION include)
```

And to the end of the top-level CMakeLists.txt we add:

```Makefile
#! CMakeLists.txt

install(TARGETS Tutorial DESTINATION bin)
install(FILES "${PROJECT_BINARY_DIR}/TutorialConfig.h"
  DESTINATION include
  )
```

That is all that is needed to create a basic local install of the tutorial.

这就是创建一个基本的本地安装教程所需的全部内容。

> Now run the cmake executable or the cmake-gui to configure the project and then build it with your chosen build tool.

现在运行 cmake 可执行文件或 cmake-gui 来配置该项目，然后用你选择的构建工具来构建它。

> Then run the install step by using the `install` option of the `cmake` command (introduced in 3.15, older versions of CMake must use `make install`) from the command line. For multi-configuration tools, don't forget to use the `--config` argument to specify the configuration. If using an IDE, simply build the `INSTALL` target. This step will install the appropriate header files, libraries, and executables. For example:

然后通过在命令行中使用 `cmake` 命令的 `install` 选项（3.15 中引入，旧版本的 CMake 必须使用 `make install` ）来运行安装步骤。对于多配置的工具，别忘了使用 `--config` 参数来指定配置。如果使用 IDE，只需构建 `INSTALL` 目标。这一步将安装适当的头文件、库和可执行文件。比如说。

```bash
cmake --install .
```

> The CMake variable `CMAKE_INSTALL_PREFIX` is used to determine the root of where the files will be installed. If using the `cmake --install` command, the installation prefix can be overridden via the `--prefix` argument. For example:

CMake 变量 `CMAKE_INSTALL_PREFIX` 用于确定文件的安装根目录。如果使用 `cmake --install` 命令，安装前缀可以通过 `--prefix` 参数覆盖。比如说

```bash
cmake --install . --prefix "/home/myuser/installdir"
```

Navigate to the install directory and verify that the installed Tutorial runs.

导航到安装目录，验证已安装的 Tutorial 是否运行。

### Testing Support
> Next let's test our application. At the end of the top-level `CMakeLists.txt` file we can enable testing and then add a number of basic tests to verify that the application is working correctly.

接下来让我们测试一下我们的应用程序。在顶层的 `CMakeLists.txt` 文件的末尾，我们可以启用测试，然后添加一些基本测试来验证应用程序是否正常工作。

```Makefile
#! CMakeLists.txt

enable_testing()

# does the application run
add_test(NAME Runs COMMAND Tutorial 25)

# does the usage message work?
add_test(NAME Usage COMMAND Tutorial)
set_tests_properties(Usage
  PROPERTIES PASS_REGULAR_EXPRESSION "Usage:.*number"
  )

# define a function to simplify adding tests
function(do_test target arg result)
  add_test(NAME Comp${arg} COMMAND ${target} ${arg})
  set_tests_properties(Comp${arg}
    PROPERTIES PASS_REGULAR_EXPRESSION ${result}
    )
endfunction()

# do a bunch of result based tests
do_test(Tutorial 4 "4 is 2")
do_test(Tutorial 9 "9 is 3")
do_test(Tutorial 5 "5 is 2.236")
do_test(Tutorial 7 "7 is 2.645")
do_test(Tutorial 25 "25 is 5")
do_test(Tutorial -25 "-25 is (-nan|nan|0)")
do_test(Tutorial 0.0001 "0.0001 is 0.01")
```

>The first test simply verifies that the application runs, does not segfault or otherwise crash, and has a zero return value. This is the basic form of a CTest test.

第一个测试简单地验证了应用程序的运行，没有发生 segfault 或其他崩溃，并且返回值为零。这就是 CTest 测试的基本形式。

> The next test makes use of the `PASS_REGULAR_EXPRESSION` test property to verify that the output of the test contains certain strings. In this case, verifying that the usage message is printed when an incorrect number of arguments are provided.

下一个测试利用 `PASS_REGULAR_EXPRESSION` 测试属性来验证测试的输出是否包含某些字符串。在这种情况下，验证当提供不正确的参数数量时，是否打印了使用信息。

> Lastly, we have a function called do_test that runs the application and verifies that the computed square root is correct for given input. For each invocation of `do_test`, another test is added to the project with a name, input, and expected results based on the passed arguments.

最后，我们有一个叫做 do_test 的函数，运行应用程序并验证计算的平方根对于给定的输入是否正确。每次调用 `do_test' 时，都会在项目中添加另一个测试，其名称、输入和预期结果都是基于传递的参数。

> Rebuild the application and then cd to the binary directory and run the `ctest` executable: `ctest -N` and `ctest -VV`. For multi-config generators (e.g. Visual Studio), the configuration type must be specified with the` -C <mode>` flag. For example, to run tests in Debug mode use `ctest -C Debug -VV` from the binary directory (not the Debug subdirectory!). Release mode would be executed from the same location but with a `-C Release`. Alternatively, build the `RUN_TESTS` target from the IDE.

重建应用程序，然后 cd 到二进制目录，运行 `ctest` 可执行程序。`ctest -N` 和 `ctest -VV`。对于多配置生成器（如 Visual Studio），配置类型必须用 `-C <mode>` 标志指定。例如，要在 Debug 模式下运行测试，使用 `ctest -C Debug -VV` 从二进制目录（不是 Debug 子目录！）。发布模式将从相同的位置执行，但要用 `-C Release`。或者，从 IDE 中建立 `RUN_TESTS` 目标。

## Step5: Adding System Introspection
> Let us consider adding some code to our project that depends on features the target platform may not have. For this example, we will add some code that depends on whether or not the target platform has the `log` and `exp` functions. Of course almost every platform has these functions but for this tutorial assume that they are not common.

让我们考虑在我们的项目中添加一些代码，这些代码依赖于目标平台可能没有的功能。在这个例子中，我们将添加一些代码，这些代码取决于目标平台是否有 `log` 和 `exp` 函数。当然，几乎每个平台都有这些函数，但在本教程中，假设它们并不常见。

> If the platform has log and `exp` then we will use them to compute the square root in the `mysqrt` function. We first test for the availability of these functions using the `CheckSymbolExists` module in `MathFunctions/CMakeLists.txt`. On some platforms, we will need to link to the m library. If `log` and `exp` are not initially found, require the m library and try again.

如果平台上有 `log` 和 `exp`，那么我们将使用它们来计算 `mysqrt` 函数中的平方根。我们首先使用 `MathFunctions/CMakeLists.txt` 中的 `CheckSymbolExists` 模块测试这些函数是否可用。在一些平台上，我们将需要链接到 m 库。如果最初没有找到 `log` 和 `exp`，请要求使用 m 库并重新尝试。

> Add the checks for log and exp to `MathFunctions/CMakeLists.txt`, after the call to `target_include_directories()`:

在 `MathFunctions/CMakeLists.txt` 中，在调用 `target_include_directories()` 之后，增加对 log 和 exp 的检查。

```Makefile
#! MathFunctions/CMakeLists.txt
target_include_directories(MathFunctions
          INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}
          )

# does this system provide the log and exp functions?
include(CheckSymbolExists)
check_symbol_exists(log "math.h" HAVE_LOG)
check_symbol_exists(exp "math.h" HAVE_EXP)
if(NOT (HAVE_LOG AND HAVE_EXP))
  unset(HAVE_LOG CACHE)
  unset(HAVE_EXP CACHE)
  set(CMAKE_REQUIRED_LIBRARIES "m")
  check_symbol_exists(log "math.h" HAVE_LOG)
  check_symbol_exists(exp "math.h" HAVE_EXP)
  if(HAVE_LOG AND HAVE_EXP)
    target_link_libraries(MathFunctions PRIVATE m)
  endif()
endif()
```

> If available, use target_compile_definitions() to specify HAVE_LOG and HAVE_EXP as PRIVATE compile definitions.

如果有的话，使用 target_compile_definitions() 来指定 HAVE_LOG 和 HAVE_EXP 作为 PRIVATE 编译定义。

```Makefile
#!MathFunctions/CMakeLists.txt

if(HAVE_LOG AND HAVE_EXP)
  target_compile_definitions(MathFunctions
                             PRIVATE "HAVE_LOG" "HAVE_EXP")
endif()
```

> If log and exp are available on the system, then we will use them to compute the square root in the mysqrt function. Add the following code to the mysqrt function in MathFunctions/mysqrt.cxx (don't forget the #endif before returning the result!):

如果 log 和 exp 在系统中可用，那么我们将在 mysqrt 函数中使用它们来计算平方根。在MathFunctions/mysqrt.cxx 中的 mysqrt 函数中添加以下代码（不要忘记在返回结果之前的 #endif！）。

```Makefile
#! MathFunctions/mysqrt.cxx

#if defined(HAVE_LOG) && defined(HAVE_EXP)
  double result = exp(log(x) * 0.5);
  std::cout << "Computing sqrt of " << x << " to be " << result
            << " using log and exp" << std::endl;
#else
  double result = x;
```

We will also need to modify mysqrt.cxx to include cmath.

```Makefile
#! MathFunctions/mysqrt.cxx

#include <cmath>
```

> Run the `cmake` executable or the `cmake-gui` to configure the project and then build it with your chosen build tool and run the Tutorial executable.

运行 `cmake` 可执行文件或 `cmake-gui` 来配置项目，然后用你选择的构建工具来构建，并运行 Tutorial 可执行文件。

Which function gives better results now, `sqrt` or `mysqrt`?

## Step 6: Adding a Custom Command and Generated File
> Suppose, for the purpose of this tutorial, we decide that we never want to use the platform `log` and `exp` functions and instead would like to generate a table of precomputed values to use in the `mysqrt` function. In this section, we will create the table as part of the build process, and then compile that table into our application.

假设在本教程中，我们决定永远不使用平台上的 `log` 和 `exp` 函数，而是希望生成一个预计算值的表格，用于 `mysqrt` 函数。在本节中，我们将作为构建过程的一部分创建表格，然后将该表格编译到我们的应用程序中。

> First, let's remove the check for the `log` and `exp` functions in `MathFunctions/CMakeLists.txt`. Then remove the check for `HAVE_LOG` and `HAVE_EXP` from `mysqrt.cxx`. At the same time, we can remove `#include <cmath>`.

首先，让我们删除 `MathFunctions/CMakeLists.txt` 中对 `log` 和 `exp` 函数的检查。然后从 `mysqrt.cxx` 中删除对 `HAVE_LOG` 和 `HAVE_EXP` 的检查。同时，我们可以删除 `#include <cmath>`。

> In the `MathFunctions` subdirectory, a new source file named `MakeTable.cxx` has been provided to generate the table.

在 "MathFunctions" 子目录中，提供了一个名为 "MakeTable.cxx" 的新源文件来生成表格。

> After reviewing the file, we can see that the table is produced as valid C++ code and that the output filename is passed in as an argument.

在审查文件后，我们可以看到该表是作为有效的 C++ 代码产生的，而且输出文件名是作为参数传入的。

> The next step is to add the appropriate commands to the `MathFunctions/CMakeLists.txt` file to build the MakeTable executable and then run it as part of the build process. A few commands are needed to accomplish this.

下一步是在 `MathFunctions/CMakeLists.txt` 文件中添加适当的命令来构建MakeTable可执行文件，然后作为构建过程的一部分运行它。需要几个命令来完成这个任务。

> First, at the top of `MathFunctions/CMakeLists.txt`, the executable for `MakeTable` is added as any other executable would be added.

首先，在 `MathFunctions/CMakeLists.txt` 的顶部，添加 `MakeTable` 的可执行文件，如同添加任何其他可执行文件一样。

```makefile
#! MathFunctions/CMakeLists.txt

add_executable(MakeTable MakeTable.cxx)
```

> Then we add a custom command that specifies how to produce Table.h by running MakeTable.

然后我们添加一个自定义命令，指定如何通过运行 MakeTable 来产生 Table.h。

```makefile
#! MathFunctions/CMakeLists.txt

add_custom_command(
  OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/Table.h
  COMMAND MakeTable ${CMAKE_CURRENT_BINARY_DIR}/Table.h
  DEPENDS MakeTable
  )
```

Next we have to let CMake know that mysqrt.cxx depends on the generated file Table.h. This is done by adding the generated Table.h to the list of sources for the library MathFunctions.

接下来我们必须让 CMake 知道 mysqrt.cxx 依赖于生成的 Table.h 文件。这可以通过将生成的 Table.h 添加到 MathFunctions 库的源列表中来完成。

```makefile
#! MathFunctions/CMakeLists.txt

add_library(MathFunctions
            mysqrt.cxx
            ${CMAKE_CURRENT_BINARY_DIR}/Table.h
            )
```

We also have to add the current binary directory to the list of include directories so that Table.h can be found and included by mysqrt.cxx.

我们还必须将当前的二进制目录添加到包含目录列表中，以便 Table.h 可以被找到并被 mysqrt.cxx 包含。

```makefile
#! MathFunctions/CMakeLists.txt

target_include_directories(MathFunctions
          INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}
          PRIVATE ${CMAKE_CURRENT_BINARY_DIR}
          )
```

Now let's use the generated table. First, modify mysqrt.cxx to include Table.h. Next, we can rewrite the mysqrt function to use the table:

现在让我们使用生成的表。首先，修改 mysqrt.cxx 以包括 Table.h。接下来，我们可以重写 mysqrt函数以使用该表。

```makefile
#! MathFunctions/mysqrt.cxx
double mysqrt(double x)
{
  if (x <= 0) {
    return 0;
  }

  // use the table to help find an initial value
  double result = x;
  if (x >= 1 && x < 10) {
    std::cout << "Use the table to help find an initial value " << std::endl;
    result = sqrtTable[static_cast<int>(x)];
  }

  // do ten iterations
  for (int i = 0; i < 10; ++i) {
    if (result <= 0) {
      result = 0.1;
    }
    double delta = x - (result * result);
    result = result + 0.5 * delta / result;
    std::cout << "Computing sqrt of " << x << " to be " << result << std::endl;
  }

  return result;
}
```

Run the `cmake` executable or the `cmake-gui` to configure the project and then build it with your chosen build tool.

运行 `cmake` 可执行文件或 `cmake-gui` 来配置项目，然后用你选择的构建工具来构建。

> When this project is built it will first build the `MakeTable` executable. It will then run `MakeTable` to produce `Table.h`. Finally, it will compile `mysqrt.cxx` which includes `Table.h` to produce the `MathFunctions` library.

当这个项目被建立时，它将首先建立 `MakeTable` 可执行文件。然后它将运行 `MakeTable` 来产生 `Table.h`。最后，它将编译 `mysqrt.cxx`，其中包括 `Table.h` 以产生 `MathFunctions` 库。

Run the Tutorial executable and verify that it is using the table.

运行 Tutorial 可执行文件，并验证它是否在使用该表。

## Step 7: Packaging an Installer
Next suppose that we want to distribute our project to other people so that they can use it. We want to provide both binary and source distributions on a variety of platforms. This is a little different from the install we did previously in `Installing and Testing`, where we were installing the binaries that we had built from the source code. In this example we will be building installation packages that support binary installations and package management features. To accomplish this we will use CPack to create platform specific installers. Specifically we need to add a few lines to the bottom of our top-level `CMakeLists.txt` file.

```Makefile
#! CMakeLists.txt

include(InstallRequiredSystemLibraries)
set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/License.txt")
set(CPACK_PACKAGE_VERSION_MAJOR "${Tutorial_VERSION_MAJOR}")
set(CPACK_PACKAGE_VERSION_MINOR "${Tutorial_VERSION_MINOR}")
set(CPACK_SOURCE_GENERATOR "TGZ")
include(CPack)
```

That is all there is to it. We start by including InstallRequiredSystemLibraries. This module will include any runtime libraries that are needed by the project for the current platform. Next we set some CPack variables to where we have stored the license and version information for this project. The version information was set earlier in this tutorial and the License.txt has been included in the top-level source directory for this step. The CPACK_SOURCE_GENERATOR variable selects a file format for the source package.

Finally we include the CPack module which will use these variables and some other properties of the current system to setup an installer.

The next step is to build the project in the usual manner and then run the cpack executable. To build a binary distribution, from the binary directory run:

```bash
cpack
```

To specify the generator, use the -G option. For multi-config builds, use -C to specify the configuration. For example:

```bash
cpack -G ZIP -C Debug
```

For a list of available generators, see `cpack-generators(7)` or call `cpack --help`. An archive generator like ZIP creates a compressed archive of all installed files.

To create an archive of the full source tree you would type:

```bash
cpack --config CPackSourceConfig.cmake
```

Alternatively, run `make package` or right click the `Package` target and `Build Project` from an IDE.

Run the installer found in the binary directory. Then run the installed executable and verify that it works.

## Step 8: Adding Support for a Testing Dashboard
Adding support for submitting our test results to a dashboard is simple. We already defined a number of tests for our project in `Testing Support`. Now we just have to run those tests and submit them to a dashboard. To include support for dashboards we include the `CTest` module in our top-level `CMakeLists.txt`.

Replace:

```makefile
#! CMakeLists.txt

# enable testing
enable_testing()
```

With:

```makefile
#! CMakeLists.txt

# enable dashboard scripting
include(CTest)
```

The `CTest` module will automatically call `enable_testing()`, so we can remove it from our CMake files.

We will also need to acquire a `CTestConfig.cmake` file to be placed in the top-level directory where we can specify information to CTest about the project. It contains:
+ The project name
+ The project "Nightly" start time
  + The time when a 24 hour "day" starts for this project
+ The URL of the CDash instance where the submission's generated documents will be sent

One has been provided for you in this directory. It would normally be downloaded from the Settings page of the project on the CDash instance that will host and display the test results. Once downloaded from CDash, the file should not be modified locally.

```makefile
#! CTestConfig.cmake

set(CTEST_PROJECT_NAME "CMakeTutorial")
set(CTEST_NIGHTLY_START_TIME "00:00:00 EST")

set(CTEST_DROP_METHOD "http")
set(CTEST_DROP_SITE "my.cdash.org")
set(CTEST_DROP_LOCATION "/submit.php?project=CMakeTutorial")
set(CTEST_DROP_SITE_CDASH TRUE)
```

The ctest executable will read in this file when it runs. To create a simple dashboard you can run the cmake executable or the cmake-gui to configure the project, but do not build it yet. Instead, change directory to the binary tree, and then run:

```bash
ctest [-VV] -D Experimental
```

Remember, for multi-config generators (e.g. Visual Studio), the configuration type must be specified:

```bash
ctest [-VV] -C Debug -D Experimental
```

Or, from an IDE, build the `Experimental` target.

The `ctest` executable will build and test the project and submit the results to Kitware's public dashboard: https://my.cdash.org/index.php?project=CMakeTutorial.

## Step 9: Selecting Static or Shared Libraries
In this section we will show how the `BUILD_SHARED_LIBS` variable can be used to control the default behavior of `add_library()`, and allow control over how libraries without an explicit type (`STATIC`, `SHARED`, `MODULE` or `OBJECT`) are built.

To accomplish this we need to add `BUILD_SHARED_LIBS` to the top-level `CMakeLists.txt`. We use the `option()` command as it allows users to optionally select if the value should be `ON` or `OFF`.

Next we are going to refactor `MathFunctions` to become a real library that encapsulates using `mysqrt` or `sqrt`, instead of requiring the calling code to do this logic. This will also mean that `USE_MYMATH` will not control building `MathFunctions`, but instead will control the behavior of this library.

The first step is to update the starting section of the top-level `CMakeLists.txt` to look like:

```makefile
#! CMakeLists.txt
cmake_minimum_required(VERSION 3.10)

# set the project name and version
project(Tutorial VERSION 1.0)

# specify the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# control where the static and shared libraries are built so that on windows
# we don't need to tinker with the path to run the executable
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")

option(BUILD_SHARED_LIBS "Build using shared libraries" ON)

# configure a header file to pass the version number only
configure_file(TutorialConfig.h.in TutorialConfig.h)

# add the MathFunctions library
add_subdirectory(MathFunctions)

# add the executable
add_executable(Tutorial tutorial.cxx)
target_link_libraries(Tutorial PUBLIC MathFunctions)
```

Now that we have made MathFunctions always be used, we will need to update the logic of that library. So, in MathFunctions/CMakeLists.txt we need to create a SqrtLibrary that will conditionally be built and installed when USE_MYMATH is enabled. Now, since this is a tutorial, we are going to explicitly require that SqrtLibrary is built statically.

The end result is that `MathFunctions/CMakeLists.txt` should look like:

```makefile
#! MathFunctions/CMakeLists.txt


# add the library that runs
add_library(MathFunctions MathFunctions.cxx)

# state that anybody linking to us needs to include the current source dir
# to find MathFunctions.h, while we don't.
target_include_directories(MathFunctions
                           INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}
                           )

# should we use our own math functions
option(USE_MYMATH "Use tutorial provided math implementation" ON)
if(USE_MYMATH)

  target_compile_definitions(MathFunctions PRIVATE "USE_MYMATH")

  # first we add the executable that generates the table
  add_executable(MakeTable MakeTable.cxx)

  # add the command to generate the source code
  add_custom_command(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/Table.h
    COMMAND MakeTable ${CMAKE_CURRENT_BINARY_DIR}/Table.h
    DEPENDS MakeTable
    )

  # library that just does sqrt
  add_library(SqrtLibrary STATIC
              mysqrt.cxx
              ${CMAKE_CURRENT_BINARY_DIR}/Table.h
              )

  # state that we depend on our binary dir to find Table.h
  target_include_directories(SqrtLibrary PRIVATE
                             ${CMAKE_CURRENT_BINARY_DIR}
                             )

  target_link_libraries(MathFunctions PRIVATE SqrtLibrary)
endif()

# define the symbol stating we are using the declspec(dllexport) when
# building on windows
target_compile_definitions(MathFunctions PRIVATE "EXPORTING_MYMATH")

# install rules
set(installable_libs MathFunctions)
if(TARGET SqrtLibrary)
  list(APPEND installable_libs SqrtLibrary)
endif()
install(TARGETS ${installable_libs} DESTINATION lib)
install(FILES MathFunctions.h DESTINATION include)
```

Next, update MathFunctions/mysqrt.cxx to use the mathfunctions and detail namespaces:

```c++
//! MathFunctions/mysqrt.cxx

#include <iostream>

#include "MathFunctions.h"

// include the generated table
#include "Table.h"

namespace mathfunctions {
    namespace detail {
        // a hack square root calculation using simple operations
        double mysqrt(double x)
        {
            if (x <= 0) {
                return 0;
            }

            // use the table to help find an initial value
            double result = x;
            if (x >= 1 && x < 10) {
                std::cout << "Use the table to help find an initial value " << std::endl;
                result = sqrtTable[static_cast<int>(x)];
            }

            // do ten iterations
            for (int i = 0; i < 10; ++i) {
                if (result <= 0) {
                result = 0.1;
                }
                double delta = x - (result * result);
                result = result + 0.5 * delta / result;
                std::cout << "Computing sqrt of " << x << " to be " << result << std::endl;
            }

            return result;
        }
    }
}
```

We also need to make some changes in `tutorial.cxx`, so that it no longer uses `USE_MYMATH`:
1. Always include `MathFunctions.h`
2. Always use `mathfunctions::sqrt`
3. Don't include `cmath`

Finally, update `MathFunctions/MathFunctions.h` to use dll export defines:

```c++
#! MathFunctions/MathFunctions.h


#if defined(_WIN32)
#  if defined(EXPORTING_MYMATH)
#    define DECLSPEC __declspec(dllexport)
#  else
#    define DECLSPEC __declspec(dllimport)
#  endif
#else // non windows
#  define DECLSPEC
#endif

namespace mathfunctions {
    double DECLSPEC sqrt(double x);
}

```

At this point, if you build everything, you may notice that linking fails as we are combining a static library without position independent code with a library that has position independent code. The solution to this is to explicitly set the POSITION_INDEPENDENT_CODE target property of SqrtLibrary to be True no matter the build type.

```makefile
#! MathFunctions/CMakeLists.txt


  # state that SqrtLibrary need PIC when the default is shared libraries
  set_target_properties(SqrtLibrary PROPERTIES
                        POSITION_INDEPENDENT_CODE ${BUILD_SHARED_LIBS}
                        )

  target_link_libraries(MathFunctions PRIVATE SqrtLibrary)

```

Exercise: We modified MathFunctions.h to use dll export defines. Using CMake documentation can you find a helper module to simplify this?

## Step 10: Adding Generator Expressions
`Generator expressions` are evaluated during build system generation to produce information specific to each build configuration.

`Generator expressions` are allowed in the context of many target properties, such as `LINK_LIBRARIES`, `INCLUDE_DIRECTORIES`, `COMPILE_DEFINITIONS` and others. They may also be used when using commands to populate those properties, such as `target_link_libraries()`, `target_include_directories()`, `target_compile_definitions()` and others.

`Generator expressions` may be used to enable conditional linking, conditional definitions used when compiling, conditional include directories and more. The conditions may be based on the build configuration, target properties, platform information or any other queryable information.

There are different types of `generator expressions` including Logical, Informational, and Output expressions.

Logical expressions are used to create conditional output. The basic expressions are the `0` and `1` expressions. A `$<0:...>` results in the empty string, and `<1:...>` results in the content of `...`. They can also be nested.

A common usage of `generator expressions` is to conditionally add compiler flags, such as those for language levels or warnings. A nice pattern is to associate this information to an `INTERFACE` target allowing this information to propagate. Let's start by constructing an `INTERFACE` target and specifying the required C++ standard level of `11` instead of using `CMAKE_CXX_STANDARD`.

So the following code:

```makefile
#! CMakeLists.txt
# specify the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)
```

Would be replaced with:

```makefile
CMakeLists.txt
add_library(tutorial_compiler_flags INTERFACE)
target_compile_features(tutorial_compiler_flags INTERFACE cxx_std_11)
```

Note: This upcoming section will require a change to the `cmake_minimum_required()` usage in the code. The Generator Expression that is about to be used was introduced in 3.15. Update the call to require that more recent version:

```makefile
#! CMakeLists.txt
cmake_minimum_required(VERSION 3.15)
```

Next we add the desired compiler warning flags that we want for our project. As warning flags vary based on the compiler we use the `COMPILE_LANG_AND_ID` generator expression to control which flags to apply given a language and a set of compiler ids as seen below:

```makefile
#! CMakeLists.txt
set(gcc_like_cxx "$<COMPILE_LANG_AND_ID:CXX,ARMClang,AppleClang,Clang,GNU,LCC>")
set(msvc_cxx "$<COMPILE_LANG_AND_ID:CXX,MSVC>")
target_compile_options(tutorial_compiler_flags INTERFACE
  "$<${gcc_like_cxx}:$<BUILD_INTERFACE:-Wall;-Wextra;-Wshadow;-Wformat=2;-Wunused>>"
  "$<${msvc_cxx}:$<BUILD_INTERFACE:-W3>>"
)
```

Looking at this we see that the warning flags are encapsulated inside a `BUILD_INTERFACE` condition. This is done so that consumers of our installed project will not inherit our warning flags.

Exercise: Modify `MathFunctions/CMakeLists.txt` so that all targets have a `target_link_libraries()` call to `tutorial_compiler_flags`.

## Step 11: Adding Export Configuration
During `Installing and Testing` of the tutorial we added the ability for CMake to install the library and headers of the project. During `Packaging an Installer` we added the ability to package up this information so it could be distributed to other people.

The next step is to add the necessary information so that other CMake projects can use our project, be it from a build directory, a local install or when packaged.

The first step is to update our `install(TARGETS)` commands to not only specify a `DESTINATION` but also an `EXPORT`. The `EXPORT` keyword generates a CMake file containing code to import all targets listed in the install command from the installation tree. So let's go ahead and explicitly `EXPORT` the `MathFunctions` library by updating the `install` command in `MathFunctions/CMakeLists.txt` to look like:

```makefile
#! MathFunctions/CMakeLists.txt

set(installable_libs MathFunctions tutorial_compiler_flags)
if(TARGET SqrtLibrary)
  list(APPEND installable_libs SqrtLibrary)
endif()
install(TARGETS ${installable_libs}
        EXPORT MathFunctionsTargets
        DESTINATION lib)
install(FILES MathFunctions.h DESTINATION include)
```

Now that we have MathFunctions being exported, we also need to explicitly install the generated MathFunctionsTargets.cmake file. This is done by adding the following to the bottom of the top-level CMakeLists.txt:

```makefile
#! CMakeLists.txt

install(EXPORT MathFunctionsTargets
  FILE MathFunctionsTargets.cmake
  DESTINATION lib/cmake/MathFunctions
)
```

At this point you should try and run CMake. If everything is setup properly you will see that CMake will generate an error that looks like:

```
Target "MathFunctions" INTERFACE_INCLUDE_DIRECTORIES property contains
path:

  "/Users/robert/Documents/CMakeClass/Tutorial/Step11/MathFunctions"

which is prefixed in the source directory.
```

What CMake is trying to say is that during generating the export information it will export a path that is intrinsically tied to the current machine and will not be valid on other machines. The solution to this is to update the `MathFunctions` `target_include_directories()` to understand that it needs different `INTERFACE` locations when being used from within the build directory and from an install / package. This means converting the `target_include_directories()` call for `MathFunctions` to look like:

```makefile
#! MathFunctions/CMakeLists.txt
target_include_directories(MathFunctions
                           INTERFACE
                            $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>
                            $<INSTALL_INTERFACE:include>
                           )
```

Once this has been updated, we can re-run CMake and verify that it doesn't warn anymore.

At this point, we have CMake properly packaging the target information that is required but we will still need to generate a `MathFunctionsConfig.cmake` so that the CMake `find_package()` command can find our project. So let's go ahead and add a new file to the top-level of the project called `Config.cmake.in` with the following contents:

```makefile
#! Config.cmake.in

@PACKAGE_INIT@

include ( "${CMAKE_CURRENT_LIST_DIR}/MathFunctionsTargets.cmake" )
```

Then, to properly configure and install that file, add the following to the bottom of the top-level CMakeLists.txt:

```makefile
#! CMakeLists.txt

install(EXPORT MathFunctionsTargets
  FILE MathFunctionsTargets.cmake
  DESTINATION lib/cmake/MathFunctions
)

include(CMakePackageConfigHelpers)
```

Next, we execute the `configure_package_config_file()`. This command will configure a provided file but with a few specific differences from the standard `configure_file()` way. To properly utilize this function, the input file should have a single line with the text `@PACKAGE_INIT@` in addition to the content that is desired. That variable will be replaced with a block of code which turns set values into relative paths. These values which are new can be referenced by the same name but prepended with a `PACKAGE_` prefix.

```makefile
#! CMakeLists.txt
install(EXPORT MathFunctionsTargets
  FILE MathFunctionsTargets.cmake
  DESTINATION lib/cmake/MathFunctions
)

include(CMakePackageConfigHelpers)
# generate the config file that is includes the exports
configure_package_config_file(${CMAKE_CURRENT_SOURCE_DIR}/Config.cmake.in
  "${CMAKE_CURRENT_BINARY_DIR}/MathFunctionsConfig.cmake"
  INSTALL_DESTINATION "lib/cmake/example"
  NO_SET_AND_CHECK_MACRO
  NO_CHECK_REQUIRED_COMPONENTS_MACRO
  )
```

The `write_basic_package_version_file()` is next. This command writes a file which is used by the "find_package" document the version and compatibility of the desired package. Here, we use the `Tutorial_VERSION_`* variables and say that it is compatible with `AnyNewerVersion`, which denotes that this version or any higher one are compatible with the requested version.

```makefile
#! CMakeLists.txt
write_basic_package_version_file(
  "${CMAKE_CURRENT_BINARY_DIR}/MathFunctionsConfigVersion.cmake"
  VERSION "${Tutorial_VERSION_MAJOR}.${Tutorial_VERSION_MINOR}"
  COMPATIBILITY AnyNewerVersion
)
```

Finally, set both generated files to be installed:

```makefile
#! CMakeLists.txt
install(FILES
  ${CMAKE_CURRENT_BINARY_DIR}/MathFunctionsConfig.cmake
  ${CMAKE_CURRENT_BINARY_DIR}/MathFunctionsConfigVersion.cmake
  DESTINATION lib/cmake/MathFunctions
  )
```

At this point, we have generated a relocatable CMake Configuration for our project that can be used after the project has been installed or packaged. If we want our project to also be used from a build directory we only have to add the following to the bottom of the top level `CMakeLists.txt`:

```makefile
#! CMakeLists.txt
export(EXPORT MathFunctionsTargets
  FILE "${CMAKE_CURRENT_BINARY_DIR}/MathFunctionsTargets.cmake"
)
```

With this export call we now generate a `Targets.cmake`, allowing the configured `MathFunctionsConfig.cmake` in the build directory to be used by other projects, without needing it to be installed.

## Step 12: Packaging Debug and Release
Note: This example is valid for single-configuration generators and will not work for multi-configuration generators (e.g. Visual Studio).

By default, CMake's model is that a build directory only contains a single configuration, be it Debug, Release, MinSizeRel, or RelWithDebInfo. It is possible, however, to setup CPack to bundle multiple build directories and construct a package that contains multiple configurations of the same project.

First, we want to ensure that the debug and release builds use different names for the executables and libraries that will be installed. Let's use d as the postfix for the debug executable and libraries.

Set `CMAKE_DEBUG_POSTFIX` near the beginning of the top-level CMakeLists.txt file:

```makefile
#! CMakeLists.txt
set(CMAKE_DEBUG_POSTFIX d)

add_library(tutorial_compiler_flags INTERFACE)
```

And the DEBUG_POSTFIX property on the tutorial executable:

```makefile
#! CMakeLists.txt
add_executable(Tutorial tutorial.cxx)
set_target_properties(Tutorial PROPERTIES DEBUG_POSTFIX ${CMAKE_DEBUG_POSTFIX})

target_link_libraries(Tutorial PUBLIC MathFunctions)
```

Let's also add version numbering to the MathFunctions library. In MathFunctions/CMakeLists.txt, set the VERSION and SOVERSION properties:

```makefile
#! MathFunctions/CMakeLists.txt

set_property(TARGET MathFunctions PROPERTY VERSION "1.0.0")
set_property(TARGET MathFunctions PROPERTY SOVERSION "1")
```

From the `Step12` directory, create `debug` and `release` subbdirectories. The layout will look like:

```
- Step12
   - debug
   - release
```

Now we need to setup debug and release builds. We can use CMAKE_BUILD_TYPE to set the configuration type:

```
cd debug
cmake -DCMAKE_BUILD_TYPE=Debug ..
cmake --build .
cd ../release
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build .
```

Now that both the debug and release builds are complete, we can use a custom configuration file to package both builds into a single release. In the `Step12` directory, create a file called `MultiCPackConfig.cmake`. In this file, first include the default configuration file that was created by the `cmake` executable.

Next, use the `CPACK_INSTALL_CMAKE_PROJECTS` variable to specify which projects to install. In this case, we want to install both debug and release.

```makefile
#! MultiCPackConfig.cmake

include("release/CPackConfig.cmake")

set(CPACK_INSTALL_CMAKE_PROJECTS
    "debug;Tutorial;ALL;/"
    "release;Tutorial;ALL;/"
    )
```

From the `Step12` directory, run `cpack` specifying our custom configuration file with the `config` option:

```
cpack --config MultiCPackConfig.cmake
```