#include <pthread.h>
#include <stdio.h>
#include <unistd.h>


void* on_thread_created(void* arg)
{
    printf("on_thread_created pid=%d, tid=%lu \n", getpid(), pthread_self()->__sig);
    return NULL;
}

int main()
{
    pthread_t tid;
    int ret = pthread_create(&tid, NULL, on_thread_created, NULL);
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