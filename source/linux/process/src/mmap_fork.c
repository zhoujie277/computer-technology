#include <sys/mman.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/wait.h>

/**
 * 进程间通信
 * 内存映射 mmap 实现父子进程通信的示例
 */
int global_var = 200;
int main(int argc, char *argv[])
{
    int fd = open("./resource/mmap", O_RDWR | O_CREAT | O_TRUNC);
    if (fd == -1)
    {
        perror("open error");
        return 1;
    }

    int ret = ftruncate(fd, 10);
    if (ret == -1)
    {
        perror("ftruncate error");
        return 2;
    }
    int len = lseek(fd, 0, SEEK_END);

    int *p = mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (p == MAP_FAILED)
    {
        perror("mmap error");
        return 3;
    }

    close(fd);

    int pid = fork();
    if (pid == 0)
    {
        *p = 2000;
        global_var = 400;
        printf("child process write msg, *p=%d, var=%d \n", *p, global_var);
    }
    else
    {
        wait(NULL);
        printf("parent process write msg, *p=%d, var=%d \n", *p, global_var);

        int iret = munmap(p, len);
        if (iret == -1)
        {
            perror("munmap error");
            return 4;
        }
    }

    return 0;
}