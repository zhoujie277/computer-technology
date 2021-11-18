#include <unistd.h>
#include <signal.h>
#include <stdio.h>

/**
 * 
 */
int main()
{
    pid_t pid;
    int i = 0;
    for (; i < 5; i++)
    {
        pid = fork();
        if (pid == 0) break;
    }

    if (i == 5)
    {
        int ret = kill(pid, SIGKILL);
        if (ret == -1)
        {
            perror("kill error");
        }
    } else {
        while (1);
    }

    return 1;
}