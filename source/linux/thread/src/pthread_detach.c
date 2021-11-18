#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

void *fn(void *arg)
{
    printf("sub thread: pid = %d, tid = %lu \n", getpid(), pthread_self()->__sig);
    return NULL;
}

int main()
{
    pthread_t tid;
    int ret = pthread_create(&tid, NULL, fn, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_create_error %s \n", strerror(ret));
        exit(1);
    }

    ret = pthread_detach(tid);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_detach_error %s \n", strerror(ret));
        exit(1);
    }
    printf("main thread: pid = %d, tid = %lu \n", getpid(), pthread_self()->__sig);
    
    // detach 的线程不可join，此处可演示错误
    ret = pthread_join(tid, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join_error %s \n", strerror(ret));
        exit(1);
    }

    pthread_exit(0);
}