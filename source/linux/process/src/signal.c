#include <signal.h>
#include <stdio.h>

/**
 * 遗留问题，如果信号处理阶段，又产生信号时且没有设置屏蔽，怎么办？
 * 1. 信号产生时，用户程序有定义处理函数时，默认会进行两次进入内核，两次出内核的情况
 *  + 第一次进入内核，是因为信号产生的时候实际是产生了一次中断，通过门进入了内核。
 *  + 第一次从内核返回用户空间时，是因为信号产生并且需要处理的时候，内核远返回了用户注册的回调函数。
 *  + 第二次进入内核，是因为用户定义的信号处理函数低啊用完毕后，系统默认通过sigreturn系统调用又再次进入到了内核。
 *  + 第二次从内核回到用户空间时，是因为信号处理完毕，紧接着因信号产生而中断的代码段指令继续往下执行。
 */

void onSignal(int signo)
{
    printf("catch signal, %d \n", signo);
}

/**
 * 演示捕获信号的处理函数
 */
int main()
{
    signal(SIGQUIT, onSignal);
    signal(SIGINT, onSignal);
    printf("begin catch signal..");
    while (1);
}