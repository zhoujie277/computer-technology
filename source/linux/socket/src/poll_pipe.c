#include <stdio.h>
#include <unistd.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <ctype.h>
#include <stdlib.h>
#include <poll.h>
#include <errno.h>

#define MAXLINE 10

#define PORT 9000

void checkError(int ret, const char *str)
{
    if (ret == -1)
    {
        perror(str);
        exit(1);
    }
}
/**
 * poll 从管道中读取数据
 * 该程序可验证 poll 是电平触发模式（Level triggers，LT）
 */
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
        struct pollfd pfds[MAXLINE];
        pfds[0].fd = pipefds[0];
        pfds[0].events = POLLIN;
        while (1)
        {
            // 管道中有数据，poll 便会返回
            int nfds = poll(pfds, 1, -1);
            checkError(nfds, "poll error");
            if (pfds->revents & POLLIN)
            {
                int len = read(pipefds[0], buf, MAXLINE / 2);
                checkError(len, "read error");
                write(STDOUT_FILENO, buf, len);
            }
        }
    }
}