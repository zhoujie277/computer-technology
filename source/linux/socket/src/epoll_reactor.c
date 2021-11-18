/**
 * epoll 基于 非阻塞 I/O 事件驱动
 * epoll 反应堆
 */

#include <stdio.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#define MAX_EVENTS 1024
#define BUFLEN 4096
#define SERV_PORT 8080

void recvdata(int fd, int events, void *arg);
void senddata(int fd, int events, void *arg);

/**
 * 描述就绪文件描述符相关信息
 */
struct myevent_s
{
    // 要监听的描述符
    int sfd;
    // 对应的监听事件
    int events;
    // 链接指针
    void *arg;
    // 回调函数
    void (*call_back)(int fd, int events, void *arg);

    // 是否在监听: 1->在红黑树上(监听), 0->不在(不监听)
    int status;

    char buf[BUFLEN];
    int len;
    // 记录每次加入红黑树 g_efd 的时间值
    long last_active;
};

// 全局变量，保存 epoll_create 返回的文件描述符
int g_efd;
// 自定义结构类型数组
struct myevent_s g_events[MAX_EVENTS + 1];

/**
 * 将结构体 myevent_s 成员变量初始化
 */
void eventset(struct myevent_s *myev, int fd, void (*callback)(int, int, void *), void *arg)
{
    myev->sfd = fd;
    myev->call_back = callback;
    myev->events = 0;
    myev->arg = arg;
    myev->status = 0;
    memset(myev->buf, 0, sizeof(myev->buf));
    myev->len = 0;
    // 调用 eventset 函数的时间。
    myev->last_active = time(NULL);
}

/* 向 epoll 的红黑树添加一个文件描述符 */
void eventadd(int efd, int events, struct myevent_s *myev)
{
    struct epoll_event ev = {0, {0}};
    int op;
    ev.data.ptr = myev;
    ev.events = myev->events = events; //EPOLLIN 或 EPOLLOUT

    if (myev->status == 1)
    {
        op = EPOLL_CTL_MOD;
    }
    else
    {
        op = EPOLL_CTL_ADD;
        myev->status = 1;
    }

    if (epoll_ctl(efd, op, myev->sfd, &ev) < 0)
    {
        printf("event add failed [fd=%d], events[%d] \n", myev->sfd, events);
    }
    else
    {
        printf("event add OK [fd=%d], op=%d, events[%d] \n", myev->sfd, op, events);
    }
}

void eventdel(int efd, struct myevent_s *myev)
{
    struct epoll_event ev = {0, {0}};
    if (myev->status != 1)
        return;
    ev.data.ptr = myev;
    myev->status = 0;
    epoll_ctl(efd, EPOLL_CTL_DEL, myev->sfd, &ev);
}

void acceptconn(int lfd, int events, void *arg)
{
    struct sockaddr_in cli_addr;
    socklen_t len = sizeof(cli_addr);

    int cfd, i;
    if ((cfd = accept(lfd, (struct sockaddr *)&cli_addr, &len)) == -1)
    {
        if (errno != EAGAIN && errno != EINTR)
        {
            // 暂时不做出错处理
        }
        printf("%s, accept, %s\n", __func__, strerror(errno));
        return;
    }

    do
    {
        // 从全局数据 g_events 中找一个空闲元素，类似于 select 中找值为 -1 的元素，跳出 for
        for (i = 0; i < MAX_EVENTS; i++)
        {
            if (g_events[i].status == 0)
                break;
        }
        if (i == MAX_EVENTS)
        {
            printf("%s: max connect limit[%d] \n", __func__, MAX_EVENTS);
            break;
        }
        int flag = 0;
        if ((flag = fcntl(cfd, F_SETFL, O_NONBLOCK)) < 0)
        {
            printf("%s: fcntl nonblocking failed, %s \n", __func__, strerror(errno));
            break;
        }
        // 给 cfd 设置一个 myevent_s 结构体，回调函数设置为 recvdata
        eventset(&g_events[i], cfd, recvdata, &g_events[i]);
        eventadd(g_efd, EPOLLIN, &g_events[i]);
    } while (0);
    printf("new connect [%s:%d][time:%ld], pos[%d] \n",
           inet_ntoa(cli_addr.sin_addr), ntohs(cli_addr.sin_port),
           g_events[i].last_active, i);
}

