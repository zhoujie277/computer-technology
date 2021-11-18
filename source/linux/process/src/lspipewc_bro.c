#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>
#include <stdlib.h>

void sys_error(const char *str)
{
    perror(str);
    exit(1);
}

void wc(int fd)
{
    dup2(fd, STDIN_FILENO);
    execlp("wc", "wc", "-l", NULL);
}

void ls(int fd)
{
    dup2(fd, STDOUT_FILENO);
    execlp("ls", "ls", "-l", "-h", NULL);
}

/**
 * 练习：
 * 兄弟进程通信
 * 实现 ls | wc -l 的效果
 * 技术限定：fork/exec/dup2/
 * 父进程等待所有子进程退出。
 */
int main()
{
    int fd[2];
    int ret = pipe(fd);
    if (ret < 0)
    {
        sys_error("pipe error");
    }
    int i = 0;
    for (; i < 2; i++)
    {
        pid_t pid = fork();
        if (pid == 0)
        {
            break;
        }
    }
    if (i == 0)
    {
        close(fd[0]);
        ls(fd[1]);
    }
    else if (i == 1)
    {
        close(fd[1]);
        wc(fd[0]);
    }
    else if (i == 2)
    {
        close(fd[0]);
        close(fd[1]);
        for (int i = 0; i < 2; i++)
        {
            // 等待任意一个子进程结束a
            // int ret = waitpid(-1, NULL, 0);
            wait(NULL);
        }
        write(STDOUT_FILENO, "execute over", 12);
    }

    return 0;
}