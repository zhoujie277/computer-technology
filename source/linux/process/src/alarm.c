#include <unistd.h>
#include <stdio.h>

int main()
{
    alarm(1);
    long i = 0;
    while (1)
    {
        i++;
        printf("i=%lu \n", i);
    }
    printf("end...");
    return 0;
}