[TOC]

# Makefile 工作原理
1. 基本原则：若想生成目标，检查规则中的依赖条件是否存在，如不存在，则寻找是否有规则用来生成该依赖文件。
2. 检查规则中的目标是否需要更新，必须先检查它的所有依赖，依赖中有任一个被更新，则目标必须更新。
   1. 分析各个目标和依赖之间的关系
   2. 根据依赖关系自底向上执行命令
   3. 根据修改时间比目标新，确定更新
   4. 如果目标不依赖任何条件，则执行对应命令，以示更新。

```
// hello：生成的目标名
// hello.c 生成目标依赖的文件
// 紧接着一个tab，生成目标的方法（命令）
hello:hello.c           
	gcc hello.c -o hello
```

### Makefile 变量
在 Makefile 中使用变量类似于 C 语言的宏定义，使用该变量相当于内容替换，使用变量可以使 Makefile 易于维护，修改内容变得简单。
+ 变量定义使用‘=’
+ '+=' 可追加变量
+ ’:=' 恒等于，可定义常量
+ 使用变量值用$(变量名)
```
# 变量定义及使用
foo = abc
bar = $(foo)
# 以上定义了两个变量：foo、bar，其中 bar 的值是 foo 变量值的引用。
```

#### 自动变量
```
$@  #表示规则中的目标
$<  #表示规则中的第一个条件。如果将该变量应用在模式规则中，它可将依赖条件列表中的依赖依次取出，套用模式规则。
$^  #表示规则中的所有条件，组成一个列表，以空格隔开，如果这个列表中有重复的项则消除重复项。
```

##### 模式规则
至少在规则的目标定义中要包含 ‘%’，‘%’ 表示一个或多个，在依赖条件中同样可以使用 ‘%’, 依赖条件中的 '%' 的取值，取决于其目标

##### 静态模式规则

##### 伪目标
.PHONY clean ALL


### Makefile 函数
```
// 匹配当前工作目录下的所有.c文件，将文件名组成列表，赋值给变量 src
    src = $(wildcard ./*.c) 

// 将参数 3 中，包含参数 1 的部分，替换为参数 2
    obj = $(patsubst %.c, %.o, $(src))
```


命名：makefile or Makefile
1个规则：
    目标：依赖条件
            （一个tab缩进）命令



# Learn Makefiles

## Getting Started

### Why do Makefiles exist?
> Makefiles are used to help decide which parts of a large parogram need to be recompiled. In the vast majority of cases, C or C++ files are compiled. Other languages typically have their own tools that serve a similar purpose as Make. It can be used beyond programs too, when you need a series of instructions to run depending on what files have changed. This tutorial will focus on the C/C++ compilation use case.

Makefile 用于帮助决定一个大型准系统的哪些部分需要被重新编译。在绝大多数情况下，C 或 C++ 文件被编译。其他语言通常有自己的工具，其作用与 Make 类似。它也可以在程序之外使用，当你需要根据哪些文件的变化来运行一系列指令时。本教程将重点介绍 C/C++ 编译的使用情况。

### What alternatives are there to Make?
> Popular C/C++ alternative build systems are SCons, CMake, Bazel, and Ninja. Some code editors like Microsoft Visual Studio have their own built in build tools. For Java, there's Ant, Maven, and Gradle. Other languages like Go and Rust have their own build tools.

流行的 C/C++ 替代构建系统有 SCons、CMake、Bazel 和 Ninja。一些代码编辑器，如微软 Visual Studio，也有自己的内置构建工具。对于 Java，有 Ant、Maven 和 Gradle。其他语言如 Go 和 Rust 也有自己的构建工具。

> Interpreted languages like Python, Ruby, and Javascript don't require an analogue to Makefiles. The goal of Makefiles is to compile whatever files need to be compiled, based on what files have changed. But when files in interpreted languages change, nothing needs to get recompiled. When the program runs, the most recent version of the file is used.

像 Python、Ruby 和 Javascript 这样的解释型语言不需要类似 Makefiles 的东西。Makefiles 的目标是根据哪些文件发生了变化，来编译任何需要编译的文件。但是，当解释型语言的文件发生变化时，没有什么需要被重新编译的。当程序运行时，使用的是文件的最新版本。

### The versions and types of Make
> There are a variety of implementations of Make, but most of this guide will work on whatever version you're using. However, it's specifically written for GNU Make, which is the standard implementation on Linux and MacOS. All the examples work for Make versions 3 and 4, which are nearly equivalent other than some esoteric differences.

