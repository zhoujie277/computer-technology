#include <unistd.h>
#include <sys/socket.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#define PORT 9001
#define SERVER_IP "192.168.31.236"

/**
 * udp client 演示
 */
void checkError(int ret, const char *str)
{
    if (ret >= 0)
        return;
    perror(str);
    exit(EXIT_FAILURE);
}

int main()
{
    int lfd = socket(AF_INET, SOCK_DGRAM, 0);
    checkError(lfd, "socket error");
    char buf[BUFSIZ];
    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    // 点分十进制的字符串转换为网络地址结构
    inet_pton(AF_INET, SERVER_IP, &serv_addr.sin_addr);
    serv_addr.sin_port = htons(PORT);
    int ret;
    while (fgets(buf, BUFSIZ, stdin) != NULL)
    {
        ret = sendto(lfd, buf, strlen(buf), 0, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
        checkError(ret, "send error");
        ret = recv(lfd, buf, BUFSIZ, 0);
        // ret = recvfrom(lfd, buf, BUFSIZ, 0, NULL, NULL);
        checkError(ret, "recv error");
        write(STDOUT_FILENO, buf, ret);
    }
    close(lfd);
    return 0;
}