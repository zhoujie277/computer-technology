#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <string.h>
#include <arpa/inet.h>

#define CLIENT_PORT 9000
#define MAXLINE 4096

/**
 * 服务端发送广播演示
 */
int main(void)
{
    struct sockaddr_in localaddr;
    char buf[MAXLINE];

    // 构造用于 UDP 通信的套接字
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);

    // 初始化本地套接字
    bzero(&localaddr, sizeof(localaddr));
    localaddr.sin_family = AF_INET;
    // localaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    inet_pton(AF_INET, "0.0.0.0", &localaddr.sin_addr.s_addr);
    localaddr.sin_port = htons(CLIENT_PORT);

    int ret = bind(sockfd, (struct sockaddr *)&localaddr, sizeof(localaddr));

    int i = 0;
    while (1)
    {
        int len = recvfrom(sockfd, buf, sizeof(buf), 0, NULL, 0);
        write(STDOUT_FILENO, buf, len);
    }
    close(sockfd);

    return 0;
}