有各种不同的 Make 实现，但本指南的大部分内容将适用于你所使用的任何版本。然而，它是专门为 GNU Make 编写的，它是 Linux 和 MacOS 上的标准实现。所有的例子都适用于 Make 的第 3 和第 4 版，除了一些深奥的差异外，它们几乎是等同的。

### Running the Examples
> To run these examples, you'll need a terminal and "make" installed. For each example, put the contents in a file called Makefile, and in that directory run the command make. Let's start with the simplest of Makefiles:

要运行这些例子，你需要一个终端和安装 "make"。对于每个例子，将其内容放在一个叫做 Makefile 的文件中，并在该目录下运行 make 命令。让我们从最简单的 Makefile 文件开始。

```Makefile
hello:
    echo "hello world"
```

> Here is the output of running the above example:

```bash
$ make
echo "hello world"
hello world
```

### Makefile Syntax
A Makefile consists of a set of rules. A rule generally looks like this:

```Makefile
targets: prerequisites
    command
    command
    command
```

+ The targets are file names, separated by spaces. Typically, there is only one per rule.
+ The commands are a series of steps typically used to make the target(s). These need to start with a tab character, not spaces.
+ The prerequisites are also file names, separated by spaces. These files need to exist before the commands for the target are run. These are also called dependencies

+ 目标是文件名，用空格分隔。通常，每条规则只有一条。
+ 这些命令是一系列通常用于生成目标的步骤。它们需要以制表符开始，而不是空格。
+ 先决条件也是文件名，用空格分隔。在运行目标命令之前，这些文件必须存在。这些也被称为依赖项

### Beginner Examples
> The following Makefile has three separate rules. When you run make blah in the terminal, it will build a program called blah in a series of steps:

下面的 Makefile 有三个单独的规则。在终端中运行 make blah 时，它将通过一系列步骤构建一个名为 blah 的程序：

+ Make is given blah as the target, so it first searches for this target
+ blah requires blah.o, so make searches for the blah.o target
+ blah.o requires blah.c, so make searches for the blah.c target
+ blah.c has no dependencies, so the echo command is run
+ The cc -c command is then run, because all of the blah.o dependencies are finished
+ The top cc command is run, because all the blah dependencies are finished
+ That's it: blah is a compiled c program

```Makefile
blah: blah.o
    cc blah.o blah                              # uns third

blah.o: blah.c
    cc -c blah.c -o blah.o                      # Runs second

blah.c:
    echo "int main() { return 0; }" > blah.c    # Runs first
```

This makefile has a single target, called some_file. The default target is the first target, so in this case some_file will run.

```Makefile
some_file:
    echo "This line will always print"
```

This file will make some_file the first time, and the second time notice it's already made, resulting in make: 'some_file' is up to date.

```Makefile
some_file:
    echo "This line will always print once"
    touch some_file
```

