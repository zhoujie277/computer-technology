#include <stdio.h>
#include "mymath.h"

/**
 * 静态库的使用示例
 * 1. 编译源文件
 **   gcc -c add.c -o add.o
 **   gcc -c sub.c -o sub.o
 * 2. 制作静态库
 * *  ar rcs lib库名.a add.o sub.o
 **  对参数的说明：
 **  参数 r 用来替换库中已有的目标文件，或者加入新的目标文件。
 **  参数 c 表示创建一个库。不管库否存在，都将创建。　
 **  参数 s 用来创建目标文件索引，这在创建较大的库时能提高速度。
 * 3。引用库文件
 **   引用头文件 "libmath.h"
 **   编译包含库文件。gcc static_library.c libmath.o -o a.out
 */
int main(int argc, char *argv[])
{
    int a = 10, b = 4;
    int c = add(a, b);
    int d = sub(a, b);
    printf("%d + %d = %d \n", a, b, c);
    printf("%d - %d = %d \n", a, b, d);
    return 0;
}