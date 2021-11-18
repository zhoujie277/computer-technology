#include <unistd.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <sys/un.h>
#include <string.h>

#define SERV_ADDR "serv.socket"

/**
 * 本地 socket 演示
 * 进程间通信的一种方式
 */
void checkError(int ret, const char *str)
{
    if (ret >= 0)
        return;
    perror(str);
}

void strToUpper(char *buf, int size)
{
    for (int i = 0; i < size; i++)
        buf[i] = toupper(buf[i]);
}

int main(void)
{
    int lfd, cfd, len, size, i;
    struct sockaddr_un servaddr, cliaddr;
    char buf[4096];

    lfd = socket(AF_UNIX, SOCK_STREAM, 0);
    checkError(lfd, "socket error");

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sun_family = AF_UNIX;
    strcpy(servaddr.sun_path, SERV_ADDR);

    len = __offsetof(struct sockaddr_un, sun_path) + strlen(servaddr.sun_path);

    unlink(SERV_ADDR); //确保bind之前 serv.sock文件不存在，bind会创建该文件
    int ret = bind(lfd, (struct sockaddr *)&servaddr, len);
    checkError(ret, "bind error");

    listen(lfd, 20);

    printf("Accept ...\n");

    while (1)
    {
        len = sizeof(cliaddr);
        cfd = accept(lfd, (struct sockaddr *)&cliaddr, (socklen_t *)&len);
        len -= __offsetof(struct sockaddr_un, sun_path);
        cliaddr.sun_path[len] = '\0'; //确保打印不会出现乱码

        printf("client bind filename %s\n", cliaddr.sun_path);

        while ((size = read(cfd, buf, sizeof(buf))) > 0)
        {
            write(STDOUT_FILENO, buf, size);
            strToUpper(buf, size);
            write(cfd, buf, size);
        }
        close(cfd);
    }

    close(lfd);

    return 0;
}