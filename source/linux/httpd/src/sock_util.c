

#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/epoll.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include "../include/sock_util.h"
#include "../include/error_util.h"

int get_line(int sock, char *buf, int size)
{
    int i = 0;
    char c = '\0';
    int n;
    while ((i < size - 1 && (c != '\n')))
    {
        n = recv(sock, &c, 1, 0);
        if (n > 0)
        {
            if (c == '\r')
            {
                n = recv(sock, &c, 1, MSG_PEEK);
                if (n > 0 && c == '\n')
                    recv(sock, &c, 1, 0);
                else
                    c = '\n';
            }
            buf[i] = c;
            i++;
        }
        else
        {
            if (n == -1)
            {
                if (errno != EAGAIN && errno != EWOULDBLOCK)
                    perror("recv");
                break;
            }
            c = '\n';
        }
    }
    buf[i] = '\0';
    return i;
}

void setnonblocking(int fd)
{
    int flag = fcntl(fd, F_GETFL);
    flag |= O_NONBLOCK;
    int ret = fcntl(fd, F_SETFL, flag);
    check_error(ret, "fcntl");
}

int build_socket(int port)
{
    int sfd = socket(AF_INET, SOCK_STREAM, 0);
    check_error(sfd, "socket error");

    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    setnonblocking(sfd);

    int flag = 1;
    setsockopt(sfd, SOL_SOCKET, SO_REUSEPORT, &flag, sizeof(flag));

    int ret = bind(sfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    check_error(ret, "bind error");

    ret = listen(sfd, 20);
    check_error(ret, "listen error");
    return sfd;
}

void set_epoll_event(int efd, int op, int events, int fd)
{
    struct epoll_event ev;
    ev.events = events;
    ev.data.fd = fd;

    int ret = epoll_ctl(efd, op, fd, &ev);
    check_error(ret, "epoll_ctl");
}

int build_epoll(int fd)
{
    int efd = epoll_create1(0);
    check_error(efd, "epoll_create1");
    set_epoll_event(efd, EPOLL_CTL_ADD, EPOLLIN | EPOLLET, fd);
    return efd;
}

int accept_client(int sockfd)
{
    static struct sockaddr_in cli_addr;
    static socklen_t len;
    len = sizeof(cli_addr);
    int cfd = accept(sockfd, (struct sockaddr *)&cli_addr, &len);
    check_error(cfd, "accept");
    setnonblocking(cfd);
    char buf[INET_ADDRSTRLEN];
    const char *ip = inet_ntop(AF_INET, &cli_addr.sin_addr, buf, sizeof(buf));
    unsigned short port = ntohs(cli_addr.sin_port);
    printf("accept client %s : %d \n", ip, port);
    return cfd;
}

void start_server(int port, const handle_line on_handle_line)
{
    int sfd = build_socket(port);
    int efd = build_epoll(sfd);
    static struct epoll_event events[FD_SETSIZE];

    int i;
    while (1)
    {
        int nfds = epoll_wait(efd, events, FD_SETSIZE, -1);
        check_error(nfds, "epoll_wait");
        for (i = 0; i < nfds; i++)
        {
            int fd = events[i].data.fd;
            if (fd == sfd)
            {
                int cfd = accept_client(sfd);
                set_epoll_event(efd, EPOLL_CTL_ADD, EPOLLIN, cfd);
            }
            else if (events[i].events & EPOLLIN)
            {
                int ret;
                size_t len;
                char content[8 * BUFSIZ];
                char buf[BUFSIZ];
                while ((ret = recv(fd, buf, sizeof(buf), 0)) > 0)
                {
                    strcat(content, buf);
                }
                check_error(ret, "recv error");
                on_handle_line(content, ret, content, &len);
                send(fd, content, strlen(content), 0);
                memset(content, 0, sizeof(content));
                // 处理一次会话断开连接
                set_epoll_event(efd, EPOLL_CTL_DEL, EPOLLIN, fd);
                close(fd);
            }
        }
    }
}
