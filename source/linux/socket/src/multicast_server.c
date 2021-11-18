/**
 * 组播(多播)
 * 组播组可以是永久的也可以是临时的。
 * 组播组地址中，有一部分由官方分配的，称为永久组播组。
 * 永久组播组保持不变的是它的ip地址，组中的成员构成可以发送变化。
 * 永久组播组中成员的数量都可以是任意的，甚至可以为零，那些没有保留下来供永久组播组
 * 使用的ip组播地址，可以被临时组播组利用。
 * 
 * + 224.0.0.0 ~ 224.0.0.255 为预留的组播地址(永久组地址),地址 224.0.0.0 保留不做分配，其他地址供路由协议使用。
 * + 224.0.1.0 ~ 224.0.1.255 是公用组播地址，可以用于 internet，欲使用需申请。
 * + 224.0.2.0 ~ 238.255.255.255 为用户可用的组播地址(临时组地址)，全网范围内有效
 * + 239.0.0.0 ~ 239.255.255.255 为本地管理组播地址，仅在特定的本地范围内有效
 * 
 * 
 */

#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>
#include <net/if.h>

#define SERVER_PORT 8000
#define CLIENT_PORT 9000
#define MAXLINE 1500

#define GROUP "239.0.0.2"

/**
 * server 向 client 发送组播
 */
int main(void)
{
    int sockfd;
    struct sockaddr_in servaddr, clientaddr;
    char buf[MAXLINE] = "hello world\n";
    struct ip_mreqn group;

    sockfd = socket(AF_INET, SOCK_DGRAM, 0);

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(SERVER_PORT);

    bind(sockfd, (struct sockaddr *)&servaddr, sizeof(servaddr));

    // 设置组播组地址
    inet_pton(AF_INET, GROUP, &group.imr_multiaddr);
    // 本地任意 IP
    inet_pton(AF_INET, "0.0.0.0", &group.imr_address);
    // 给出网卡名，转换为对应编号：eth0 --> 编号 命令：ip ad 
    group.imr_ifindex = if_nametoindex("eth0");

    // 设置组播，向组播发送数据
    setsockopt(sockfd, IPPROTO_IP, IP_MULTICAST_IF, &group, sizeof(group));

    // 构造 client 地址 IP + 端口
    bzero(&clientaddr, sizeof(clientaddr));
    clientaddr.sin_family = AF_INET;
    // IPv4 239.0.0.2:9000
    inet_pton(AF_INET, GROUP, &clientaddr.sin_addr.s_addr);
    clientaddr.sin_port = htons(CLIENT_PORT);

    int i = 0;
    while (1)
    {
        sprintf(buf, "hello world %d\n", i++);
        sendto(sockfd, buf, strlen(buf), 0, (struct sockaddr *)&clientaddr, sizeof(clientaddr));
        sleep(1);
    }
    return 0;
}