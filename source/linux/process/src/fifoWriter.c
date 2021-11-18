#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>

/**
 * 命名管道 
 * 可用于任意两个进程的通信。
 */

int main()
{
    int fd = open("./resource/fifo", O_WRONLY);
    char buf[256];
    for (int i = 0; i < 10; i++)
    {
        sprintf(buf, "hello world %d\n", i);
        write(fd, buf, strlen(buf));
        sleep(1);
    }
}