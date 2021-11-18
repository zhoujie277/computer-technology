#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

int main(int argc, char *argv[])
{
    int fd1 = open(argv[1], O_RDWR);
    int fd2 = open(argv[2], O_RDWR);
    int nf = dup2(fd1, fd2);
    printf("file descriptor %d, %d, %d\n", fd1, fd2, nf);

    int ret = write(fd2, "12345", 5);
    printf("write retno is %d", ret);

    dup2(fd1, STDOUT_FILENO);
    printf("hello world..........");

    return 0;
}