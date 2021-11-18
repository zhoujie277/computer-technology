#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

void copy(const char* src, const char* dst)
{
    int n = 0;
    char buf[1024];

    int rfd = open(src, O_RDONLY);
    if (rfd == -1)
    {
        perror("open src was error!");
        return;
    }

    int wfd = open(dst, O_RDWR | O_CREAT | O_TRUNC, 0664);
    if (wfd == -1)
    {
        perror("open dst was error!");
        return;
    }
    
    // 0 表示读到末尾
    while ((n = read(rfd, buf, 1024)) != 0)
    {
        write(wfd, buf, n);
    }

    close(rfd);
    close(wfd);
}

int main(int argc, char *argv[])
{  
    copy(argv[1], argv[2]);
    return 0;
}