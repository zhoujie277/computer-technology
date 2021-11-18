#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <ctype.h>
#include <signal.h>
#include <sys/wait.h>
#include <errno.h>
#include <pthread.h>

#define SERV_PORT 9000

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}

struct ClientInfo
{
    int clientFd;
    struct sockaddr_in client_addr;
};

void *doWork(void *arg)
{
    struct ClientInfo *cInfo = (struct ClientInfo *)arg;
    int clientFd = cInfo->clientFd;
    struct sockaddr_in *client_addr = &cInfo->client_addr;
    free(cInfo);
    // 网络地址结构转本地IP，端口网络字节序转本地字符显示
    char ipbuf[BUFSIZ];
    const char *clientIP = inet_ntop(AF_INET, &client_addr->sin_addr.s_addr, ipbuf, sizeof(ipbuf));
    int clientPort = ntohs(client_addr->sin_port);
    printf("client connecting: ip = %s, port = %d\n", clientIP, clientPort);

    int ret;
    char buf[BUFSIZ];
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
    close(clientFd);
    printf("client %d is exit.\n", clientFd);
    return NULL;
}

void onSignal(int signo)
{
    pid_t pid;
    while ((pid = waitpid(-1, NULL, WNOHANG)) > 0)
    {
        printf("-------onSignalHandle %d \n", pid);
    }
    printf("-------onSignal End \n");
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

    // 端口复用技术
    int opt = 1;
    setsockopt(serverFd, SOL_SOCKET, SO_REUSEPORT, &opt, sizeof(opt));

    // 绑定本地地址结构
    int ret = bind(serverFd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    checkError(ret, "bind error");

    // 设置链接数
    listen(serverFd, SOMAXCONN);
    checkError(ret, "listen error");

    struct ClientInfo *info = NULL;

    pthread_t tid;
    while (1)
    {
        // 等待客户端连接
        info = malloc(sizeof(struct ClientInfo));
        socklen_t len = sizeof(info->client_addr); // 传入传出参数
        info->clientFd = accept(serverFd, (struct sockaddr *)&info->client_addr, &len);
        checkError(info->clientFd, "accept error");
        pthread_create(&tid, NULL, doWork, info);
        pthread_detach(tid);
    }

    close(serverFd);
    return 0;
}