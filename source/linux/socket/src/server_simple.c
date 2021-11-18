#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <ctype.h>

#define SERV_PORT 9989


void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}

int main()
{
    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(SERV_PORT);
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    // 创建本地套接字
    int serverFd = socket(AF_INET, SOCK_STREAM, 0);
    checkError(serverFd, "socket error");

    // 绑定本地地址结构
    int ret = bind(serverFd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "bind error");

    // 设置链接数
    listen(serverFd, 128);
    checkError(ret, "listen error");

    // 等待客户端连接
    struct sockaddr_in client_addr;
    socklen_t len = sizeof(client_addr); // 传入传出参数
    char buf[BUFSIZ];
    int clientFd = accept(serverFd, (struct sockaddr *)&client_addr, &len);
    checkError(clientFd, "accept error");

    // 网络地址结构转本地IP，端口网络字节序转本地字符显示
    char ipbuf[BUFSIZ];
    const char* clientIP = inet_ntop(AF_INET, &client_addr.sin_addr.s_addr, ipbuf, sizeof(ipbuf));;
    int clientPort = ntohs(client_addr.sin_port);
    printf("client connecting: ip = %s, port = %d\n", clientIP, clientPort);
    
    // 读取客户端数据
    while ((ret = read(clientFd, buf, sizeof(buf))) != 0)
    {
        checkError(ret, "read error");
        write(STDOUT_FILENO, buf, ret);
        for (int i = 0; i < ret; i++)
        {
            buf[i] = toupper(buf[i]);
        }
        write(clientFd, buf, ret);
    }
    close(serverFd);
    close(clientFd);
    return 0;
}