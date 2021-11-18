#include <stdio.h>
#include "mymath.h"
/**
 * 动态库的使用演示
 * 1. 编译源文件，并使用 -fPIC 生成与位置无关的代码
 **   gcc -c add.c -o add.o -fPIC
 **   gcc -c sub.c -o sub.o -fPIC
 * 2. 使用 gcc -shared 制作动态库
 **   gcc -shared -o lib库名.so add.o sub.o
 * 3. 编译可执行文件时，指定所使用的的动态库。
 **     -l: 指定库名  -L: 指定库路径
 **   gcc dynamic_library.c -o a.out -lmymath -L ./lib
 * 4. 运行可执行文件 ./a.out
 **     命令行当前工作路径若不在libray目录下直接执行，会报错。原因是：
 **        链接器：工作于链接阶段，需要 -l 和 -L 参数指定。
 **     动态连接器：工作于程序运行阶段，工作时需要提供动态库所在目录位置。
 ** linux 下用 ldd，macOS 下用 otool -L a.out 可查看动态链接库的相关链接信息。
 ** 发现链接的是相对目录，所以必须在指定目录下运行，才不会报错。
 */
int main(int argc, char *argv[]) 
{
    int a = 10, b = 4;
    int c = add(a, b);
    int d = sub(a, b);
    printf("%d + %d = %d \n", a, b, c);
    printf("%d - %d = %d \n", a, b, d);
    return 0;
    return 0;
}