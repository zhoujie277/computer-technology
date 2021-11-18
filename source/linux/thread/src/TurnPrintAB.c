#include <stdio.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>

int MAX_COUNT = 50;
int turn = 1;
char buf[120];

void *onACreated(void *arg)
{
    int i = 0;
    while (i < MAX_COUNT)
    {
        while (turn == 0);
        // putchar('A');
        buf[i * 2] = 'A';
        turn = 0;
        i++;
    }

    return NULL;
}

void *onBCreated(void *arg)
{
    int i = 0;
    while (i < MAX_COUNT)
    {
        while (turn == 1);
        // putchar('B');
        buf[i * 2 + 1] = 'B';
        turn = 1;
        i++;
    }
    return NULL;
}

/**
 * 两个线程交替在屏幕输出A和B。
 * 
 * 这个程序的正确性值得商榷。
 * 问题在于：这个程序的输出能不能保证内存可见性？
 * 比如，A 线程修改了 turn 的值，B 线程会不会一直读不到，然后一直忙等。
 */
int main()
{
    pthread_t a, b;
    int ret = pthread_create(&a, NULL, onACreated, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_create a error %s", strerror(ret));
        exit(1);
    }

    ret = pthread_create(&b, NULL, onBCreated, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_create b error %s", strerror(ret));
        exit(1);
    }

    ret = pthread_join(a, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join a error %s", strerror(ret));
        exit(1);
    }

    ret = pthread_join(b, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join b error %s", strerror(ret));
        exit(1);
    }
    printf("%s", buf);
    return 0;
}