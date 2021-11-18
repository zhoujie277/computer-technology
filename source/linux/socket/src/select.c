#include <stdio.h>
#include <unistd.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <ctype.h>
#include <stdlib.h>

#define PORT 9000

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}

void writeback(int fd, char *buf, size_t nbyte)
{
    write(STDOUT_FILENO, buf, nbyte);
    for (int i = 0; i < nbyte; i++)
    {
        buf[i] = toupper(buf[i]);
    }
    write(fd, buf, nbyte);
}

/**
 * select 的缺点：
 * + 监听上限受文件描述符限制。
 */
int main()
{
    // 记录客户端连接
    int clientFds[FD_SETSIZE];
    int clientCount = 0;
    char ipStr[INET_ADDRSTRLEN];
    for (int i = 0; i < FD_SETSIZE; i++)
    {
        clientFds[i] = -1;
    }

    // 初始化 TCP 套接字
    int listenfd = socket(AF_INET, SOCK_STREAM, 0);
    checkError(listenfd, "socket error");
    int maxfd = listenfd + 1;

    // 端口复用
    int opt = 1;
    setsockopt(listenfd, SOL_SOCKET, SO_REUSEPORT, &opt, sizeof(opt));

    // 绑定本地地址结构
    struct sockaddr_in serv_addr;
    serv_addr.sin_port = htons(PORT);
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    socklen_t len = sizeof(serv_addr);
    int ret = bind(listenfd, (struct sockaddr *)&serv_addr, len);
    checkError(ret, "bind error");

    // 设置连接数
    ret = listen(listenfd, 128);
    checkError(ret, "listen error");

    // 初始化 读事件集合
    fd_set rset, allset;
    FD_ZERO(&allset);
    FD_SET(listenfd, &allset);

    struct sockaddr_in cli_addr;
    socklen_t cli_addr_len;
    int clifd;
    while (1)
    {
        rset = allset;
        int nready = select(maxfd, &rset, NULL, NULL, NULL);
        if (nready < 0)
        {
            perror("select error");
        }
        if (nready == 0)
            continue;
        if (FD_ISSET(listenfd, &rset))
        {
            cli_addr_len = sizeof(cli_addr);
            clifd = accept(listenfd, (struct sockaddr *)&cli_addr, &cli_addr_len);
            checkError(clifd, "accept error");

            printf("accept client clifd = %d \n", clifd);
            const char *ip = inet_ntop(AF_INET, &cli_addr.sin_addr.s_addr, ipStr, sizeof(ipStr));
            int clientPort = ntohs(cli_addr.sin_port);
            printf("client connecting: ip = %s, port = %d\n", ip, clientPort);

            int i = 0;
            for (; i < FD_SETSIZE; i++)
            {
                if (clientFds[i] < 0)
                {
                    clientFds[i] = clifd;
                    break;
                }
            }
            if (i == FD_SETSIZE)
            {
                fputs("too many clients \n", stderr);
                exit(1);
            }
            if (clientCount <= i)
            {
                clientCount = i + 1;
            }

            FD_SET(clifd, &allset); // 添加需要监听的读事件。

            if (clifd >= maxfd)
                maxfd = clifd + 1;
            if (0 == --nready)
                continue;
        }
        // 这个循环遍历查找文件描述符可优化。
        for (int i = 0; i < clientCount; i++)
        {
            int sockfd = clientFds[i];
            if (sockfd < 0)
                continue;
            if (FD_ISSET(sockfd, &rset))
            {
                char buf[BUFSIZ];
                ret = read(sockfd, buf, sizeof(buf));
                checkError(ret, "read error");
                if (ret == 0)
                {
                    printf("close client sockfd = %d \n", sockfd);
                    clientFds[i] = -1;
                    close(sockfd); // 客户端连接关闭，从读事件中删除。
                    FD_CLR(sockfd, &allset);
                }
                else
                {
                    printf("read client sockfd = %d \n", sockfd);
                    writeback(sockfd, buf, ret);
                }
                if (0 == --nready)
                    break;
            }
        }
    }
    close(listenfd);

    return 0;
}