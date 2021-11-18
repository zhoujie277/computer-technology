#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/epoll.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <errno.h>
#include <ctype.h>

/**
 * epoll 是linux下多路复用 IO 接口 select/poll 的增强版本。
 * 它能显著提高程序在大量并发连接中只有少量活跃的情况下的系统 CPU 利用率，
 * 因为它会复用文件描述符集合来传递结果而不用迫使开发者每次等待事件之前
 * 都必须重新准备要被侦听的文件描述符集合。另一个原因就是获取事件的时候，
 * 它无须遍历整个被侦听的描述符集，只要遍历那些被内核 IO 事件异步唤醒
 * 而假如 Ready 队列的描述符集合就行了。
 * epoll 除了提供 select/poll 那种 IO 事件的电平触发(Level Trigger)外，
 * 还提供了边沿触发(Edge Triggered),这就使得用户空间程序有可能获得缓存 IO 状态，
 * 减少 epoll_wait/epoll_pwait 的调用，提高应用程序效率。
 * 
 * 可以使用 cat 命令查看一个进程可以打开的 socket 描述符上限
 * cat /proc/sys/fs/file-max
 * 
 */

#define PORT 9000
#define MAX_EVENTS 10

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(EXIT_FAILURE);
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
int main()
{
    int listen_sock, conn_sock, nfds, epollfd;

    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    // 创建套接字
    listen_sock = socket(AF_INET, SOCK_STREAM, 0);
    checkError(listen_sock, "socket error");

    // 设置重用端口属性
    int opt = 1;
    setsockopt(listen_sock, SOL_SOCKET, SO_REUSEPORT, &opt, sizeof(opt));

    // 绑定地址结构
    int ret = bind(listen_sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "bind error");

    listen(listen_sock, 128);

    struct sockaddr_in cli_addr;
    socklen_t cli_addr_len;
    // 定义IP缓冲
    char ipbuf[INET_ADDRSTRLEN];

    // 初始化数据
    struct epoll_event ev, events[MAX_EVENTS];
    epollfd = epoll_create(MAX_EVENTS);
    checkError(epollfd, "epoll_create error");
    ev.events = EPOLLIN;
    ev.data.fd = listen_sock;
    ret = epoll_ctl(epollfd, EPOLL_CTL_ADD, listen_sock, &ev);
    checkError(ret, "epoll_ctl:listen_sock");

    for (;;)
    {
        nfds = epoll_wait(epollfd, events, MAX_EVENTS, -1);
        checkError(nfds, "epoll_wait");
        int n = 0;
        for (; n < nfds; ++n)
        {
            // 如果不是读事件，继续处理下一个
            if (!(events[n].events & EPOLLIN))
                continue;

            if (events[n].data.fd == listen_sock)
            {
                cli_addr_len = sizeof(cli_addr);
                conn_sock = accept(listen_sock, (struct sockaddr *)&cli_addr, &cli_addr_len);
                checkError(conn_sock, "accept");
                // setnonblocking(conn_sock);
                // ev.events = EPOLLIN | EPOLLET;
                ev.events = EPOLLIN;
                ev.data.fd = conn_sock;
                ret = epoll_ctl(epollfd, EPOLL_CTL_ADD, conn_sock, &ev);
                checkError(ret, "epoll_ctl:conn_sock");

                const char *ip = inet_ntop(AF_INET, &cli_addr.sin_addr.s_addr, ipbuf, sizeof(ipbuf));
                int port = ntohs(cli_addr.sin_port);
                printf("client connecting: ip = %s, port = %d\n", ip, port);
            }
            else
            {
                char buf[BUFSIZ];
                ret = read(events[n].data.fd, buf, sizeof(buf));
                if (ret < 0)
                {
                    perror("read error");
                    int ret = epoll_ctl(epollfd, EPOLL_CTL_DEL, events[n].data.fd, NULL);
                    checkError(ret, "epoll_ctl del conn_scok");
                    close(events[n].data.fd);
                    if (errno == ECONNRESET)
                    {
                        printf("connect reset");
                    }
                    else
                    {
                        exit(EXIT_FAILURE);
                    }
                }
                if (ret == 0)
                {
                    printf("client [%d] exited\n", events[n].data.fd);
                    int ret = epoll_ctl(epollfd, EPOLL_CTL_DEL, events[n].data.fd, NULL);
                    checkError(ret, "epoll_ctl ret del conn_scok");
                    close(events[n].data.fd);
                }
                else
                {
                    writeback(events[n].data.fd, buf, ret);
                }
            }
        }
    }

    close(listen_sock);
    return 0;
}