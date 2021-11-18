#include <unistd.h>
#include <stdio.h>

int main()
{
    pid_t pid = fork();
    if (pid == 0)
    {
        printf("child process pid = %d, pgid=%d, sid=%d \n", getpid(), getpgid(0), getsid(0));
        setsid();
        printf("Changed\n");
        printf("child process pid = %d, pgid=%d, sid=%d \n", getpid(), getpgid(0), getsid(0));
        sleep(10);
    }
    else
    {
        sleep(10);
        
    }

    return 0;
}