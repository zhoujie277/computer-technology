#include <stdio.h>

int add(int a, int b);
int sub(int a, int c);

int main(int argc, char *argv[])
{
    int a = 10, b = 5;
    int c = add(a, b);
    int d = sub(a, b);
    printf("%d + %d = %d \n", a, b, c);
    printf("%d - %d = %d \n", a, b, d);
    return 0;
}