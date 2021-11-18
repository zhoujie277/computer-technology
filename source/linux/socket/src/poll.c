#include <stdio.h>
#include <unistd.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <ctype.h>
#include <stdlib.h>
#include <poll.h>
#include <errno.h>

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
 * poll 功能演示
 * 优点：
 *  自带数组结构，监听事件和返回事件分离。
 * 缺点：
 *  无法直接定位监听事件的描述符，不能跨平台。
 */
int main()
{
    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    // 创建套接字
    int listenfd = socket(AF_INET, SOCK_STREAM, 0);
    checkError(listenfd, "socket error");

    // 设置属性，此处复用套接字
    int opt = 1;
    setsockopt(listenfd, SOL_SOCKET, SO_REUSEPORT, &opt, sizeof(opt));

    // 绑定地址结构
    int ret = bind(listenfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "bind error");

    // 设置连接上限
    ret = listen(listenfd, 128);
    checkError(ret, "listen error");

    struct pollfd fds[FD_SETSIZE];

    // 初始化listenfd的监听
    fds[0].fd = listenfd;
    fds[0].events = POLLIN;

    // 初始化
    for (int i = 1; i < FD_SETSIZE; i++)
    {
        fds[i].fd = -1;
    }

    // 定义连接客户端的地址结构
    struct sockaddr_in cli_addr;
    socklen_t cli_addr_len;

    // 定义最大描述符索引
    int maxIndex = 0;

    // 定义IP缓冲
    char ipbuf[INET_ADDRSTRLEN];

    while (1)
    {
        printf("poll before \n");
        // 实际监听的数量
        int nready = poll(fds, maxIndex + 1, -1); // -1表示阻塞等待
        checkError(nready, "poll error");
        printf("poll nready = %d \n", nready);
        if (nready == 0)
            continue;
        // 判断 listenfd 是否有读事件发生
        if (fds[0].revents & POLLIN)
        {
            cli_addr_len = sizeof(cli_addr);
            int cfd = accept(listenfd, (struct sockaddr *)&cli_addr, &cli_addr_len);
            checkError(cfd, "accept error");

            const char *ip = inet_ntop(AF_INET, &cli_addr.sin_addr.s_addr, ipbuf, sizeof(ipbuf));
            int port = ntohs(cli_addr.sin_port);
            printf("client connecting: ip = %s, port = %d\n", ip, port);

            int i = 1;
            for (; i < FD_SETSIZE; i++)
            {
                if (fds[i].fd < 0)
                {
                    fds[i].fd = cfd;
                    fds[i].events = POLLIN;
                    break;
                }
            }
            if (i == FD_SETSIZE)
            {
                perror("too many clients");
                exit(1);
            }

            if (maxIndex < i)
                maxIndex = i;

            if (--nready == 0)
                continue;
        }
        for (int i = 1; i <= maxIndex; i++)
        {
            if (fds[i].fd < 0)
                continue;
            if (fds[i].revents & POLLIN)
            {
                char buf[BUFSIZ];
                ret = read(fds[i].fd, buf, sizeof(buf));
                if (ret < 0)
                {
                    if (errno == ECONNRESET)
                    {
                        printf("client[%d] aborted connection \n", i);
                        close(fds[i].fd);
                        fds[i].fd = -1;
                    }
                    else
                        checkError(ret, "read error");
                }
                else if (ret == 0)
                {
                    close(fds[i].fd);
                    fds[i].fd = -1;
                }
                else
                {
                    writeback(fds[i].fd, buf, ret);
                }
                if (0 == --nready)
                    continue;
            }
        }
    }

    close(listenfd);
    return 0;
}