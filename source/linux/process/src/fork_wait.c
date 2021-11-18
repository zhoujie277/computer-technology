#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>

int main(int argc, char *argv[])
{
    pid_t pid = fork();
    if (pid == -1)
    {
        perror("fork error");
        return 1;
    }
    else if (pid == 0)
    {
        for (int i = 0; i < 10; i++)
        {
            printf("hello, I'm child process...%d \n", getpid());
            sleep(2);
        }

        return 1;
    }
    else
    {
        // 父进程无限期等待子进程结束
        int status;
        int ret = wait(&status);
        printf("hi, I'm parent process... %d the childPid is=%d \n", getpid(), ret);
        
        if (WIFEXITED(status))
        {
            // 子进程自己终止进程
            printf("child process was kill by WIFEXITED status is %d \n", WEXITSTATUS(status));
        } else if (WIFSIGNALED(status))
        {
            // 被信号终止进程
            printf("child process was kill by WIFSIGNALED status is %d \n", WTERMSIG(status));
        }
    }
    return 0;
}