void recvdata(int fd, int events, void *arg)
{
    struct myevent_s *ev = (struct myevent_s *)arg;
    int len;

    // 读文件描述符，数据存入 myevent_s 成员 buf 中
    len = recv(fd, ev->buf, sizeof(ev->buf), 0);

    eventdel(g_efd, ev);

    if (len > 0)
    {
        // 手动添加字符串结束标记
        ev->len = len;
        ev->buf[len] = '\0';
        printf("C[%d]:%s\n", fd, ev->buf);
        // 设置该 fd 对应的回调函数为 senddata
        // eventset(ev, fd, senddata, ev);
        ev->call_back = senddata;
        // 将 fd 加入红黑树 g_efd 中，监听其写事件。
        eventadd(g_efd, EPOLLOUT, ev);
    }
    else if (len == 0)
    {
        close(ev->sfd);
        // ev - g_events 地址相减得到偏移元素位置
        printf("[fd=%d] pos[%ld], closed\n", fd, ev - g_events);
    }
    else
    {
        close(ev->sfd);
        printf("recv[fd=%d], error[%d]:%s\n", fd, errno, strerror(errno));
    }
}

void senddata(int fd, int events, void *arg)
{
    struct myevent_s *ev = (struct myevent_s *)arg;
    int len;
    // 直接将数据回写给客户端，未作处理
    len = send(fd, ev->buf, ev->len, 0);

    if (len > 0)
    {
        printf("send[fd=%d], [%d]%s\n", fd, len, ev->buf);
        // 从红黑树 g_efd 中移除
        eventdel(g_efd, ev);
        // 将该 fd 的回调函数改为 recvdata
        eventset(ev, fd, recvdata, ev);
        // 重新添加到红黑树上，设为监听该事件
        eventadd(g_efd, EPOLLIN, ev);
    }
    else
    {
        eventdel(g_efd, ev);
        close(ev->sfd);
        printf("send[fd=%d] error %s\n", fd, strerror(errno));
    }
}

void initlistensocket(int efd, short port)
{
    int lfd = socket(AF_INET, SOCK_STREAM, 0);
    fcntl(lfd, F_SETFL, O_NONBLOCK); // 非阻塞

    eventset(&g_events[MAX_EVENTS], lfd, acceptconn, &g_events[MAX_EVENTS]);

    eventadd(efd, EPOLLIN, &g_events[MAX_EVENTS]);

    struct sockaddr_in servaddr;
    memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port = htons(port);

    bind(lfd, (struct sockaddr *)&servaddr, sizeof(servaddr));
    listen(lfd, 20);
}

int main(int argc, char *argv[])
{
    unsigned short port = SERV_PORT;
    if (argc == 2)
        port = atoi(argv[1]);

    // 创建 epoll 实例，返回给全局 g_efd
    g_efd = epoll_create(MAX_EVENTS + 1);
    if (g_efd <= 0)
    {
        printf("create efd in %s err %s\n", __func__, strerror(errno));
        return 1;
    }
    // 初始化监听 socket
    initlistensocket(g_efd, port);

    // 保存已经满足就绪事件的文件描述符数组
    struct epoll_event events[MAX_EVENTS + 1];
    printf("server running:port[%d]\n", port);

    int checkpos = 0, i;
    while (1)
    {
        // 超时验证，每次测试 100 个连接，不测试 listenfd。
        // 当客户端 60 秒内没有和服务器通信，则关闭此客户端连接
        long now = time(NULL); // 当前时间

        // 一次循环检测 100 个，使用 checkpos 控制检测对象
        for (i = 0; i < 100; i++, checkpos++)
        {
            if (checkpos == MAX_EVENTS)
                checkpos = 0;
            // 不在红黑树上
            if (g_events[checkpos].status != 1)
                continue;
            
            // 客户端不活跃的时间
            long duration = now - g_events[checkpos].last_active;
            if (duration >= 60)
            {
                // 关闭与该客户端连接
                close(g_events[checkpos].sfd);
                printf("[fd=%d] timeout \n", g_events[checkpos].sfd);
                // 将该客户端从红黑树中 g_efd 删除。
                eventdel(g_efd, &g_events[checkpos]);
            }
        }

        // 每秒钟醒来一次，nfd为0，目的是去检测连接超时的客户端
        int nfd = epoll_wait(g_efd, events, MAX_EVENTS + 1, 1000);
        if (nfd < 0)
        {
            printf("epoll_wait error, wait\n");
            break;
        }
        for (i = 0; i < nfd; i++)
        {
            // 使用自定义结构体 myevent_s 类型指针，接收联合体 data 的 void *ptr 成员
            struct myevent_s *ev = (struct myevent_s *)events[i].data.ptr;
            // 读就绪事件
            if ((events[i].events & EPOLLIN) && (ev->events & EPOLLIN))
            {
                ev->call_back(ev->sfd, events[i].events, ev->arg);
            }
            // 写就绪事件
            if ((events[i].events & EPOLLOUT) && (ev->events & EPOLLOUT))
            {
                ev->call_back(ev->sfd, events[i].events, ev->arg);
            }
        }
    }

    return 0;
}