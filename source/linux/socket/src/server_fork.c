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

#define SERV_PORT 9989

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}

void doWork(int clientFd, struct sockaddr_in *client_addr)
{
    // 网络地址结构转本地IP，端口网络字节序转本地字符显示
    char ipbuf[BUFSIZ];
    const char *clientIP = inet_ntop(AF_INET, &client_addr->sin_addr.s_addr, ipbuf, sizeof(ipbuf));
    ;
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

/**
 * 多进程并发服务器
 * 需关注进程可中断等待状态和kill 信号以及系统调用的关系
 */
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
    struct sigaction act;
    act.sa_handler = onSignal;
    sigemptyset(&act.sa_mask);
    act.sa_flags |= SA_RESTART; // 重启 accept 系统调用
    sigaction(SIGCHLD, &act, NULL);
    pid_t pid;
    struct sockaddr_in client_addr;
    int clientFd;
    while (1)
    {
        // 等待客户端连接
        socklen_t len = sizeof(client_addr); // 传入传出参数
        clientFd = accept(serverFd, (struct sockaddr *)&client_addr, &len);
        
        printf("------------------accept...clientFd=%d errno=%d \n", clientFd, errno);
        
        if (clientFd == -1 && errno == EINTR)
        {
            printf("------------------interrupted...、\n");
            continue;
        }
        checkError(clientFd, "accept error");

        pid = fork();
        checkError(pid, "fork error");
        if (pid == 0)
        {
            close(serverFd);
            break;
        }
        else
        {
            close(clientFd);
        }
    }
    if (pid == 0)
    {
        doWork(clientFd, &client_addr);
        return 0;
    }

    close(serverFd);
    return 0;
}