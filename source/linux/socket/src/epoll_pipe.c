#include <stdlib.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/epoll.h>

#define MAXLINE 10

/**
 * 利用 epoll 和管道的读写示例。
 * 说明 水平触发(Level Triggers, LT) 和 边沿触发(Edge Triggers, ET) 的区别。
 * 
 */
void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(EXIT_FAILURE);
    }
}

int main()
{
    int pipefds[2];
    int ret = pipe(pipefds);
    checkError(ret, "pipe error");

    char buf[MAXLINE];
    pid_t pid = fork();

    if (pid == 0)
    {
        char ch = 'a';
        close(pipefds[0]);
        int i;
        while (1)
        {
            for (i = 0; i < MAXLINE / 2; i++)
            {
                buf[i] = ch;
            }
            buf[i - 1] = '\n';
            ch++;
            for (; i < MAXLINE; i++)
                buf[i] = ch;
            buf[i - 1] = '\n';
            ch++;
            write(pipefds[1], buf, sizeof(buf));
            sleep(5);
        }
    }
    else
    {
        close(pipefds[1]);
        struct epoll_event ev;
        struct epoll_event events[10];
        int efd = epoll_create(10);
        checkError(efd, "epoll_create");
        ev.events = EPOLLIN;  // LT 水平触发（默认)
        // ev.events |= EPOLLET; // ET 边沿触发
        ev.data.fd = pipefds[0];
        int ret = epoll_ctl(efd, EPOLL_CTL_ADD, pipefds[0], &ev);
        checkError(ret, "epoll_ctl pfd");

        while (1)
        {
            // 水平触发：缓冲区只要有数据，就可以返回
            // 边沿触发：除非子进程再往管道里写数据，才会触发wait返回。
            int nfds = epoll_wait(efd, events, 10, -1);
            checkError(nfds, "epoll_wait error");
            printf("nfds %d \n", nfds);
            if (events[0].data.fd == pipefds[0])
            {
                int len = read(pipefds[0], buf, MAXLINE / 2);
                checkError(len, "read error");
                write(STDOUT_FILENO, buf, len);
            }
        }
    }

    return 0;
}