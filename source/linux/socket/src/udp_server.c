#include <unistd.h>
#include <sys/socket.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <ctype.h>

#define PORT 9001

/**
 * udp server 演示
 */
void checkError(int ret, const char *str)
{
    if (ret >= 0)
        return;
    perror(str);
}

void strToUpper(char *buf, int size)
{
    for (int i = 0; i < size; i++)
        buf[i] = toupper(buf[i]);
}

int main()
{
    int lfd = socket(AF_INET, SOCK_DGRAM, 0);
    checkError(lfd, "socket error");

    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(PORT);

    int ret = bind(lfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "bind error");

    char buf[BUFSIZ];
    struct sockaddr_in cli_addr;
    socklen_t cli_addr_len;
    char str[INET_ADDRSTRLEN];
    while (1)
    {
        cli_addr_len = sizeof(cli_addr);
        ret = recvfrom(lfd, buf, BUFSIZ, 0, (struct sockaddr *)&cli_addr, &cli_addr_len);
        checkError(ret, "recv error");
        printf("received from %s at PORT %d\n",
                inet_ntop(AF_INET, &cli_addr.sin_addr, str, sizeof(str)),
                ntohs(cli_addr.sin_port));
        write(STDOUT_FILENO, buf, ret);
        strToUpper(buf, ret);
        ret = sendto(lfd, buf, ret, 0, (struct sockaddr *)&cli_addr, sizeof(cli_addr));
        checkError(ret, "send error");
    }
    close(lfd);
    return 0;
}