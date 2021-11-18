#include <unistd.h>
#include <stdio.h>

int main()
{
    printf("fork1\n");
    printf("fork2\n");
    printf("fork3\n");
    printf("fork4\n");
    printf("fork5\n");
    
    int pid = fork();
    if (pid == -1)
    {
        perror("fork was error\n");
    } else if (pid == 0)
    {
        printf("this is child process...%d, parent id is %d\n", getpid(), getppid());
    } else {
        printf("this is father process, the child pid is %d ,self pid %d, parent id %d \n", pid, getpid(), getppid());
    }

    printf("===== fork end =====\n");
    return 0;
}