#include <stdio.h>


int max(int a, int b) {
    return a >= b ? a: b;
}

int main() {
    int (*p)(int, int) = max;
    int result = p(6, 4);
    int result1 = (*p)(2, 4);
    printf("max value is %d, %d", result, result1);
    return 0;
}