#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#define BOUNDS 20

struct Msg
{
    int msg;
};

struct Msg msgBuf[BOUNDS];
int size = 0;

pthread_mutex_t mutex;
pthread_cond_t pCond;
pthread_cond_t cCond;

void checkError(const char *info, int ret)
{
    if (ret != 0)
    {
        fprintf(stderr, "%s was error %s", info, strerror(ret));
        exit(1);
    }
}

void *producer(void *arg)
{
    int msgid = 1;
    while (1)
    {
        struct Msg msg;
        msg.msg = msgid++;
        pthread_mutex_lock(&mutex);
        while (size == BOUNDS)
        {
            pthread_cond_wait(&pCond, &mutex);
        }
        msgBuf[size++] = msg;
        pthread_cond_signal(&cCond);
        pthread_mutex_unlock(&mutex);
    }
    
    return NULL;
}

void *consumer(void *arg)
{
    while (1)
    {
        pthread_mutex_lock(&mutex);
        while (size == 0)
        {
            pthread_cond_wait(&cCond, &mutex);
        }
        struct Msg msg = msgBuf[--size];
        pthread_cond_signal(&pCond);
        pthread_mutex_unlock(&mutex);
        printf("consume msg %d \n", msg.msg);
    }
    return NULL;
}

/**
 * 有界生产者-消费者模型
 * pthread 库实现。
 */
int main()
{
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&pCond, NULL);
    pthread_cond_init(&cCond, NULL);

    int ret;
    pthread_t p, c;
    ret = pthread_create(&p, NULL, producer, NULL);
    checkError("pthread_create p", ret);

    ret = pthread_create(&c, NULL, consumer, NULL);
    checkError("pthread_create c", ret);

    ret = pthread_join(p, NULL);
    checkError("pthread_join p", ret);

    ret = pthread_join(c, NULL);
    checkError("pthread_join c", ret);

    return 0;
}