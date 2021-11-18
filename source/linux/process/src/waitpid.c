#include <sys/wait.h>

/**
 * waitpid 功能演示
 * pid_t pid:
 * pid<-1	等待进程组号为pid绝对值的任何子进程。
 * pid=-1	等待任何子进程，此时的waitpid()函数就退化成了普通的wait()函数。
 * pid=0	等待进程组号与目前进程相同的任何子进程，也就是说任何和调用waitpid()函数的进程在同一个进程组的进程。
 * pid>0	等待进程号为pid的子进程。
 * 
 * 2）int *status
 * 这个参数将保存子进程的状态信息，有了这个信息父进程就可以了解子进程为什么会推出，是正常推出还是出了什么错误。如果status不是空指针，则状态信息将被写入 
 * 器指向的位置。当然，如果不关心子进程为什么推出的话，也可以传入空指针。
 * Linux提供了一些非常有用的宏来帮助解析这个状态信息，这些宏都定义在sys/wait.h头文件中。主要有以下几个：
 * 
 * 3）int options
 * 参数options提供了一些另外的选项来控制waitpid()函数的行为。如果不想使用这些选项，则可以把这个参数设为0。
 * 主要使用的有以下两个选项：
 * WNOHANG	如果pid指定的子进程没有结束，则waitpid()函数立即返回0，而不是阻塞在这个函数上等待；如果结束了，则返回该子进程的进程号。
 * WUNTRACED	如果子进程进入暂停状态，则马上返回。
 *
 * 如果waitpid()函数执行成功，则返回子进程的进程号；如果有错误发生，则返回-1，并且将失败的原因存放在errno变量中。
 * 失败的原因主要有：没有子进程（errno设置为ECHILD），调用被某个信号中断（errno设置为EINTR）或选项参数无效（errno设置为EINVAL）
 * 如果像这样调用waitpid函数：waitpid(-1, status, 0)，这此时waitpid()函数就完全退化成了wait()函数。
 */

int main()
{
    pid_t pid = fork();
    if (pid == -1)
    {
        perror("fork error");
        return 1;
    }
    else if (pid == 0)
    {
        for (int i = 0; i < 10; i++)
        {
            printf("hello, I'm child process...%d \n", getpid());
            // sleep(2);
        }

        return 1;
    }
    else
    {
        // 父进程等待子进程结束
        sleep(2);
        int status;
        int ret = waitpid(pid, &status, WNOHANG);
        printf("hi, I'm parent process... %d the childPid is=%d \n", getpid(), ret);
        
        if (WIFEXITED(status))
        {
            // 子进程自己终止进程
            printf("child process was kill by WIFEXITED status is %d \n", WEXITSTATUS(status));
        } else if (WIFSIGNALED(status))
        {
            // 被信号终止进程
            printf("child process was kill by WIFSIGNALED status is %d \n", WTERMSIG(status));
        }
    }
    return 0;
}