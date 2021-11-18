#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>

#define SERV_IP "192.168.31.189"
#define SERV_PORT 9000

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}

int main(int argc, char *argv[])
{
    void* dst;
    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(SERV_PORT);
    // IP 地址转为网络字节序
    inet_pton(AF_INET, SERV_IP, &serv_addr.sin_addr.s_addr);

    // 创建本地套接字
    int localFd = socket(AF_INET, SOCK_STREAM, 0);
    checkError(localFd, "socket error");

    // 连接服务器
    int ret = connect(localFd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "connect error");

    char buf[BUFSIZ];
    while((ret = read(STDIN_FILENO, buf, sizeof(buf))) != 0)
    {
        checkError(ret, "read error");
        write(localFd, buf, ret);
        ret = read(localFd, buf, sizeof(buf));
        checkError(ret, "read server error");
        write(STDOUT_FILENO, buf, ret);
    }

    close(localFd);
    return 0;
}