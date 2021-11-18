#include <unistd.h>
#include <stdio.h>

// ps -A -o stat,ppid,pid,cmd | grep -e '^[Zz]'
// 查看僵尸进程命令
int main(int argc, char *argv[])
{
    int var = 0;
    int i = 0;
    for (; i < 5; i++)
    {
        int fid = fork();
        if (fid == 0)
        {
            if (i == 2)
                var = getpid();
            break;
        }
    }

    if (i == 5)
    {
        for (int i = 0; i < 30; i++)
        {
            printf("I'm parent process. pid=%d, var=%d \n", getpid(), var);
            sleep(1);
        }
    }
    else
    {
        for (int i = 0; i < 3; i++)
        {
            printf("I'm child process..process, pid=%d, ppid=%d, var=%d \n", getpid(), getppid(), var);
            sleep(1);
        }
    }

    return 0;
}