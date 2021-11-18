#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
/**
 * 写一个程序，将当前进程信息重定向到文件中。
 */
int main(int argc, char *argv[])
{
    pid_t p = fork();
    if (p == -1)
    {
        perror("fork error");
        return (1);
    }
    else if (p == 0)
    {
        int fd = open(argv[1], O_RDWR | O_CREAT | O_TRUNC);
        if (fd == -1) 
        {
            perror("open error");
            return 1;
        }
        dup2(fd, STDOUT_FILENO);
        execlp("ps", "ps", "aux", NULL);
        perror("child exec error");
    }
    else
    {
        sleep(1);
        printf("I'm parent process....");
    }
    return 0;
}