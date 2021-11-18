#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>

/**
 * 匿名管道通信
 * fd[0] 读端
 * fd[1] 写端
 * 
 * fork 子进程创建之后，共享父进程的文件描述符资源。
 */
int main()
{
    int ret;
    int fd[2];
    ret = pipe(fd);
    if (ret < 0)
    {
        perror("create pipe error");
        return 1;
    }

    int pid = fork();
    if (pid > 0)
    {
        char buf[] = "hello world\n";
        // printf("sizeof(buf)=%lu\n", sizeof(buf));
        // write(fd[1], buf, sizeof(buf));
        char readbuf[256];
        int wc = read(fd[0], readbuf, sizeof(readbuf));
        write(STDOUT_FILENO, readbuf, wc);
        int ret = wait(NULL);
        printf("parent process %d will die, the child pid is %d \n", getpid(), pid);
    }
    else if (pid == 0)
    {
        // char buf[1024];
        // int c = read(fd[0], buf, sizeof(buf));
        // write(STDOUT_FILENO, buf, c);
        char writebuf[] = "you are welcome\n";
        write(fd[1], writebuf, sizeof(writebuf));
    }
    else
    {
        perror("fork error");
    }
    return 0;
}