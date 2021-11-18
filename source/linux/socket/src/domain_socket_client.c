#include <unistd.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <sys/un.h>
#include <string.h>

#define SERV_ADDR "serv.socket"
#define CLI_ADDR "cli.socket"

/**
 * 本地 socket 演示
 * 进程间通信的一种方式
 */
void checkError(int ret, const char *str)
{
    if (ret >= 0)
        return;
    perror(str);
    exit(EXIT_FAILURE);
}

int main(void)
{
    char buf[BUFSIZ];

    int lfd = socket(AF_UNIX, SOCK_STREAM, 0);
    checkError(lfd, "socket error");

    struct sockaddr_un servaddr;
    bzero(&servaddr, sizeof(servaddr));
    servaddr.sun_family = AF_UNIX;
    strcpy(servaddr.sun_path, SERV_ADDR);

    struct sockaddr_un cli_addr;
    bzero(&cli_addr, sizeof(cli_addr));
    cli_addr.sun_family = AF_UNIX;
    strcpy(cli_addr.sun_path, CLI_ADDR);

    // 需要绑定socket文件，服务器才能拿到地址。如果不绑定，能进行读写数据，但是拿不到地址信息。
    int len = __offsetof(struct sockaddr_un, sun_path) + strlen(cli_addr.sun_path);
    unlink(CLI_ADDR);
    int ret = bind(lfd, (struct sockaddr *)&cli_addr, len);
    checkError(ret, "bind error");

    len = __offsetof(struct sockaddr_un, sun_path) + strlen(servaddr.sun_path);

    ret = connect(lfd, (struct sockaddr *)&servaddr, len);
    checkError(ret, "connect error");

    while (fgets(buf, BUFSIZ, stdin) != NULL)
    {
        ret = send(lfd, buf, strlen(buf), 0);
        checkError(ret, "send error");
        ret = recv(lfd, buf, BUFSIZ, 0);
        checkError(ret, "recv error");
        write(STDOUT_FILENO, buf, ret);
    }

    close(lfd);

    return 0;
}