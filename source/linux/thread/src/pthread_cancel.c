#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>

void *fn2(void *arg)
{
    while (1)
    {
        printf("sub thread....\n");
        // sleep(1);
        // pthread_testcancel();
    }
}

/**
 * pthread_cancel 能杀死线程是有限制条件的。
 * 不是任何情况下都能杀死的。系统为其提供了安全点检查机制。
 * 目前已知的安全点有：
 * 1. sleep()
 * 2. pthread_testcancel()
 * 
 * 取消点:
 * 线程取消的方法是向目标线程发Cancel信号，
 * 但如何处理Cancel信号则由目标线程自己决定，或者忽略、或者立即终止、或者继续运行至Cancelation-point（取消点），由不同的Cancelation状态决定。
 * 
 * pthreads标准指定了几个取消点，其中包括：
 * (1)通过pthread_testcancel调用以编程方式建立线程取消点。 
 * (2)线程等待pthread_cond_wait或pthread_cond_timewait()中的特定条件。 
 * (3)被sigwait(2)阻塞的函数 
 * (4)一些标准的库调用。通常，这些调用包括线程可基于阻塞的函数。
 * 
 * 缺省情况下，将启用取消功能。有时，您可能希望应用程序禁用取消功能。如果禁用取消功能，则会导致延迟所有的取消请求，
 * 直到再次启用取消请求。  
 * 根据POSIX标准，pthread_join()、pthread_testcancel()、pthread_cond_wait()、pthread_cond_timedwait()、sem_wait()、sigwait()等函数以及
 * read()、write()等会引起阻塞的系统调用都是Cancelation-point，而其他pthread函数都不会引起Cancelation动作。
 * 但是pthread_cancel的手册页声称，由于LinuxThread库与C库结合得不好，因而目前C库函数都不是Cancelation-point；
 * 但CANCEL信号会使线程从阻塞的系统调用中退出，并置EINTR错误码，
 * 因此可以在需要作为Cancelation-point的系统调用前后调用pthread_testcancel()，
 * 从而达到POSIX标准所要求的目标.
 * 即如下代码段：
 * pthread_testcancel();
 * retcode = read(fd, buffer, length);
 * pthread_testcancel();
 * 
 * 取消类型(Cancellation Type)
 * 我们会发现，通常的说法：某某函数是 Cancellation Points，这种方法是容易令人混淆的。
 * 因为函数的执行是一个时间过程，而不是一个时间点。其实真正的 Cancellation Points 
 * 只是在这些函数中 Cancellation Type 被修改为 PHREAD_CANCEL_ASYNCHRONOUS 和修改回 PTHREAD_CANCEL_DEFERRED 中间的一段时间。
 * POSIX的取消类型有两种，一种是延迟取消(PTHREAD_CANCEL_DEFERRED)，这是系统默认的取消类型，即在线程到达取消点之前，不会出现真正的取消；
 * 另外一种是异步取消(PHREAD_CANCEL_ASYNCHRONOUS)，使用异步取消时，线程可以在任意时间取消。
 * 
 * 线程终止的清理工作

 * Posix的线程终止有两种情况：正常终止和非正常终止。
 * 线程主动调用pthread_exit()或者从线程函数中return都将使线程正常退出，这是可预见的退出方式；
 * 非正常终止是线程在其他线程的干预下，或者由于自身运行出错（比如访问非法地址）而退出，这种退出方式是不可预见的。
 * 不论是可预见的线程终止还是异常终止，都会存在资源释放的问题，在不考虑因运行出错而退出的前提下，如何保证线程终止时能顺利的释放掉自己所占用的资源，特别是锁资源，就是一个必须考虑解决的问题。
 * 最经常出现的情形是资源独占锁的使用：线程为了访问临界资源而为其加上锁，但在访问过程中被外界取消，如果线程处于响应取消状态，且采用异步方式响应，或者在打开独占锁以前的运行路径上存在取消点，
 * 则该临界资源将永远处于锁定状态得不到释放。外界取消操作是不可预见的，因此的确需要一个机制来简化用于资源释放的编程。
 * 在POSIX线程API中提供了一个pthread_cleanup_push()/ pthread_cleanup_pop()函数,
 * 对用于自动释放资源—从pthread_cleanup_push()的调用点到pthread_cleanup_pop()之间的程序段中的终止动作（包括调用pthread_exit()和取消点终止）都将执行pthread_cleanup_push()所指定的清理函数。
 * API定义如下：
 * void pthread_cleanup_push(void (*routine) (void *), void *arg)
 * void pthread_cleanup_pop(int execute)
 * pthread_cleanup_push()/pthread_cleanup_pop()采用先入后出的栈结构管理，void routine(void *arg)函数
 * 在调用pthread_cleanup_push()时压入清理函数栈，多次对pthread_cleanup_push() 的调用将在清理函数栈中形成一个函数链；
 * 从pthread_cleanup_push的调用点到pthread_cleanup_pop之间的程序段中的终止动作（包括调用pthread_exit()和异常终止，不包括return）
 * 都将执行pthread_cleanup_push()所指定的清理函数。
 * 在执行该函数链时按照压栈的相反顺序弹出。execute参数表示执行到 pthread_cleanup_pop()时
 * 是否在弹出清理函数的同时执行该函数，为0表示不执行，非0为执行；这个参数并不影响异常终止时清理函数的执行。
 * 
 */
int main()
{
    pthread_t tid;
    int ret = pthread_create(&tid, NULL, fn2, NULL);
    if (ret != 0)
    {
        fprintf(stderr, "pthread create error %s \n", strerror(ret));
        return 1;
    }
    sleep(1);
    ret = pthread_cancel(tid);
    if (ret != 0)
    {
        fprintf(stderr, "pthread create error %s \n", strerror(ret));
        return 1;
    }
    pthread_exit(0);
}