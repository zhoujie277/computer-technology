#include <stddef.h>

/**
 * 字节对齐的演示
 * 默认 4 字节对齐
 */
struct data1
{
    char a1;
    char a2;
    int a3;
};

struct data2
{
    char a1;
    int a3;
    char a2;
};

struct data3
{
    int pos;
    char path[107];
};

int main(int argc, char const *argv[])
{  
    int a = offsetof(struct data1, a3);
    int b = offsetof(struct data2, a2);
    int e = offsetof(struct data3, path);
    int c = sizeof(struct data1);
    int d = sizeof(struct data2);
    int f = sizeof(struct data3);
    return 0;
}
