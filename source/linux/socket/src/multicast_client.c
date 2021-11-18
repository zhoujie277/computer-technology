#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>
#include <net/if.h>

#define SERVER_PORT 8000
#define CLIENT_PORT 9000

#define GROUP "239.0.0.2"

/**
 * 加入多播组，收消息
 */
int main(int argc, char *argv[])
{
    struct sockaddr_in localaddr;
    int confd;
    ssize_t len;
    char buf[BUFSIZ];

    // 组播结构体
    struct ip_mreqn group;

    confd = socket(AF_INET, SOCK_DGRAM, 0);

    bzero(&localaddr, sizeof(localaddr));
    localaddr.sin_family = AF_INET;
    inet_pton(AF_INET, "0.0.0.0", &localaddr.sin_addr.s_addr);
    localaddr.sin_port = htons(CLIENT_PORT);

    bind(confd, (struct sockaddr *)&localaddr, sizeof(localaddr));

    // 设置组地址
    inet_pton(AF_INET, GROUP, &group.imr_multiaddr);
    // 使用本地任意 IP 添加到组播组
    inet_pton(AF_INET, "0.0.0.0", &group.imr_address);
    // 通过网卡名获取编号 ip ad
    group.imr_ifindex = if_nametoindex("eth0");

    // 设置 client 加入多播组
    setsockopt(confd, IPPROTO_IP, IP_ADD_MEMBERSHIP, &group, sizeof(group));

    while (1)
    {
        len = recvfrom(confd, buf, sizeof(buf), 0, NULL, 0);
        write(STDOUT_FILENO, buf, len);
    }
    close(confd);

    return 0;
}