#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

struct var_param
{
    int var;
    char *str;
};

// void *on_thread_created(void *arg)
// {
//     struct var_param *p;
//     p = malloc(sizeof(p));
//     p->str = "hello world";
//     p->var = 200;
//     return (void *)p;
// }

void *on_thread_created(void *arg)
{
    struct var_param *p = (struct var_param *)arg;
    p->str = "hello world";
    p->var = 200;
    return (void*)p;
}

int main()
{
     struct var_param pstr;
    struct var_param *p;
    pthread_t tid;
    int ret = pthread_create(&tid, NULL, on_thread_created, (void *)&pstr);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join_error %s \n", strerror(ret));
        exit(1);
    }
    ret = pthread_join(tid, (void **)&p);
    if (ret != 0)
    {
        fprintf(stderr, "pthread_join_error %s \n", strerror(ret));
        exit(1);
    }
    printf("obtain subthread param, %d, %s \n", p->var, p->str);
    return 0;
}