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
            FD_SET(clifd, &allset);  // 添加需要监听的读事件。
            printf("accept client clifd = %d \n", clifd);
            if (clifd >= maxfd)
                maxfd = clifd + 1;
            if (1 == nready)
                continue;
        }
        // 这个循环遍历查找文件描述符可优化。
        for (int i = listenfd + 1; i < maxfd; i++)
        {
            if (FD_ISSET(i, &rset))
            {
                printf("read client i = %d \n", i);
                char buf[BUFSIZ];
                ret = read(i, buf, sizeof(buf));
                checkError(ret, "read error");
                if (ret == 0)
                {
                    close(i); // 客户端连接关闭，从读事件中删除。
                    FD_CLR(i, &allset);
                }
                else
                {
                    writeback(i, buf, ret);
                }
            }
        }
    }
    close(listenfd);

    return 0;
}