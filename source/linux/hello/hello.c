#include <stdio.h>

int sub(int c)
{
    int a = 10;
    int b = 5;
    int d = 20;
    int f = 32;
    return a - b + c + f - d;
}

int main()
{
    int d = sub(2);
    printf("hello, linux, %d", d);
    return 1;
}