Here, the target some_file "depends" on other_file. When we run make, the default target (some_file, since it's first) will get called. It will first look at its list of dependencies, and if any of them are older, it will first run the targets for those dependencies, and then run itself. The second time this is run, neither target will run because both targets exist.

在这里，some_file 的目标依赖于 other_file。当我们运行 make 时，将调用默认目标（some_file，因为它是第一个）。它将首先查看其依赖项列表，如果其中任何依赖项比较旧，它将首先运行这些依赖项的目标，然后自行运行。第二次运行时，两个目标都不会运行，因为两个目标都存在。

```Makefile
some_file: other_file
    echo "This will run second, because it depends on other_file"
    touch some_file

other_file:
    echo "This will run first"
    touch other_file
```

This will always run both targets, because some_file depends on other_file, which is never created.

```Makefile
some_file: other_file
    touch some_file

other_file:
    echo "nothing"
```

clean is often used as a target that removes the output of other targets, but it is not a special word in make.

```Makefile
some_file:
    touch some_file

clean:
    rm -f some_file
```

### Variables
Variables can only be strings. You'll typically want to use :=, but = also works. See [Variables Pt 2.](#variables-pt2)

Here's an example of using variables:

```Makefile
files := file1 file2

some_file: $(files)
    echo "Look at this variable: " $(files)
    touch some_file

file1:
    touch file1
file2:
    touch file2

clean:
    rm -f file1 file2 some_file
```

Reference variables using either ${} or $()

```Makefile
x := dude

all:
    echo $(x)
    echo ${x}

    # Bad practice, but works
    echo $x
```

## Targets

### The all target
Makeing multiple targets and you want all of them to run? Make an all target

```Makefile
all: one two three

one:
    touch one
two:
    touch two
three:
    touch three

clean:
    rm -f one two three
```

### Multiple targets
When there are multiple targets for a rule, the commands will be run for each target $@ is an automatic variable that contains the target name.

```Makefile
all: f1.o f2.o

f1.o f2.o
    echo $@
# Equivalent to:
# f1.o:
#   echo f1.o
# f2.o
#   echo f2.o
```

## Automatic Variables and Wildcards

### * Wildcard
> Both * and % are called wildcards in Make, but they mean entirely different things. * searches your filesystem for matching filenames. I suggest that you always wrap it in the wildcard funciton, because otherwise you may fail into a common pitfall described below.

* 和 % 在 Make 中都被称为通配符，但它们的含义完全不同。* 在文件系统中搜索匹配的文件名。我建议您总是将其包装在通配符函数中，否则您可能会陷入下面描述的常见陷阱。

```Makefile
# Print out file information about every .c file
print: $(wildcard *.c)
    ls -la $?
```

* may be used in the target, prerequisites, or in the wildcard function.

Danger: * may not be directly used in a variable definitions

Danger: When * matched no files, it is left as it is (unless run in the wildcard function)

```Makefile
thing_wrong := *.o  # Don't do this! '*' will not get expanded
thing_right := $(wildcard *.o)

all: one two three four

# Fails, because $(thing_wrong) is the string "*.o"
one: $(thing_wrong)

# Stays as *.o if there are no files that match this pattern :(
two: *.o

# Works as you would expect! In this case, it does nothing.
three: $(thing_right)

# Same as rule three
four: $(wildcard *.o)
```

### % Wildcard
> % is really useful, but is somewhat confusing because of the variety of situations it can be used in.

% 是非常有用的，但由于它可以用于多种情况，因此有点令人困惑。

+ When used in "matching" mode, it matches one or more characters in a string. This match is called the stem.
+ When used in "replacing" mode, it takes the stem that was matched and replaces that in a string.
+ % is most often used in rule definitions and in some specific functions.

+ 在“匹配”模式下使用时，它匹配字符串中的一个或多个字符。这场比赛被称为 stem。
+ 在“替换”模式下使用时，它会将匹配的阀杆替换为字符串。
+ % 最常用于规则定义和某些特定函数中。

See these sections on examples of it being used:

+ [Static Pattern Rules](#static-pattern-rules)
+ [Pattern Rules](#pattern-rules)
+ [String Substitution](#string-substitution)
+ [The vpath Directive](#the-vpath-directive)

### Automatic Variables
> There are many automatic variables, but often only a few show up:

自动变量有很多，但通常只有少数几个：

```Makefile
hey: one two
    # Outputs "hey", since this is the first target
    echo $@

    # Outputs all prerequisites newer than the target
    echo $?

    # Outputs all prerequisites
    echo $^

    touch key

one:
    touch one

two:
    touch two

clean:
    rm -f hey one two
```

## Fancy Rules

### Implicit Rules
> Make loves c compilation. And every time it expresses its love, things get confusing. Perhaps the most confusing part of Make is the magic/automatic rules that are made. Make calls these "implicit" rules. they're often used and are thus useful to know. Here's a list of implicit rules:

Make 喜欢 c 语言编译。每一次它表达爱的时候，事情都会变得扑朔迷离。也许 Make 中最令人困惑的部分是那些神奇的/自动的规则。Make 把这些规则称为隐式规则。它们经常被使用，因此了解它们很有用。以下是一系列隐含规则：

+ Compiling a C program: n.o is made automatically from n.c with a command of the form $(CC) -c $(CPPFLAGS) $(CFLAGS)
+ Compiling a C++ program: n.o is made automatically from n.cc or n.cpp with a command of the form $(CXX) -c $(CPPFLAGS) $(CXXFLAGS)
+ Linking a single object file: n is made automatically from n.o by running the command $(CC) $(LDFLAGS) n.o $(LOADLIBES) $(LDLIBS)

The important variables used by implicit rules are:
+ CC: Program for compiling C programs; default cc
+ CXX: Program for compiling C++ programs; default g++
+ CFLAGS: Extra flags to give to the C compiler
+ CXXFLAGS: Extra flags to give to the C++ compiler
+ CPPFLAGS: Extra flags to give to the C preprocessor
+ LDFLAGS: Extra flags to give to compilers when the are supposed to invoke the linker

Let's see how we can now build a C program without ever explicitly telling Make how to do the compililation:

```Makefile
CC = gcc    # Flag for implicit rules
CFLAGS = -g     # Flag for implicit rules. Turn on debug info

# Implicit rule  #1: blah is built via the C linker implicit rule
# Implicit rule  #2: blah.o is built via the C compilation implicit rule, because blah.c exists
blah: blah.o

blah.c:
	echo "int main() { return 0; }" > blah.c

clean:
	rm -f blah*
```

### Static Pattern Rules
> Static pattern rules are another way to write less in a Makefile, but I'd say are more useful and a bit less "magic". Here's their syntax:

静态模式规则是在 Makefile 中编写更少代码的另一种方式，但我认为它更有用，也没有那么“神奇”。以下是它们的语法：

```Makefile
targets...: target-pattern: prereq-patterns ...
   commands
```

> The essence is that the given target is matched by the target-pattern (via a % wildcard). Whatever was matched is called the stem. The stem is then substituted into the prereq-pattern, to generate the target's prereqs.

本质上，给定的目标与目标模式匹配（通过 % 通配符）。匹配的东西叫做 stem。然后将 stem 替换为 prereq 模式，以生成目标的 prereq。

A typical use case is to compile .c files into .o files. Here's the manual way:

```Makefile
objects = foo.o bar.o all.o
all: $(objects)

# These files compile via implicit rules
foo.o: foo.c
bar.o: bar.c
all.o: all.c

all.c:
	echo "int main() { return 0; }" > all.c

%.c:
	touch $@

clean:
	rm -f *.c *.o all
```

Here's the more efficient way, using a static pattern rule:

```Makefile
objects = foo.o bar.o all.o
all: $(objects)

# These files compile via implicit rules
# Syntax - targets ...: target-pattern: prereq-patterns ...
# In the case of the first target, foo.o, the target-pattern matches foo.o and sets the "stem" to be "foo".
# It then replaces the '%' in prereq-patterns with that stem
$(objects): %.o: %.c

all.c:
	echo "int main() { return 0; }" > all.c

%.c:
	touch $@

clean:
	rm -f *.c *.o all
```

### Static Pattern Rules and Filter
> While I introduce functions later on, I'll foreshadow what you can do with them. The filter function can be used in Static pattern rules to match the correct files. In this example, I made up the .raw and .result extensions.

在我稍后介绍函数的同时，我将预告如何使用它们。可以在静态模式规则中使用 filter 函数来匹配正确的文件。

```Makefile
obj_files = foo.result bar.o lose.o
src_files = foo.raw bar.c lose.c

.PHONY: all
all: $(obj_files)

$(filter %.o, $(obj_files)): %.o: %.c
	echo "target: $@ prereq: $<"
$(filter %.result, $(obj_files)): %.result: %.raw
	echo "target: $@ prereq: $<" 

%.c %.raw:
	touch $@

clean:
	rm -f $(src_files)
```

### Pattern Rules
Pattern rules are often used but quite confusing. You can look at them as two ways:
+ A way to define your own implicit rules
+ A simpler form of static pattern rules

Let's start with an example first:
```Makefile
# Define a pattern rule that compiles every .c file into a .o file
%.o : %.c
		$(CC) -c $(CFLAGS) $(CPPFLAGS) $< -o $@
```

> Pattern rules contain a '%' in the target. This '%' matches any nonempty string, and the other characters match themselves. ‘%’ in a prerequisite of a pattern rule stands for the same stem that was matched by the ‘%’ in the target.

模式规则在目标中包含 '%'。'%' 匹配任何非空字符串，其他字符匹配它们自己在模式的先决条件中，规则代表与目标中 “%” 匹配的同一个 stem。

Here's another example:

```Makefile
# Define a pattern rule that has no pattern in the prerequisites.
# This just creates empty .c files when needed.
%.c:
   touch $@
```

### Double-Colon Rules
Double-Colon Rules are rarely used, but allow multiple rules to be defined for the same target. If these were single colons, a warning would be printed and only the second set of commands would run.

很少使用双冒号规则，但允许为同一目标定义多个规则。如果这些是单冒号，则会打印一条警告，并且只会运行第二组命令。

```Makefile
all: blah

blah::
    echo "hello"

blah::
    echo "hello again"
```

## Commands and execution

### Command Echoing/Silencing
Add an @ before a command to stop it from being printed
You can also run make with -s to add an @ before each line

```Makefile
all:
    @echo "This make line will not be printed"
    echo "But this will"
```

### Command Execution
Each command is run in a new shell (or at least the effect is as such)

```Makefile
all:
    cd ..
    # The cd above does not affect this line, because each command is effectively run in a new shell
    echo `pwd`

    # This cd command affects the next because they are on the same line
    cd ..;echo `pwd`

    # Same as above
    cd ..; \
    echo `pwd`
```

### Default Shell
The default shell is /bin/sh. You can change this by changing the variable SHELL:

```Makefile
SHELL=/bin/bash

cool:
    echo "Hello from bash"
```

### Error handling with -k, -i, and -
+ Add -k when running make to continue running even in the face of errors. Helpful if you want to see all the errors of Make at once.
+ Add a - before a command to suppress the error
+ Add -i to make to have this happen for every command.

+ 运行 make 时添加 -k，即使遇到错误也可以继续运行。如果您想同时查看 Make 的所有错误，这将非常有用。
+ 在命令前添加 - 以抑制错误
+ 添加 -i 以使每个命令都发生这种情况。


```Makefile
one:
    # This error will be printed but ignored, and make will continue to run
	-false
	touch one
```

### Interrupting or killing make
Note only: If you ```ctrl + c``` make, it will delete the newer targets it just made.

### Recursive use of make
> To recursively call a makefile, use the special $(MAKE) instead of make because it will pass the make flags for you and won't itself be affected by them.

要递归调用 makefile，请使用特殊的 $(MAKE) 而不是 make，因为它会为您传递 make 标志，并且自身不会受到它们的影响。

```Makefile
new_contents = "hello:\n\ttouch inside_file"
all:
	mkdir -p subdir
	printf $(new_contents) | sed -e 's/^ //' > subdir/makefile
	cd subdir && $(MAKE)

clean:
	rm -rf subdir
```

### Use export for recursive make
> The export directive takes a variable and makes it accessible to sub-make commands. In this example, cooly is exported such that the makefile in subdir can use it.

export 指令接受一个变量，并使子 make 命令可以访问该变量。在本例中，将导出 cooly，以便subdir 中的 makefile 可以使用它。

> Note: export has the same syntax as sh, but they aren't related (although similar in function)

注意：export 的语法与 sh 相同，但它们之间没有关联（尽管功能类似）

```Makefile
new_contents = "hello:\n\\techo \$$(cooly)"

all:
	mkdir -p subdir
	echo $(new_contents) | sed -e 's/^ //' > subdir/makefile
	@echo "---MAKEFILE CONTENTS---"
	@cd subdir && cat makefile
	@echo "---END MAKEFILE CONTENTS---"
	cd subdir && $(MAKE)

# Note that variables and exports. They are set/affected globally.
cooly = "The subdirectory can see me!"
export cooly
# This would nullify the line above: unexport cooly

clean:
	rm -rf subdir
```

You need to export variables to have them run in the shell as well.

```Makefile
one=this will only work locally
export two=we can run subcommands with this

all: 
	@echo $(one)
	@echo $$one
	@echo $(two)
	@echo $$two
```

.EXPORT_ALL_VARIABLES exports all variables for you.

```Makefile
.EXPORT_ALL_VARIABLES:
new_contents = "hello:\n\techo \$$(cooly)"

cooly = "The subdirectory can see me!"
# This would nullify the line above: unexport cooly

all:
	mkdir -p subdir
	echo $(new_contents) | sed -e 's/^ //' > subdir/makefile
	@echo "---MAKEFILE CONTENTS---"
	@cd subdir && cat makefile
	@echo "---END MAKEFILE CONTENTS---"
	cd subdir && $(MAKE)

clean:
	rm -rf subdir
```

### Arguments to make
There's a nice list of options that can be run from make. Check out --dry-run, --touch, --old-file.

You can have multiple targets to make, i.e. make clean run test runs the clean goal, then run, and then test.

## Variables Pt.2

### Flavors and modification
There are two flavors of variables:
+ recursive (use =) - only looks for the variables when the command is used, not when it's defined.
+ simply expanded (use :=) - like normal imperative programming -- only those defined so far get expanded

```Makefile
# Recursive variable. This will print "later" below
one = one ${later_variable}
# Simply expanded variable. This will not print "later" below
two := two ${later_variable}

later_variable = later

all: 
	echo $(one)
	echo $(two)
```

> Simply expanded (using :=) allows you to append to a variable. Recursive definitions will give an infinite loop error.

简单扩展（使用：=）允许您附加到变量。递归定义将产生无限循环错误。

```Makefile
one = hello
# one gets defined as a simply expanded variable (:=) and thus can handle appending
one := ${one} there

all: 
	echo $(one)
```

?= only sets variables if they have not yet been set

```Makefile
one = hello
one ?= will not be set
two ?= will be set

all: 
	echo $(one)
	echo $(two)
```

> Spaces at the end of a line are not stripped, but those at the start are. To make a variable with a single space, use $(nullstring)

行末端的空间不会被剥离，但开头的空间会被剥离。要使用单个空格生成变量，请使用 $（nullstring）

```Makefile
with_spaces = hello   # with_spaces has many spaces after "hello"
after = $(with_spaces)there

nullstring =
space = $(nullstring) # Make a variable with a single space.

all: 
	echo "$(after)"
	echo start"$(space)"end
```

An undefined variable is actually an empty string!

```Makefile
all: 
	# Undefined variables are just empty strings!
	echo $(nowhere)
```

Use += to append

```Makefile
foo := start
foo += more

all: 
	echo $(foo)
```

[String Substitution](#string-substitution) is also a really common and useful way to modify variables. Also check out Text Functions and Filename Functions.

### Command line arguments and override
You can override variables that come from the command line by using override. Here we ran make with `make option_one=hi`

可以通过使用 override 覆盖来自命令行的变量。

```Makefile
# Overrides command line arguments
override option_one = did_override
# Does not override command line arguments
option_two = not_override
all: 
	echo $(option_one)
	echo $(option_two)
```

### List of commands and define
> "define" is actually just a list of commands. It has nothing to do with being a function. Note here that it's a bit different than having a semi-colon between commands, because each is run in a separate shell, as expected.

“define” 实际上只是一个命令列表。它与函数无关。请注意，这与在命令之间使用分号有点不同，因为正如预期的那样，每个命令都在单独的 shell 中运行。

```Makefile
one = export blah="I was set!"; echo $$blah

define two
export blah=set
echo $$blah
endef

# One and two are different.

all: 
	@echo "This prints 'I was set'"
	@$(one)
	@echo "This does not print 'I was set' because each command runs in a separate shell"
	@$(two)

```

### Target-specific variables
Variables can be assigned for specific targets

```Makefile
all: one = cool

all: 
	echo one is defined: $(one)

other:
	echo one is nothing: $(one)
```

### Pattern-specfic variables
You can assign variables for specific target patterns

```Makefile
%.c: one = cool

blah.c: 
	echo one is defined: $(one)

other:
	echo one is nothing: $(one)
```

## Conditional part of Makefiles

### Conditional if/else
```Makefile
foo = ok

all:
ifeq ($(foo), ok)
	echo "foo equals ok"
else
	echo "nope"
endif
```

### Check if a variable is empty

```Makefile
nullstring =
foo = $(nullstring) # end of line; there is a space here

all:
ifeq ($(strip $(foo)),)
	echo "foo is empty after being stripped"
endif
ifeq ($(nullstring),)
	echo "nullstring doesn't even have spaces"
endif
```

### Check if a variable is defined
ifdef does not expand variable references; it just sees if something is defined at all

```Makefile
bar =
foo = $(bar)

all:
ifdef foo
	echo "foo is defined"
endif
ifdef bar
	echo "but bar is not"
endif
```

### $(makeflags)
> This example shows you how to test make flags with findstring and MAKEFLAGS. Run this example with make -i to see it print out the echo statement.

此示例演示如何使用 findstring 和 MAKEFLAGS 测试 make 标志。用 make -i 运行这个例子，看它打印出 echo 语句。

```Makefile
bar =
foo = $(bar)

all:
# Search for the "-i" flag. MAKEFLAGS is just a list of single characters, one per flag. So look for "i" in this case.
ifneq (,$(findstring i, $(MAKEFLAGS)))
	echo "i was passed to MAKEFLAGS"
endif
```

## Functions

### First Functions
Functions are mainly just for text processing. Call functions with $(fn, arguments) or ${fn, arguments}. You can make your own using the call builtin function. Make has a decent amount of builtin functions.

函数主要用于文本处理。使用 $（fn，参数）或 ${fn，参数} 调用函数。您可以使用 call builtin function 自行创建。

```Makefile
bar := ${subst not, totally, "I am not superman"}
all: 
	@echo $(bar)
```

If you want to replace spaces or commas, use variables

```Makefile
comma := ,
empty:=
space := $(empty) $(empty)
foo := a b c
bar := $(subst $(space),$(comma),$(foo))

all: 
	@echo $(bar)
```

Do NOT include spaces in the arguments after the first. That will be seen as part of the string.

```Makefile
comma := ,
empty:=
space := $(empty) $(empty)
foo := a b c
bar := $(subst $(space), $(comma) , $(foo))

all: 
	# Output is ", a , b , c". Notice the spaces introduced
	@echo $(bar)
```

### String Substitution
`$(patsubst pattern,replacement,text)` does the following:

> "Finds whitespace-separated words in text that match pattern and replaces them with replacement. Here pattern may contain a ‘%’ which acts as a wildcard, matching any number of any characters within a word. If replacement also contains a ‘%’, the ‘%’ is replaced by the text that matched the ‘%’ in pattern. Only the first ‘%’ in the pattern and replacement is treated this way; any subsequent ‘%’ is unchanged." (GNU docs)

“在与模式匹配的文本中查找以空格分隔的单词，并将其替换为替换。此处模式可能包含一个 “%”，它充当通配符，匹配单词中任意数量的字符。如果替换也包含 “%”，则 “%” 将替换为与模式中 “%” 匹配的文本。只有模式和替换中的第一个 “%” 被这样处理；任何后续 “%” 都将保持不变。“（GNU 文件）

> The substitution reference `$(text:pattern=replacement)` is a shorthand for this.

替换引用 $（text:pattern=replacement）是这方面的缩写。

> There's another shorthand that that replaces only suffixes: `$(text:suffix=replacement)`. No `%` wildcard is used here.

还有一种只替换后缀的速记法：$(text:suffix=replacement）。此处未使用 % 通配符。

> Note: don't add extra spaces for this shorthand. It will be seen as a search or replacement term.

注意：不要为这个速记添加额外的空格。它将被视为搜索或替换术语。

```Makefile
foo := a.o b.o l.a c.o
one := $(patsubst %.o,%.c,$(foo))
# This is a shorthand for the above
two := $(foo:%.o=%.c)
# This is the suffix-only shorthand, and is also equivalent to the above.
three := $(foo:.o=.c)

all:
	echo $(one)
	echo $(two)
	echo $(three)
```

### The foreach function
> The foreach function looks like this: $(foreach var,list,text). It converts one list of words (separated by spaces) to another. var is set to each word in list, and text is expanded for each word.

foreach 函数如下所示：$(foreach 变量、列表、文本）。它将一个单词列表（用空格分隔）转换为另一个。var 被设置为列表中的每个单词，文本被展开为每个单词。

This appends an exclamation after each word:

```Makefile
foo := who are you
# For each "word" in foo, output that same word with an exclamation after
bar := $(foreach wrd,$(foo),$(wrd)!)

all:
	# Output is "who! are! you!"
	@echo $(bar)
```

### The if function
`if` checks if the first argument is nonempty. If so runs the second argument, otherwise runs the third.

```Makefile
foo := $(if this-is-not-empty,then!,else!)
empty :=
bar := $(if $(empty),then!,else!)

all:
	@echo $(foo)
	@echo $(bar)
```

### The call function
Make supports creating basic functions. You "define" the function just by creating a variable, but use the parameters $(0), $(1), etc. You then call the function with the special call function. The syntax is $(call variable,param,param). $(0) is the variable, while $(1), $(2), etc. are the params.

```Makefile
sweet_new_fn = Variable Name: $(0) First: $(1) Second: $(2) Empty Variable: $(3)

all:
	# Outputs "Variable Name: sweet_new_fn First: go Second: tigers Empty Variable:"
	@echo $(call sweet_new_fn, go, tigers)
```

### The shell function
shell - This calls the shell, but it replaces newlines with spaces!

```Makefile
all: 
	@echo $(shell ls -la) # Very ugly because the newlines are gone!
```
## Other Features

### Include Makefiles
The include directive tells make to read one or more other makefiles. It's a line in the makefile makefile that looks like this:

include 指令告诉 make 读取一个或多个其他 makefile。makefile makefile 中的一行如下所示：

```Makefile
include filenames...
```

This is particularly useful when you use compiler flags like -M that create Makefiles based on the source. For example, if some c files includes a header, that header will be added to a Makefile that's written by gcc. I talk about this more in the Makefile Cookbook

当使用编译器标志（如 -M）基于源代码创建 makefile 时，这一点尤其有用。例如，如果一些 c 文件包含一个头文件，该头文件将被添加到由 gcc 编写的 Makefile 中。

### The vpath Directive
Use vpath to specify where some set of prerequisites exist. The format is ```vpath <pattern> <directories, space/colon separated>
<pattern>``` can have a `%`, which matches any zero or more characters.
You can also do this globallyish with the variable VPATH

```Makefile
vpath %.h ../headers ../other-directory

some_binary: ../headers blah.h
	touch some_binary

../headers:
	mkdir ../headers

blah.h:
	touch ../headers/blah.h

clean:
	rm -rf ../headers
	rm -f some_binary
```

### Multiline
The backslash ("\") character gives us the ability to use multiple lines when the commands are too long

```Makefile
some_file: 
	echo This line is too long, so \
		it is broken up into multiple lines
```

### .phony
> Adding .PHONY to a target will prevent make from confusing the phony target with a file name. In this example, if the file clean is created, make clean will still be run. .PHONY is great to use, but I'll skip it in the rest of the examples for simplicity.

添加 .PHONY 目标将防止 make 将假冒目标与文件名混淆。在本例中，如果创建了文件 clean，make clean 仍将运行。PHONY 很好用，但为了简单起见，我将在剩下的示例中跳过它。

```Makefile
some_file:
	touch some_file
	touch clean

.PHONY: clean
clean:
	rm -f some_file
	rm -f clean
```

### .delte_on_error
> The make tool will stop running a rule (and will propogate back to prerequisites) if a command returns a nonzero exit status. DELETE_ON_ERROR will delete the target of a rule if the rule fails in this manner. This will happen for all targets, not just the one it is before like PHONY. It's a good idea to always use this, even though make does not for historical reasons.

如果命令返回非零退出状态，make 工具将停止运行规则（并将传播回先决条件）。
如果规则以这种方式失败，DELETE_ON_ERROR 将删除规则的目标。这种情况会发生在所有目标身上，而不仅仅是以前的目标，比如假目标。始终使用此选项是个好主意，即使 make 不是出于历史原因。

```Makefile
.DELETE_ON_ERROR:
all: one two

one:
	touch one
	false

two:
	touch two
	false
```

## Makefile Cookbook
> Let's go through a really juicy Make example that works well for medium sized projects.

让我们来看一个非常有趣的 Make 示例，它适用于中型项目。

> The neat thing about this makefile is it automatically determines dependencies for you. All you have to do is put your C/C++ files in the src/ folder.

这个 makefile 最巧妙的地方是它会自动为您确定依赖关系。你所要做的就是把你的 C/C++ 文件放在 src/ 文件夹中。

```Makefile
# Thanks to Job Vranish (https://spin.atomicobject.com/2016/08/26/makefile-c-projects/)
TARGET_EXEC := final_program

BUILD_DIR := ./build
SRC_DIRS := ./src

# Find all the C and C++ files we want to compile
# Note the single quotes around the * expressions. Make will incorrectly expand these otherwise.
SRCS := $(shell find $(SRC_DIRS) -name '*.cpp' -or -name '*.c' -or -name '*.s')

# String substitution for every C/C++ file.
# As an example, hello.cpp turns into ./build/hello.cpp.o
OBJS := $(SRCS:%=$(BUILD_DIR)/%.o)

# String substitution (suffix version without %).
# As an example, ./build/hello.cpp.o turns into ./build/hello.cpp.d
DEPS := $(OBJS:.o=.d)

# Every folder in ./src will need to be passed to GCC so that it can find header files
INC_DIRS := $(shell find $(SRC_DIRS) -type d)
# Add a prefix to INC_DIRS. So moduleA would become -ImoduleA. GCC understands this -I flag
INC_FLAGS := $(addprefix -I,$(INC_DIRS))

# The -MMD and -MP flags together generate Makefiles for us!
# These files will have .d instead of .o as the output.
CPPFLAGS := $(INC_FLAGS) -MMD -MP

# The final build step.
$(BUILD_DIR)/$(TARGET_EXEC): $(OBJS)
	$(CC) $(OBJS) -o $@ $(LDFLAGS)

# Build step for C source
$(BUILD_DIR)/%.c.o: %.c
	mkdir -p $(dir $@)
	$(CC) $(CPPFLAGS) $(CFLAGS) -c $< -o $@

# Build step for C++ source
$(BUILD_DIR)/%.cpp.o: %.cpp
	mkdir -p $(dir $@)
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) -c $< -o $@


.PHONY: clean
clean:
	rm -r $(BUILD_DIR)

# Include the .d makefiles. The - at the front suppresses the errors of missing
# Makefiles. Initially, all the .d files will be missing, and we don't want those
# errors to show up.
-include $(DEPS)
```