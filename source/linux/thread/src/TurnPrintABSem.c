#include <stdio.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include <semaphore.h>

int MAX_COUNT = 50;

sem_t aSem;
sem_t bSem;

void *onACreated(void *arg)
{
    int i = 0;
    while (i < MAX_COUNT)
    {
        sem_wait(&aSem);
        putchar('A');
        i++;
        sem_post(&bSem);
    }

    return NULL;
}

void *onBCreated(void *arg)
{
    int i = 0;
    while (i < MAX_COUNT)
    {
        sem_wait(&bSem);
        putchar('B');
        i++;
        sem_post(&aSem);
    }
    return NULL;
}

/**
 * 两个线程交替在屏幕输出A和B。
 * sem_init 在最新的 macOS 上已被废弃
 */
int main()
{    
    int ret = sem_init(&aSem, 0, 1);
    if (ret != 0)
    {
        fprintf(stderr, "sem_init a error %s", strerror(ret));
        exit(1);
    }
    ret = sem_init(&bSem, 0, 0);
    if (ret != 0)
    {
        fprintf(stderr, "sem_init b error %s", strerror(ret));
        exit(1);
    }
    pthread_t a, b;
    ret = pthread_create(&a, NULL, onACreated, NULL);
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
    return 0;
}