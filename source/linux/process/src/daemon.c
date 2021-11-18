#include <unistd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <fcntl.h>

/**
 * 创建守护进程的演示
 */
int main()
{
    pid_t pid = fork();
    if (pid > 0) return 1;
    setsid(); // 建立新会话，脱离控制终端

    int ret;
    ret = chdir("./resource");//改变工作目录
    if (ret == -1)
    {
        perror("chdir error");
        return 1;
    }
    umask(0022);

    close(STDIN_FILENO);
    int fd = open("/dev/null", O_RDWR);
    dup2(fd, STDOUT_FILENO);
    dup2(fd, STDERR_FILENO);
    
    while (1); // 假装有事要做
    return 0;
}