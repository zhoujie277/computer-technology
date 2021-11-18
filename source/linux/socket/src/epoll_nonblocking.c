#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/epoll.h>
#include <unistd.h>
#include <fcntl.h>

#define MAXLINE 10
#define SERV_PORT 9000

int main()
{
    struct sockaddr_in serv_addr, cli_addr;
    socklen_t cli_addr_len;
    int listenfd, connfd;
    char buf[MAXLINE];
    char str[INET_ADDRSTRLEN];
    int efd, flag;

    listenfd = socket(AF_INET, SOCK_STREAM, 0);

    bzero(&serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(SERV_PORT);

    bind(listenfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));

    listen(listenfd, 20);

    struct epoll_event ev;
    struct epoll_event events[10];
    int res, len;

    efd = epoll_create(10);
    ev.events = EPOLLIN | EPOLLET; //边沿触发

    printf("Accepting connections ...\n");

    cli_addr_len = sizeof(cli_addr);
    connfd = accept(listenfd, (struct sockaddr *)&cli_addr, &cli_addr_len);
    const char *ip = inet_ntop(AF_INET, &cli_addr.sin_addr.s_addr, str, sizeof(str));
    int port = ntohs(cli_addr.sin_port);
    printf("client connecting: ip = %s, port = %d\n", ip, port);

    // 设置非阻塞 IO
    flag = fcntl(connfd, F_GETFL);
    flag |= O_NONBLOCK;
    fcntl(connfd, F_SETFL, flag);

    ev.data.fd = connfd;
    epoll_ctl(efd, EPOLL_CTL_ADD, connfd, &ev);
    while(1)
    {
        printf("epoll_wait begin\n");
        res = epoll_wait(efd, events, 10, -1);
        printf("epoll_wait end red %d \n", res);
        if (events[0].data.fd == connfd)
        {
            while((len = read(connfd, buf, MAXLINE / 2)) > 0)
            {
                write(STDOUT_FILENO, buf, len);
            }
        }
    }

    return 0;
}