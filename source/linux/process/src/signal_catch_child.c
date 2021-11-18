#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>

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

// TODO: 此处有一个奇怪的问题，待分析
// 正常启动程序后，
// + 正常 kill 子进程，这里会收到回调，并且能处理僵尸进程。
// + 但是，如果第一次直接shell给父进程发信号 kill -17 pid, 会有回调，但后面再杀子进程，父进程将不会再处理信号回调处理函数。
void onChildStateChanged(int signo)
{
    pid_t pid;
    while ((pid = waitpid(-1, NULL, WNOHANG)) > 0)
    // while ((pid = wait(NULL)) != -1)
    {
        printf("the child process was recycled, pid=%d getpid=%d\n", pid, getpid());
    }
    // perror("wait error");
    printf("wait end \n");
}

/**
 * 捕获子进程状态变化信号的功能演示。
 */
int main()
{
    // fork 子进程之前，设置屏蔽信号字，
    // 防止出现僵尸进程。这种现象产生的原因是：
    // 子进程刚创建出来获得CPU时间，而父进程还没来得及执行注册 SIGCHLD
    // 由于没有注册，而子进程的信号已被忽略，导致父进程没有执行过处理函数。

    // 解决方法：设置屏蔽信号字，使信号保留给父进程处理。
    // TODO: 此程序在macos上运行有问题：
    // 在父进程sleep之后，查看未决信号集，没有找到子进程发送的CHLD信号
    // 用另外一种解决方案更合适：在fork之前注册信号。子进程fork之后取消注册，父进程保留
    
    // 该程序在linux下面运行正常。
    sigset_t set, pset;
    sigemptyset(&set);
    sigaddset(&set, SIGINT);
    sigaddset(&set, SIGCHLD);
    int ret = sigprocmask(SIG_BLOCK, &set, NULL);
    if (ret == -1)
    {
        perror("sigprocmask error");
        return 2;
    }
    printSignelSet(&set);
     
    pid_t pid = 0;
    int i = 0;
    for (; i < 5; i++)
    {
        pid = fork();
        if (pid == 0)
            break;
        if (pid == -1)
        {
            perror("fork error");
            return 1;
        }
    }
    if (i == 5)
    {
        // sleep(1);
        sigpending(&pset);
        printSignelSet(&pset);
        struct sigaction act;
        // act.__sigaction_u.__sa_handler = onChildStateChanged;
        act.sa_handler = onChildStateChanged; // linux下用这个
        sigemptyset(&act.sa_mask);
        sigaction(SIGCHLD, &act, NULL);
        // 恢复屏蔽信号字
        sigprocmask(SIG_UNBLOCK, &set, NULL);

        printf("I'm parent process, will handle child state,pid=%d \n", getpid());
        // sleep(10);
        while (1) ;

        printf("parent over\n");
    }
    else
    {
        // sigprocmask(SIG_UNBLOCK, &set, NULL);
        while (1);
        
        printf("I'm child process, pid = %d \n", getpid());
    }
}
