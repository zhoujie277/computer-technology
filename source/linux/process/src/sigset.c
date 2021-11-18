#include <signal.h>
#include <stdio.h>
#include <unistd.h>

void printSignelSet(const sigset_t *set)
{
    for (int i = 0; i < 32; i++)
    {
        if (sigismember(set, i))
        {
            putchar('1');
        }
        else
        {
            putchar('0');
        }
    }
    printf("\n");
}

/**
 * 屏蔽信号集功能演示
 * 该程序屏蔽了信号 2 的未决处理。
 */
int main()
{
    int ret = 0;
    sigset_t set, oldset, pset;
    sigemptyset(&set);
    sigaddset(&set, SIGINT);

    ret = sigprocmask(SIG_BLOCK, &set, &oldset);
    if (ret == -1)
    {
        perror("sigprocmask error");
        return 1;
    }

    while (1)
    {
        ret = sigpending(&pset);
        if (ret == -1)
        {
            perror("sigpending error");
        }
        printSignelSet(&pset);
        sleep(1);
    }
    return 1;
}