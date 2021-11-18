#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

int main(int argc, const char *argv[])
{
    if (argc < 2)
        return 1;


    if (argc == 4 && argv[2][0] == '>')
    {
        printf("hello world");
        int wfd = open(argv[3], O_RDWR | O_CREAT | O_TRUNC);
        dup2(wfd, STDOUT_FILENO);
    }

    int fd = open(argv[1], O_RDONLY);
    if (fd == -1)
    {
        perror("open error");
        return 3;
    }
    int ret = 0;
    char buf[4096];
    while ((ret = read(fd, buf, 4096)) != 0)
    {
        if (ret == -1)
        {
            perror("read error");
            return 2;
        }
        write(STDOUT_FILENO, buf, ret);
    }
    return 0;
}