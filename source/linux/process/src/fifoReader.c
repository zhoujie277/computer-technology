#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>

/**
 * 命名管道 
 * 可用于任意两个进程的通信。
 * 可以进程自己写，自己读。
 * 全双工进程间通信
 */
void createfifo()
{
    int fd = mkfifo("./fifo", 0664);
    if (fd == -1)
    {
        perror("mkfifo error");
    }
}

int main()
{
    // 只读方式打开时，第一次read 会阻塞，后面有进程写数据之后，
    // 但是如果没有进程以写的方式打开，则会返回0，表示读到末尾
    int fd = open("./resource/fifo", O_RDONLY);
    char buf[256];
    int rc = 0;
    while ((rc = read(fd, buf, sizeof(buf))) != 0)
    {
        if (rc == -1)
        {
            perror("read error");
            return 1;
        }
        write(STDOUT_FILENO, buf, rc);
        // write(fd, buf, rc);
    }
    return 0;
}