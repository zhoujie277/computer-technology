#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

void* on_thread_created(void* arg)
{
    printf("on_thread_created pid=%d, tid=%lu \n", getpid(), pthread_self()->__sig);
    return NULL;
}

int main()
{
    pthread_t tid;
    pthread_attr_t attr;
    int ret = pthread_attr_init(&attr);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_init_error %s \n", strerror(ret));
        exit(1);
    }
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    ret = pthread_create(&tid, &attr, on_thread_created, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join_error %s \n", strerror(ret));
        exit(1);
    }
    ret = pthread_attr_destroy(&attr);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_destroy_error %s \n", strerror(ret));
        exit(1);
    }

    // 测试是不是分离线程
    ret = pthread_join(tid, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join_error %s \n", strerror(ret));
        exit(1);
    }


    printf("main pid=%d, tid=%lu \n", getpid(), pthread_self()->__sig);
    // sleep(1);
    // 主线程退出，其他线程正在运行，则进程不会退出。
    // 只有所有进程结束之后，进程才退出。
    pthread_exit(NULL);
    return 0;
}