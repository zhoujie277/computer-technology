#include <sys/mman.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>

/**
 * 进程间通信
 * 使用匿名内存映射 mmap 实现父子进程通信的示例
 */
int global_var = 200;
int main(int argc, char *argv[])
{
    int *p = mmap(NULL, 10, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    if (p == MAP_FAILED)
    {
        perror("mmap error");
        return 3;
    }

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

        int iret = munmap(p, 10);
        if (iret == -1)
        {
            perror("munmap error");
            return 4;
        }
    }

    return 0;
}