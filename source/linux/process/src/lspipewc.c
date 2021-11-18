#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>
#include <fcntl.h>

void redirectRead()
{
    int fd = open("./tmp", O_RDONLY);
    dup2(fd, STDIN_FILENO);
    execlp("wc", "wc", "-l", NULL);
}

void redirectWrite()
{
    int fd = open("./tmp", O_RDWR | O_CREAT | O_TRUNC);
    dup2(fd, STDOUT_FILENO);
    execlp("ls", "ls", "-l", "-h", NULL);
}

/**
 * 练习：
 * 父子进程通信
 * 实现 ls | wc -l 的效果
 * 技术限定：fork/exec/dup2/
 */
int main(int argc, char *argv[])
{
    int pfd[2];
    int ret = pipe(pfd);
    if (ret < 0)
    {
        perror("pipe error");
        return 1;
    }

    int pid = fork();
    if (pid > 0)
    {
        close(pfd[1]);
        dup2(pfd[0], STDIN_FILENO);
        execlp("wc", "wc", "-l", NULL);
        perror("error");
    }
    else if (pid == 0)
    {
        close(pfd[0]);
        dup2(pfd[1], STDOUT_FILENO);
        execlp("ls", "ls", "-l", "-h", NULL);
    }
    else
    {
        perror("fork error");
        return 2;
    }
    return 0;
}