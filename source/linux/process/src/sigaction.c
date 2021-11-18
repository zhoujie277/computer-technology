#include <signal.h>
#include <stdio.h>

void sig_catch(int signo)
{
    printf("catch signal, ths no is %d \n", signo);
}

int main()
{
    struct sigaction act, oldact;
    act.__sigaction_u.__sa_handler = sig_catch; // 设置回调函数
    sigemptyset(&act.sa_mask);                  // 设置信号屏蔽字，这里清空sa_mask关键字，只在 sig_catch执行期间有效
                                                // sigaddset(&act.sa_mask, SIGQUIT);
                                                // 信号处理期间，屏蔽quit信号。
    act.sa_flags = 0;                           // 默认值，本信号自动屏蔽

    int ret = sigaction(SIGINT, &act, &oldact); //注册信号回调函数
    if (ret == -1)
    {
        perror("sigaction error");
    }

    while (1)
        ;
    return 0;
}