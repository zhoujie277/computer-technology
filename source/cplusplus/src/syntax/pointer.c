#include <stdio.h>

/*
    野指针：
    1、指针指向一个不是自己申请的内存空间，比如如下代码：指向一个内存编号为4的地址。
    2、或者指向一块被释放了的内存。如下代码：指向一块刚刚被free过的内存。
*/
void fieldPointer()
{
    int *p = 0x4;
    int *p1 = (int *)malloc(4);
    free(p1);
    p1 = NULL; // 释放之后更改p1指向，以防p1指向一块被释放的内存，成为野指针。
}
