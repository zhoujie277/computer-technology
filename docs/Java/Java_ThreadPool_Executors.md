## Java Thread Pool Executors

[TOC]

### Executors 类组织关系图概览

### 1. Executor
An object that executes submitted Runnable tasks. This interface provides a way of decoupling task submission from the mechanics of how each task will be run, including details of thread use, scheduling, etc. An Executor is normally used instead of explicitly creating threads. For example, rather than invoking new Thread(new(RunnableTask())).start() for each of a set of tasks, you might use:

一个执行提交的 Runnable 任务的对象。这个接口提供了一种将任务提交与每个任务如何运行的机制解耦的方法，包括线程使用、调度等细节。执行者通常被用来代替明确地创建线程。例如，与其为一组任务中的每一个调用 new Thread(new(RunnableTask())).start()，不如使用。

```
    Executor executor = anExecutor;
    executor.execute(new RunnableTask1());
    executor.execute(new RunnableTask2());
    ...
```

However, the Executor interface does not strictly require that execution be asynchronous. In the simplest case, an executor can run the submitted task immediately in the caller's thread:

然而，Executor 接口并不严格要求执行是异步的。在最简单的情况下，一个执行器可以在调用者的线程中立即运行提交的任务。

```
    class DirectExecutor implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }
```

More typically, tasks are executed in some thread other than the caller's thread. The executor below spawns a new thread for each task.

更典型的是，任务是在调用者线程以外的某个线程中执行的。下面的执行器为每个任务生成了一个新的线程。

```
    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
```

Many Executor implementations impose some sort of limitation on how and when tasks are scheduled. The executor below serializes the submission of tasks to a second executor, illustrating a composite executor.

许多执行器的实现对任务的安排方式和时间都有某种限制。下面的执行器将任务的提交序列化到第二个执行器，说明了一个复合执行器。

```
    class SerialExecutor implements Executor {
        final Queue<Runnalbe> tasks = new ArrayDeque<>();
        final Executor executor;
        Runnable active;

        SerialExecutor(Executor executor) {
            this.executor = executor;
        }

        public synchronized void execute(final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                executor.execute(active);
            }
        }
    }
```

The Executor implementations provided in this package implement ExecutorService, which is a more extensive interface. The ThreadPoolExecutor class provides an extensible thread pool implementation. The Executors class provides convenient factory methods for these Executors.

这个包中提供的执行者实现实现了 ExecutorService，它是一个更广泛的接口。ThreadPoolExecutor 类提供了一个可扩展的线程池实现。Executors 类为这些 Executor 提供了方便的工厂方法。

Memory consistency effects: Actions in a thread prior to submitting a Runnable object to an Executor happen-before its execution begins, perhaps in another thread.

内存一致性效应。在将 Runnable 对象提交给 Executor 之前，线程中的行为会在其执行开始之前发生，也许是在另一个线程中。

### 2. ExecutorService
An Executor that provides methods to manage termination and methods that can produce a Future for tracking progress of one or more asynchronous tasks.

一个执行器，它提供了管理终止的方法和可以产生一个 Future 的方法，用于跟踪一个或多个异步任务的进展。

An ExecutorService can be shut down, which will cause it to reject new tasks. Two different methods are provided for shutting down an ExecutorService. The shutdown method will allow previously submitted tasks to execute before terminating, while the shutdownNow method prevents waiting tasks from starting and attempts to stop currently executing tasks. Upon termination, an executor has no tasks actively executing, no tasks awaiting execution, and no new tasks can be submitted. An unused ExecutorService should be shut down to allow reclamation of its resources.

一个 ExecutorService 可以被关闭，这将导致它拒绝新任务。为关闭一个 ExecutorService 提供了两种不同的方法。shutdown 方法将允许先前提交的任务在终止前执行，而 shutdownNow 方法防止等待的任务启动，并试图停止当前执行的任务。终止后，一个执行者没有正在执行的任务，没有等待执行的任务，也没有新的任务可以提交。一个未使用的 ExecutorService 应该被关闭，以允许重新获得其资源。

Method submit extends base method Executor.execute(Runnable) by creating and returning a Future that can be used to cancel execution and/or wait for completion. Methods invokeAny and invokeAll perform the most commonly useful forms of bulk execution, executing a collection of tasks and then waiting for at least one, or all, to complete. (Class ExecutorCompletionService can be used to write customized variants of these methods.)

方法 submit 扩展了基础方法 Executor.execute(Runnable)，创建并返回一个 Future，可以用来取消执行和/或等待完成。方法 invokeAny 和 invokeAll 执行最常用的批量执行形式，执行一个任务集合，然后等待至少一个或全部任务的完成。(类 ExecutorCompletionService 可以用来编写这些方法的自定义变体）。

The Executors class provides factory methods for the executor services provided in this package.

Executors 类为此包中提供的 executor 服务提供工厂方法。

#### Usage Examples
Here is a sketch of a network service in which threads in a thread pool service incoming requests. It uses the preconfigured Executors.newFixedThreadPool factory method:

下面是一个网络服务的草图，其中线程池中的线程为传入的请求提供服务。它使用了预先配置的 Executors.newFixedThreadPool 工厂方法。

```
    class NetworkService implements Runnable {
        private final ServerSocket serverSocket;
        private final ExecutorService pool;

        public NetworkService(int port, int poolSize) throws IOException {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(poolSize);
        }

        public void run() { // run the service
            try {
                for (;;) {
                    pool.execute(new Handler(serverSocket.accept()));
                }
            } catch(IOException ex) {
                pool.shutdown();
            }
        }
    }

    class Handler implements Runnable {
        private final Socket socket;
        Handler(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            // read and service request on socket
        }
    }
```

The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks:

下面的方法分两个阶段关闭一个 ExecutorService，首先调用 shutdown 来拒绝进入的任务，然后如果有必要的话，调用 shutdownNow 来取消任何滞留的任务。

```
    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();    // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-) Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
```

Memory consistency effects: Actions in a thread prior to the submission of a Runnable or Callable task to an ExecutorService happen-before any actions taken by that task, which in turn happen-before the result is retrieved via Future.get().

内存一致性影响。在向 ExecutorService 提交 Runnable 或 Callable 任务之前，线程中的行动发生在该任务采取的任何行动之前，而这些行动又发生在通过 Future.get() 检索的结果之前。

### 3. AbstractExecutorService
Provides default implementations of ExecutorService execution methods. This class implements the submit, invokeAny and invokeAll methods using a RunnableFuture returned by newTaskFor, which defaults to the FutureTask class provided in this package. For example, the implementation of submit(Runnable) creates an associated RunnableFuture that is executed and returned. Subclasses may override the newTaskFor methods to return RunnableFuture implementations other than FutureTask.

提供 ExecutorService 执行方法的默认实现。该类使用 newTaskFor 返回的 RunnableFuture 来实现 submit、invokeAny 和 invokeAll 方法，该类默认为本包中提供的 FutureTask 类。例如，submit(Runnable) 的实现创建了一个相关的 RunnableFuture，它被执行并返回。子类可以覆盖 newTaskFor方法以返回 FutureTask 以外的 RunnableFuture 实现。

Extension example. Here is a sketch of a class that customizes ThreadPoolExecutor to use a CustomTask class instead of the default FutureTask:

```
    public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
        static class CustomTask<V> implements RunnableFuture<V> {...}

        protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
            return new CustomTask<V>(c);
        }

        protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
            return new CustomTask<V>(r, v);
        }
        // ... add constructors, etc.
    }
```

### 4. ScheduledExecutorService
An ExecutorService that can schedule commands to run after a given delay, or to execute periodically.

一个 ExecutorService，可以安排命令在给定的延迟后运行，或定期执行。

The schedule methods create tasks with various delays and return a task object that can be used to cancel or check execution. The scheduleAtFixedRate and scheduleWithFixedDelay methods create and execute tasks that run periodically until cancelled.

schedule方法创建具有各种延迟的任务，并返回一个可用于取消或检查执行的任务对象。scheduleAtFixedRate和scheduleWithFixedDelay方法创建并执行定期运行直到取消的任务。

Commands submitted using the Executor.execute(Runnable) and ExecutorService submit methods are scheduled with a requested delay of zero. Zero and negative delays (but not periods) are also allowed in schedule methods, and are treated as requests for immediate execution.

使用Executor.execute(Runnable)和ExecutorService提交方法提交的命令被安排在一个要求为零的延迟上。在时间表方法中也允许零和负延迟（但不是周期），并被视为立即执行的请求。

All schedule methods accept relative delays and periods as arguments, not absolute times or dates. It is a simple matter to transform an absolute time represented as a java.util.Date to the required form. For example, to schedule at a certain future date, you can use: schedule(task, date.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS). Beware however that expiration of a relative delay need not coincide with the current Date at which the task is enabled due to network time synchronization protocols, clock drift, or other factors.

所有计划方法都接受相对延迟和周期作为参数，而不是绝对时间或日期。将以 java.util.Date 表示的绝对时间转化为所需的形式是一件简单的事情。例如，要在某个未来的日期安排，你可以使用：schedule(task, date.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)。但是要注意，由于网络时间同步协议、时钟漂移或其他因素，相对延迟的到期时间不一定与当前任务启用的日期一致。

The Executors class provides convenient factory methods for the ScheduledExecutorService implementations provided in this package.

Executors 类为这个包中提供的 ScheduledExecutorService 实现提供了方便的工厂方法。

#### Usage Sample
Here is a class with a method that sets up a ScheduledExecutorService to beep every ten seconds for an hour:

这里有一个类，它有一个方法可以设置一个 ScheduledExecutorService，在一小时内每十秒发出一次哔哔声。

```
    import static java.util.concurrent.TimeUnit.*;
    class BeeperControl {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public void beepForAnHour() {
            final Runnable beeper = new Runnable() {
                public void run() { System.out.println("beep"); }
            };
            final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
            scheduler.schedule(new Runnable() {
                public void run() { beeperHandle.cancel(true); }
            }, 60 * 60, SECONDS);
        }
    }
```

### 5. ThreadPoolExecutor
An ExecutorService that executes each submitted task using one of possibly several pooled threads, normally configured using Executors factory methods.

一个 ExecutorService，使用可能的几个池化线程中的一个来执行每个提交的任务，通常使用Executors 工厂方法来配置。

Thread pools address two different problems: they usually provide improved performance when executing large numbers of asynchronous tasks, due to reduced per-task invocation overhead, and they provide a means of bounding and managing the resources, including threads, consumed when executing a collection of tasks. Each ThreadPoolExecutor also maintains some basic statistics, such as the number of completed tasks.

线程池解决了两个不同的问题：由于**减少了每个任务的调用开销**，它们通常在执行大量异步任务时提供了更好的性能，并且它们提供了一种约束和管理资源的方法，包括线程，在执行任务集合时的消耗。每个ThreadPoolExecutor 还维护一些基本的统计数据，如已完成的任务数量。

To be useful across a wide range of contexts, this class provides many adjustable parameters and extensibility hooks. However, programmers are urged to use the more convenient Executors factory methods Executors.newCachedThreadPool (unbounded thread pool, with automatic thread reclamation), Executors.newFixedThreadPool (fixed size thread pool) and Executors.newSingleThreadExecutor (single background thread), that preconfigure settings for the most common usage scenarios. Otherwise, use the following guide when manually configuring and tuning this class:

为了在各种情况下都能发挥作用，这个类提供了许多可调整的参数和可扩展的钩子。然而，我们敦促程序员使用更方便的 Executors 工厂方法 Executors.newCachedThreadPool （无界线程池，自动回收线程）、 Executors.newFixedThreadPool （固定大小的线程池）和 Executors.newSingleThreadExecutor（单后台线程），它们为最常见的使用场景预先配置了设置。否则，在手动配置和调整该类时，请使用以下指南。

#### Core and maximum pool sizes
A ThreadPoolExecutor will automatically adjust the pool size (see getPoolSize) according to the bounds set by corePoolSize (see getCorePoolSize) and maximumPoolSize (see getMaximumPoolSize). When a new task is submitted in method execute(Runnable), and fewer than corePoolSize threads are running, a new thread is created to handle the request, even if other worker threads are idle. If there are more than corePoolSize but less than maximumPoolSize threads running, a new thread will be created only if the queue is full. By setting corePoolSize and maximumPoolSize the same, you create a fixed-size thread pool. By setting maximumPoolSize to an essentially unbounded value such as Integer.MAX_VALUE, you allow the pool to accommodate an arbitrary number of concurrent tasks. Most typically, core and maximum pool sizes are set only upon construction, but they may also be changed dynamically using setCorePoolSize and setMaximumPoolSize.

ThreadPoolExecutor 会根据 corePoolSize （见 getCorePoolSize ）和 maximumPoolSize（见 getMaximumPoolSize ）所设定的范围自动调整池的大小（见 getPoolSize ）。当一个新的任务在方法 execute(Runnable) 中提交时，如果运行的线程少于 corePoolSize ，就会创建一个新的线程来处理该请求，即使其他工作线程处于空闲状态。如果运行的线程多于 corePoolSize 但少于maximumPoolSize，只有在队列满了的情况下才会创建一个新的线程。通过设置 corePoolSize 和 maximumPoolSize 相同，你可以创建一个固定规模的线程池。通过将 maximumPoolSize 设置为一个本质上无限制的值，如 Integer.MAX_VALUE，你允许池子容纳任意数量的并发任务。最典型的是，核心和最大线程池大小只在构建时设置，但也可以使用 setCorePoolSize 和 setMaximumPoolSize 动态地改变它们。

#### On-demand construction
By default, even core threads are initially created and started only when new tasks arrive, but this can be overridden dynamically using method prestartCoreThread or prestartAllCoreThreads. You probably want to prestart threads if you construct the pool with a non-empty queue.

默认情况下，即使是核心线程也是最初创建的，并且只有在新任务到达时才会启动，但这可以通过 prestartCoreThread 或 prestartAllCoreThreads 方法来动态地重写。如果你用一个非空的队列构建池子，你可能想要预启动线程。

#### Creating new threads
New threads are created using a ThreadFactory. If not otherwise specified, a Executors.defaultThreadFactory is used, that creates threads to all be in the same ThreadGroup and with the same NORM_PRIORITY priority and non-daemon status. By supplying a different ThreadFactory, you can alter the thread's name, thread group, priority, daemon status, etc. If a ThreadFactory fails to create a thread when asked by returning null from newThread, the executor will continue, but might not be able to execute any tasks. Threads should possess the "modifyThread" RuntimePermission. If worker threads or other threads using the pool do not possess this permission, service may be degraded: configuration changes may not take effect in a timely manner, and a shutdown pool may remain in a state in which termination is possible but not completed.

新线程是使用 ThreadFactory 创建的。如果没有另外指定，就会使用 Executors.defaultThreadFactory，它创建的线程都在同一个线程组中，具有相同的 NORM_PRIORITY 优先级和非守护状态。通过提供一个不同的 ThreadFactory，你可以改变线程的名称、线程组、优先级、守护状态等。如果一个 ThreadFactory 在被要求时未能通过从 newThread 返回 null 来创建一个线程，那么执行器将继续，但可能无法执行任何任务。线程应该拥有 "modifyThread "的 RuntimePermission。如果工作线程或使用池子的其他线程不具备这种权限，服务可能会下降：配置更改可能不会及时生效，而且关闭的池子可能会保持在可能终止但未完成的状态。

#### Keep-alive times
If the pool currently has more than corePoolSize threads, excess threads will be terminated if they have been idle for more than the keepAliveTime (see getKeepAliveTime(TimeUnit)). This provides a means of reducing resource consumption when the pool is not being actively used. If the pool becomes more active later, new threads will be constructed. This parameter can also be changed dynamically using method setKeepAliveTime(long, TimeUnit). Using a value of Long.MAX_VALUE TimeUnit.NANOSECONDS effectively disables idle threads from ever terminating prior to shut down. By default, the keep-alive policy applies only when there are more than corePoolSize threads. But method allowCoreThreadTimeOut(boolean) can be used to apply this time-out policy to core threads as well, so long as the keepAliveTime value is non-zero.

如果池子当前有超过 corePoolSize 的线程，如果多余的线程闲置时间超过 keepAliveTime（见 getKeepAliveTime(TimeUnit) ），它们将被终止。这提供了一种在池子没有被积极使用时减少资源消耗的方法。如果池子以后变得更加活跃，新的线程将被构建。这个参数也可以通过 setKeepAliveTime(long, TimeUnit) 方法动态地改变。使用 Long.MAX_VALUE TimeUnit.NANOSECONDS 的值可以有效地禁止闲置线程在关闭前终止。默认情况下，只有当线程数量超过 corePoolSize 时，才会适用keep-alive 策略。但是方法 allowCoreThreadTimeOut(boolean) 也可以用来对核心线程应用这个超时策略，只要 keepAliveTime 的值不为零。

#### Queuing
Any BlockingQueue may be used to transfer and hold submitted tasks. The use of this queue interacts with pool sizing:
+ If fewer than corePoolSize threads are running, the Executor always prefers adding a new thread rather than queuing.
+ If corePoolSize or more threads are running, the Executor always prefers queuing a request rather than adding a new thread.
+ If a request cannot be queued, a new thread is created unless this would exceed maximumPoolSize, in which case, the task will be rejected.

任何 BlockingQueue 都可以用来传输和保持提交的任务。该队列的使用与池的大小相互作用。
+ 如果运行的线程少于 corePoolSize，执行器总是倾向于添加一个新的线程而不是排队。
+ 如果corePoolSize 或更多的线程正在运行，执行器总是倾向于排队请求，而不是增加一个新的线程。
+ 如果一个请求不能被排队，就会创建一个新的线程，除非这将超过 maximumPoolSize，在这种情况下，任务会被拒绝。

There are three general strategies for queuing:

1. Direct handoffs. A good default choice for a work queue is a SynchronousQueue that hands off tasks to threads without otherwise holding them. Here, an attempt to queue a task will fail if no threads are immediately available to run it, so a new thread will be constructed. This policy avoids lockups when handling sets of requests that might have internal dependencies. Direct handoffs generally require unbounded maximumPoolSizes to avoid rejection of new submitted tasks. This in turn admits the possibility of unbounded thread growth when commands continue to arrive on average faster than they can be processed.
2. Unbounded queues. Using an unbounded queue (for example a LinkedBlockingQueue without a predefined capacity) will cause new tasks to wait in the queue when all corePoolSize threads are busy. Thus, no more than corePoolSize threads will ever be created. (And the value of the maximumPoolSize therefore doesn't have any effect.) This may be appropriate when each task is completely independent of others, so tasks cannot affect each others execution; for example, in a web page server. While this style of queuing can be useful in smoothing out transient bursts of requests, it admits the possibility of unbounded work queue growth when commands continue to arrive on average faster than they can be processed.
3. Bounded queues. A bounded queue (for example, an ArrayBlockingQueue) helps prevent resource exhaustion when used with finite maximumPoolSizes, but can be more difficult to tune and control. Queue sizes and maximum pool sizes may be traded off for each other: Using large queues and small pools minimizes CPU usage, OS resources, and context-switching overhead, but can lead to artificially low throughput. If tasks frequently block (for example if they are I/O bound), a system may be able to schedule time for more threads than you otherwise allow. Use of small queues generally requires larger pool sizes, which keeps CPUs busier but may encounter unacceptable scheduling overhead, which also decreases throughput.

1. 直接交接。工作队列的一个很好的默认选择是同步队列（SynchronousQueue），它将任务移交给线程，而不持有它们。在这里，如果没有线程可以立即运行一个任务，那么排队的尝试就会失败，因此将构建一个新的线程。在处理可能有内部依赖关系的请求集时，这种策略可以避免锁定。直接交接通常需要不受限制的最大池大小，以避免拒绝新提交的任务。这反过来又承认，当命令继续以平均速度到达时，有可能出现无限制的线程增长。
2. 无界的队列。使用无界队列（例如没有预定容量的 LinkedBlockingQueue ）将导致新任务在所有corePoolSize 线程都忙碌时在队列中等待。因此，永远不会有超过 corePoolSize 的线程被创建。(因此，maximumPoolSize 的值没有任何影响。)当每个任务完全独立于其他任务时，这可能是合适的，所以任务不能影响彼此的执行；例如，在一个网页服务器中。虽然这种排队方式在平滑瞬时爆发的请求方面很有用，但当命令继续以平均速度到达时，它承认有可能出现无限制的工作队列增长。
3. 有界队列。有界队列（例如 ArrayBlockingQueue）在与有限的最大池大小一起使用时有助于防止资源耗尽，但可能更难调整和控制。队列大小和最大池大小可以相互交换。使用大队列和小池子可以最大限度地减少 CPU 使用量、操作系统资源和上下文切换开销，但会导致人为地降低吞吐量。如果任务经常阻塞（例如，如果它们是 I/O 绑定的），系统可能能够为更多的线程安排时间，而不是你所允许的。使用小队列通常需要更大的池子，这使 CPU 更加繁忙，但可能会遇到不可接受的调度开销，这也降低了吞吐量。

#### Rejected tasks
New tasks submitted in method execute(Runnable) will be rejected when the Executor has been shut down, and also when the Executor uses finite bounds for both maximum threads and work queue capacity, and is saturated. In either case, the execute method invokes the RejectedExecutionHandler.rejectedExecution(Runnable, ThreadPoolExecutor) method of its RejectedExecutionHandler. Four predefined handler policies are provided:
1. In the default ThreadPoolExecutor.AbortPolicy, the handler throws a runtime RejectedExecutionException upon rejection.
2. In ThreadPoolExecutor.CallerRunsPolicy, the thread that invokes execute itself runs the task. This provides a simple feedback control mechanism that will slow down the rate that new tasks are submitted.
3. In ThreadPoolExecutor.DiscardPolicy, a task that cannot be executed is simply dropped.
4. In ThreadPoolExecutor.DiscardOldestPolicy, if the executor is not shut down, the task at the head of the work queue is dropped, and then execution is retried (which can fail again, causing this to be repeated.)

It is possible to define and use other kinds of RejectedExecutionHandler classes. Doing so requires some care especially when policies are designed to work only under particular capacity or queuing policies.

在 execute(Runnable) 方法中提交的任务，在执行器被关闭时，以及在执行器使用最大线程和工作队列容量的有限界限并达到饱和时，将被拒绝。在这两种情况下，execute 方法都会调用其RejectedExecutionHandler.rejectedExecution(Runnable, ThreadPoolExecutor)方法的RejectedExecutionHandler。提供了四个预定义的处理策略。
1.  在默认的 ThreadPoolExecutor.AbortPolicy 中，处理程序在拒绝时抛出一个运行时RejectedExecutionException。
2.  在 ThreadPoolExecutor.CallerRunsPolicy 中，调用 executor 的线程本身运行任务。这提供了一个简单的反馈控制机制，将减慢新任务的提交速度。
3.  在 ThreadPoolExecutor.DiscardPolicy中，无法执行的任务被简单地丢弃。
4.  在 ThreadPoolExecutor.DiscardOldestPolicy 中，如果执行器没有关闭，工作队列头部的任务就会被丢弃，然后重新执行（可能再次失败，导致这种情况重复发生）。

定义和使用其他类型的 RejectedExecutionHandler 类是可能的。这样做需要注意一些问题，特别是当策略被设计为只在特定的容量或排队策略下工作时。

#### Hook methods
This class provides protected overridable beforeExecute(Thread, Runnable) and afterExecute(Runnable, Throwable) methods that are called before and after execution of each task. These can be used to manipulate the execution environment; for example, reinitializing ThreadLocals, gathering statistics, or adding log entries. Additionally, method terminated can be overridden to perform any special processing that needs to be done once the Executor has fully terminated.

If hook or callback methods throw exceptions, internal worker threads may in turn fail and abruptly terminate.

该类提供了受保护的可重写的 beforeExecute(Thread, Runnable) 和 afterExecute(Runnable, Throwable) 方法，在每个任务执行之前和之后被调用。这些方法可以用来操作执行环境；例如，重新初始化 ThreadLocals，收集统计数据，或添加日志条目。此外，方法终止可以被重写，以执行任何需要在执行器完全终止后进行的特殊处理。

如果钩子或回调方法抛出异常，内部工作线程可能反过来失败并突然终止。

#### Queue maintenance
Method getQueue() allows access to the work queue for purposes of monitoring and debugging. Use of this method for any other purpose is strongly discouraged. Two supplied methods, remove(Runnable) and purge are available to assist in storage reclamation when large numbers of queued tasks become cancelled.

方法 getQueue() 允许访问工作队列，以达到监控和调试的目的。强烈建议不要将此方法用于任何其他目的。两个提供的方法，remove(Runnable) 和 purge 是可用的，当大量的队列任务被取消时，可以帮助存储回收。

#### Finalization
A pool that is no longer referenced in a program AND has no remaining threads will be shutdown automatically. If you would like to ensure that unreferenced pools are reclaimed even if users forget to call shutdown, then you must arrange that unused threads eventually die, by setting appropriate keep-alive times, using a lower bound of zero core threads and/or setting allowCoreThreadTimeOut(boolean).

一个在程序中不再被引用并且没有剩余线程的池将被自动关闭。如果你想确保即使用户忘记调用 shutdown，未被引用的池也能被回收，那么你必须通过设置适当的保持时间，使用零核心线程的下限和/或设置 allowCoreThreadTimeOut(boolean)，来安排未使用的线程最终死亡。

#### Extension example
Most extensions of this class override one or more of the protected hook methods. For example, here is a subclass that adds a simple pause/resume feature:

这个类的大多数扩展都覆盖了一个或多个受保护的钩子方法。例如，这里有一个子类，增加了一个简单的暂停/恢复功能。

```
    class PausableThreadPoolExecutor extends ThreadPoolExecutor {
        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        public PausableThreadPoolExecutor(...) { super(...); }

        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) unpaused.await();
            } catch (InterruptedException ie) {
                t.iterrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        public void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pausedLock.unlock();
            }
        }

        public void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }
```

### 6. ScheduledThreadPoolExecutor
A ThreadPoolExecutor that can additionally schedule commands to run after a given delay, or to execute periodically. This class is preferable to Timer when multiple worker threads are needed, or when the additional flexibility or capabilities of ThreadPoolExecutor (which this class extends) are required.

一个 ThreadPoolExecutor，可以额外安排命令在给定的延迟后运行，或定期执行。当需要多个工作线程，或者需要 ThreadPoolExecutor（该类的扩展）的额外灵活性或能力时，该类比 Timer 更受欢迎。

Delayed tasks execute no sooner than they are enabled, but without any real-time guarantees about when, after they are enabled, they will commence. Tasks scheduled for exactly the same execution time are enabled in first-in-first-out (FIFO) order of submission.

延迟任务的执行时间不早于它们被启用的时间，但对它们被启用后何时开始执行没有任何实时保证。为完全相同的执行时间安排的任务以先进先出（FIFO）的提交顺序启用。

When a submitted task is cancelled before it is run, execution is suppressed. By default, such a cancelled task is not automatically removed from the work queue until its delay elapses. While this enables further inspection and monitoring, it may also cause unbounded retention of cancelled tasks. To avoid this, set setRemoveOnCancelPolicy to true, which causes tasks to be immediately removed from the work queue at time of cancellation.

当一个提交的任务在运行前被取消时，执行会被抑制。默认情况下，这种被取消的任务不会被自动从工作队列中移除，直到其延迟期结束。虽然这可以实现进一步的检查和监控，但也可能导致对已取消的任务的无限制保留。为了避免这种情况，将setRemoveOnCancelPolicy 设置为 true，这将导致任务在取消时立即从工作队列中删除。

Successive executions of a task scheduled via scheduleAtFixedRate or scheduleWithFixedDelay do not overlap. While different executions may be performed by different threads, the effects of prior executions happen-before those of subsequent ones.

通过 scheduleAtFixedRate 或 scheduleWithFixedDelay 安排的任务的连续执行不会发生重叠。虽然不同的执行可以由不同的线程执行，但之前的执行的效果会先于后面的执行发生。

While this class inherits from ThreadPoolExecutor, a few of the inherited tuning methods are not useful for it. In particular, because it acts as a fixed-sized pool using corePoolSize threads and an unbounded queue, adjustments to maximumPoolSize have no useful effect. Additionally, it is almost never a good idea to set corePoolSize to zero or use allowCoreThreadTimeOut because this may leave the pool without threads to handle tasks once they become eligible to run.

虽然这个类继承了 ThreadPoolExecutor，但继承的一些调整方法对它并没有用。特别是，由于它作为一个使用 corePoolSize 线程和无界队列的固定大小的池，对 maximumPoolSize 的调整没有任何作用。此外，将 corePoolSize 设置为零或使用 allowCoreThreadTimeOut 几乎不是一个好主意，因为这可能会使池子里没有线程来处理一旦有资格运行的任务。

Extension notes: This class overrides the execute and submit methods to generate internal ScheduledFuture objects to control per-task delays and scheduling. To preserve functionality, any further overrides of these methods in subclasses must invoke superclass versions, which effectively disables additional task customization. However, this class provides alternative protected extension method decorateTask (one version each for Runnable and Callable) that can be used to customize the concrete task types used to execute commands entered via execute, submit, schedule, scheduleAtFixedRate, and scheduleWithFixedDelay. By default, a ScheduledThreadPoolExecutor uses a task type extending FutureTask. However, this may be modified or replaced using subclasses of the form:

扩展说明。这个类重写了 execute 和 submit 方法，以生成内部的 ScheduledFuture 对象来控制每个任务的延迟和调度。为了保留功能，在子类中对这些方法的任何进一步重写都必须调用超类的版本，这就有效地禁止了额外的任务定制。然而，这个类提供了另一个受保护的扩展方法 decorateTask（Runnable 和 Callable 各有一个版本），可以用来定制具体的任务类型，用于执行通过 execute、submit、schedule、scheduleAtFixedRate 和 scheduleWithFixedDelay 输入的命令。默认情况下，一个 ScheduledThreadPoolExecutor 使用一个扩展了 FutureTask 的任务类型。然而，这可以通过使用以下形式的子类来修改或替换。

```
    public class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
        static class CustomTask<V> implements RunnableScheduledFuture<V> { ...}

        protected <V> RunnableScheduledFuture<V> decorateTask(Runanble r, RunnableScheduledFuture<V> task) {
            return new CustomTask<V>(r, task);
        }

        protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> c, RunnableScheduledFuture<V> task) {
            return new CustomTask<V>(c, task);
        }
        // ... add constructors, etc.
    }
```

This class specializes ThreadPoolExecutor implementation by
1. Using a custom task type, ScheduledFutureTask for tasks, even those that don't require scheduling (i.e., those submitted using ExecutorService execute, not ScheduledExecutorService methods) which are treated as delayed tasks with a delay of zero.
2. Using a custom queue (DelayedWorkQueue), a variant of unbounded DelayQueue. The lack of capacity constraint and the fact that corePoolSize and maximumPoolSize are effectively identical simplifies some execution mechanics (see delayedExecute) compared to ThreadPoolExecutor.
3. Supporting optional run-after-shutdown parameters, which leads to overrides of shutdown methods to remove and cancel tasks that should NOT be run after shutdown, as well as different recheck logic when task (re)submission overlaps with a shutdown.
4. Task decoration methods to allow interception and instrumentation, which are needed because subclasses cannot otherwise override submit methods to get this effect. These don't have any impact on pool control logic though.

该类通过以下方式实现ThreadPoolExecutor的专业化
1. 使用自定义任务类型 ScheduledFutureTask 的任务，即使是那些不需要调度的任务（即那些使用 ExecutorService 执行，而不是 ScheduledExecutorService 方法提交的任务），这些任务被视为延迟的任务，延迟为零。
2. 使用一个自定义队列（DelayedWorkQueue），是无界的 DelayQueue 的变种。与 ThreadPoolExecutor 相比，缺乏容量约束以及 corePoolSize 和 maximumPoolSize 实际上是相同的，简化了一些执行机制（见delayedExecute）。
3. 支持可选的 shutdown 后运行参数，这导致了对 shutdown 方法的覆盖，以删除和取消那些不应该在关机后运行的任务，以及当任务（重新）提交与 shutdown 重叠时不同的重新检查逻辑。
4. 任务装饰方法，以允许拦截和检测，这是需要的，因为子类不能覆盖提交方法来获得这种效果。不过这些对池的控制逻辑没有任何影响。

#### DelayedWorkQueue
A DelayedWorkQueue is based on a heap-based data structure like those in DelayQueue and PriorityQueue, except that every ScheduledFutureTask also records its index into the heap array. This eliminates the need to find a task upon cancellation, greatly speeding up removal (down from O(n) to O(log n)), and reducing garbage retention that would otherwise occur by waiting for the element to rise to top before clearing. But because the queue may also hold RunnableScheduledFutures that are not ScheduledFutureTasks, we are not guaranteed to have such indices available, in which case we fall back to linear search. (We expect that most tasks will not be decorated, and that the faster cases will be much more common.) 

DelayedWorkQueue 是基于一个基于堆的数据结构，就像 DelayQueue 和 PriorityQueue 中的数据结构一样，只是每个 ScheduledFutureTask 也将其索引记录到堆阵列中。这消除了在取消任务时寻找任务的需要，大大加快了清除速度（从 O(n) 下降到 O(log n)），并减少了垃圾保留，否则在清除前等待元素上升到顶部会出现垃圾。但是，由于队列也可能持有不是 ScheduledFutureTasks 的 RunnableScheduledFutures，我们不能保证有这样的索引可用，在这种情况下，我们会退回到线性搜索。(我们期望大多数任务不会被装饰，而且更快的情况会更常见）。

All heap operations must record index changes -- mainly within siftUp and siftDown. Upon removal, a task's heapIndex is set to -1. Note that ScheduledFutureTasks can appear at most once in the queue (this need not be true for other kinds of tasks or work queues), so are uniquely identified by heapIndex.

所有的堆操作都必须记录索引的变化--主要是在 siftUp 和 siftDown 中。在移除时，一个任务的堆索引被设置为 -1。注意，ScheduledFutureTasks 在队列中最多只能出现一次（其他类型的任务或工作队列不必如此），所以是由heapIndex 唯一识别的。

### 7. ForkJoinPool
An ExecutorService for running ForkJoinTasks. A ForkJoinPool provides the entry point for submissions from non-ForkJoinTask clients, as well as management and monitoring operations.

一个用于运行 ForkJoinTasks 的 ExecutorService。ForkJoinPool 为来自非 ForkJoinTask 客户端的提交提供入口点，以及管理和监控操作。

ForkJoinPool 与其他类型的 ExecutorService 的不同之处主要在于它采用了工作窃取的方式：池中的所有线程都试图找到并执行提交给池和/或由其他活动任务创建的任务（如果没有工作，最终会阻塞等待）。当大多数任务产生其他子任务时（就像大多数 ForkJoinTasks 一样），以及当许多小任务从外部客户端提交到池中时，这使得处理效率提高。特别是当在构造函数中设置 asyncMode 为 true 时，ForkJoinPools 也可能适合用于从未加入的事件式任务。

A static commonPool() is available and appropriate for most applications. The common pool is used by any ForkJoinTask that is not explicitly submitted to a specified pool. Using the common pool normally reduces resource usage (its threads are slowly reclaimed during periods of non-use, and reinstated upon subsequent use).

一个静态的 commonPool() 是可用的，并且适合于大多数应用。公共池被任何没有明确提交给指定池的ForkJoinTask 所使用。使用公共池通常会减少资源的使用（它的线程在不使用期间会被慢慢回收，并在随后的使用中恢复）。

For applications that require separate or custom pools, a ForkJoinPool may be constructed with a given target parallelism level; by default, equal to the number of available processors. The pool attempts to maintain enough active (or available) threads by dynamically adding, suspending, or resuming internal worker threads, even if some tasks are stalled waiting to join others. However, no such adjustments are guaranteed in the face of blocked I/O or other unmanaged synchronization. The nested ForkJoinPool.ManagedBlocker interface enables extension of the kinds of synchronization accommodated.

对于需要单独或自定义池的应用，可以用给定的目标并行度构建 ForkJoinPool；默认情况下，等于可用处理器的数量。该池试图通过动态添加、暂停或恢复内部工作线程来保持足够的活动（或可用）线程，即使一些任务停滞不前等待加入其他任务。然而，在面对阻塞的 I/O 或其他无人管理的同步时，不能保证这种调整。嵌套的ForkJoinPool.ManagedBlocker 接口可以扩展所容纳的同步类型。

In addition to execution and lifecycle control methods, this class provides status check methods (for example getStealCount) that are intended to aid in developing, tuning, and monitoring fork/join applications. Also, method toString returns indications of pool state in a convenient form for informal monitoring.

除了执行和生命周期控制方法外，该类还提供了状态检查方法（例如 getStealCount），旨在帮助开发、调整和监控fork/join应用程序。另外，toString 方法以一种方便的形式返回池状态的指示，用于非正式的监控。

As is the case with other ExecutorServices, there are three main task execution methods summarized in the following table. These are designed to be used primarily by clients not already engaged in fork/join computations in the current pool. The main forms of these methods accept instances of ForkJoinTask, but overloaded forms also allow mixed execution of plain Runnable- or Callable- based activities as well. However, tasks that are already executing in a pool should normally instead use the within-computation forms listed in the table unless using async event-style tasks that are not usually joined, in which case there is little difference among choice of methods.

与其他 ExecutorServices 的情况一样，有三种主要的任务执行方法，在下表中进行了总结。这些方法被设计成主要由尚未在当前池中参与分叉/联合计算的客户使用。这些方法的主要形式接受 ForkJoinTask 的实例，但重载形式也允许混合执行基于 Runnable 或 Callable 的普通活动。然而，已经在池中执行的任务通常应该使用表中列出的计算内形式，除非使用通常不被连接的异步事件式任务，在这种情况下，选择方法的区别不大。

If a SecurityManager is present and no factory is specified, then the default pool uses a factory supplying threads that have no Permissions enabled. The system class loader is used to load these classes. Upon any error in establishing these settings, default parameters are used. It is possible to disable or limit the use of threads in the common pool by setting the parallelism property to zero, and/or using a factory that may return null. However doing so may cause unjoined tasks to never be executed.

如果存在一个 SecurityManager，并且没有指定工厂，那么默认池使用一个工厂提供没有启用 Permissions 的线程。系统类加载器被用来加载这些类。在建立这些设置时出现任何错误，都会使用默认参数。可以通过将并行性属性设置为零，和/或使用一个可能返回 null 的工厂，来禁用或限制公共池中的线程的使用。然而，这样做可能导致未连接的任务永远不会被执行。

Implementation notes: This implementation restricts the maximum number of running threads to 32767. Attempts to create pools with greater than the maximum number result in IllegalArgumentException.

实现说明。该实现将运行线程的最大数量限制为 32767。试图创建超过最大数量的池，会导致IllegalArgumentException。

This implementation rejects submitted tasks (that is, by throwing RejectedExecutionException) only when the pool is shut down or internal resources have been exhausted.

这个实现只有在池关闭或内部资源耗尽时才会拒绝提交的任务（也就是抛出RejectedExecutionException）。

#### Summary of task execution methods
-------  | Call from non-fork/join clients | call from within fork/join computations
----------------------  | ---------------------- | ---------------------
Arrange async execution | execute(ForkJoinTask)  | ForkJoinTask.fork
Await and obtain result | invoke(ForkJoinTask)   | ForkJoinTask.invoke
Arrange exec and obtain Future | submit(ForkJoinTask) | ForkJoinTask.fork(ForkJoinTasks are Futures)

The common pool is by default constructed with default parameters, but these may be controlled by setting three system properties:

公用池默认是用默认参数构建的，但这些参数可以通过设置三个系统属性来控制。
+ java.util.concurrent.ForkJoinPool.common.parallelism - the parallelism level, a non-negative integer
+ java.util.concurrent.ForkJoinPool.common.threadFactory - the class name of a ForkJoinPool.ForkJoinWorkerThreadFactory
+ java.util.concurrent.ForkJoinPool.common.exceptionHandler - the class name of a Thread.UncaughtExceptionHandler

#### Implementation Overview
This class and its nested classes provide the main functionality and control for a set of worker threads: Submissions form non-FJ threads enter into submission queues. Workers take these tasks and typically split them into subtasks that may be stolen by other workers. Preference rules give first priority to processing tasks from their own queues (LIFO or FIFO, depending on mode), then to randomized FIFO steals of tasks in other queues. This framework began as vehicle for supporting tree-structured parallelism using work-stealing. Over time, its scalability advantages led to extensions and changes to better support more diverse usage contexts. Because most internal methods and nested classes are interrelated, their main rationale and descriptions are presented here; individual methods and nested classes contain only brief comments about details.

这个类和它的嵌套类为一组工作线程提供主要功能和控制。形成非 FJ 线程的提交任务进入提交队列。工作者接受这些任务并通常将其分割成可能被其他工作者窃取的子任务。优先权规则优先处理自己队列中的任务（LIFO 或FIFO，取决于模式），然后是随机的 FIFO 偷取其他队列中的任务。这个框架一开始是作为支持树状结构并行性的工具，使用工作偷窃。随着时间的推移，它的可扩展性优势导致了扩展和变化，以更好地支持更多不同的使用环境。因为大多数内部方法和嵌套类都是相互关联的，所以这里介绍了它们的主要原理和描述；个别方法和嵌套类只包含对细节的简要评论。

#### WorkQueues
Most operations occur within work-stealing queues (in nested class WorkQueue).  These are special forms of Deques that support only three of the four possible end-operations -- push, pop, and poll (aka steal), under the further constraints that push and pop are called only from the owning thread (or, as extended here, under a lock), while poll may be called from other threads.  (If you are unfamiliar with them, you probably want to read Herlihy and Shavit's book "The Art of Multiprocessor programming", chapter 16 describing these in more detail before proceeding.)  The main work-stealing queue design is roughly similar to those in the papers "Dynamic Circular Work-Stealing Deque" by Chase and Lev, SPAA 2005 (http://research.sun.com/scalable/pubs/index.html) and "Idempotent work stealing" by Michael, Saraswat, and Vechev, PPoPP 2009 (http://portal.acm.org/citation.cfm?id=1504186). The main differences ultimately stem from GC requirements that we null out taken slots as soon as we can, to maintain as small a footprint as possible even in programs generating huge numbers of tasks. To accomplish this, we shift the CAS arbitrating pop vs poll (steal) from being on the indices ("base" and "top") to the slots themselves.

大多数操作都发生在窃取工作的队列中（在嵌套类 WorkQueue 中）。 这些是 Deques 的特殊形式，只支持四种可能的终端操作中的三种--push、pop 和 poll（又称偷窃），在进一步的限制下，push 和 pop只能从拥有线程中调用（或者，如这里的扩展，在一个锁下），而 poll 可以从其他线程中调用。 (如果你对它们不熟悉，你可能想读读 Herlihy 和 Shavit的书 "The Art of Multiprocessor Programming"，在继续之前，第 16 章更详细地描述了这些内容）。 主要的工作窃取队列设计与 Chase 和 Lev 的 "Dynamic Circular Work-Stealing Deque"，SPAA 2005（http://research.sun.com/scalable/pubs/index.html） 和 Michael、Saraswat 和 Vechev的 "Idempotent work stealing"，PPoPP 2009（http://portal.acm.org/citation.cfm?id=1504186）中的论文大致相似。主要的区别最终来自于 GC 的要求，即我们要尽快将所占用的槽位清空，即使在产生大量任务的程序中也要保持尽可能小的足迹。为了达到这个目的，我们将 CAS 对 pop 与 poll（窃取）的仲裁从索引（"base "和 "top"）上转移到槽本身。

Adding tasks then takes the form of a classic array push(task):

然后，添加任务的形式是一个经典的数组push(task)。

```
    q.array[q.top] = task; 
    ++q.top;
```

(The actual code needs to null-check and size-check the array, properly fence the accesses, and possibly signal waiting workers to start scanning -- see below.)  Both a successful pop and poll mainly entail a CAS of a slot from non-null to null.

(实际的代码需要对数组进行空值检查和大小检查，对访问进行适当的屏障，并可能向等待的工作者发出信号以开始扫描--见下文）。 一个成功的 pop 和 poll 都需要将一个槽从非空变成空。

The pop operation (always performed by owner) is:

```
    if ((base != top) and (the task at top slot is not null) and (CAS slot to null))
        decrement top and return task;
```

And the poll operation (usually by a stealer) is
```
    if ((base != top) and (the task at base slot is not null) 
            and (base has not changed) and (CAS slot to null))
        increment base and return task;
```

Because we rely on CASes of references, we do not need tag bits on base or top.  They are simple ints as used in any circular array-based queue (see for example ArrayDeque).  Updates to the indices guarantee that top == base means the queue is empty, but otherwise may err on the side of possibly making the queue appear nonempty when a push, pop, or poll have not fully committed. (Method isEmpty() checks the case of a partially completed removal of the last element.)  Because of this, the poll operation, considered individually, is not wait-free. One thief cannot successfully continue until another in-progress one (or, if previously empty, a push) completes.  However, in the aggregate, we ensure at least probabilistic non-blockingness.  If an attempted steal fails, a thief always chooses a different random victim target to try next. So, in order for one thief to progress, it suffices for any in-progress poll or new push on any empty queue to complete. (This is why we normally use method pollAt and its variants that try once at the apparent base index, else consider alternative actions, rather than method poll, which retries.)

因为我们依赖于引用的 CAS，所以我们不需要 base 或 top 的标签位。 它们是任何基于循环数组的队列中使用的简单的 ints（例如，见 ArrayDeque）。 对索引的更新保证 top == base 意味着队列是空的，但在推送、弹出或轮询没有完全提交时，可能会在使队列看起来不空的方面犯错误。(方法 isEmpty() 检查部分完成的最后一个元素的移除情况）。 由于这个原因，单独考虑的轮询操作不是无等待的。在另一个正在进行的轮询（或者，如果之前是空的，则是推送）完成之前，一个轮询不能成功继续。 然而，在总体上，我们至少保证了概率上的非阻塞性。 如果尝试偷窃失败，小偷总是选择一个不同的随机受害者目标来尝试下一个。因此，为了让一个盗贼取得进展，任何正在进行的轮询或任何空队列上的新推送都足以完成。(这就是为什么我们通常使用 pollAt 方法和它的变体，在明显的基础索引上尝试一次，然后考虑其他的行动，而不是使用重试的 poll 方法）。

This approach also enables support of a user mode in which local task processing is in FIFO, not LIFO order, simply by using poll rather than pop.  This can be useful in message-passing frameworks in which tasks are never joined. However neither mode considers affinities, loads, cache localities, etc, so rarely provide the best possible performance on a given machine, but portably provide good throughput by averaging over these factors.  Further, even if we did try to use such information, we do not usually have a basis for exploiting it.  For example, some sets of tasks profit from cache affinities, but others are harmed by cache pollution effects. Additionally, even though it requires scanning, long-term throughput is often best using random selection rather than directed selection policies, so cheap randomization of sufficient quality is used whenever applicable.  Various Marsaglia XorShifts (some with different shift constants) are inlined at use points.

这种方法还可以支持用户模式，在这种模式下，本地任务的处理是按先进先出的顺序进行的，而不是按后进先出的顺序，只需使用 poll 而不是 pop。 这在消息传递框架中是很有用的，在这种框架中，任务是不会被加入的。然而，这两种模式都没有考虑亲缘关系、负载、缓存的局部性等，所以很少在给定的机器上提供最好的性能，但通过对这些因素的平均化，可提供良好的吞吐量。 此外，即使我们试图使用这些信息，我们通常也没有利用这些信息的基础。 例如，一些任务集从缓存的亲和性中获利，但另一些则因缓存污染效应而受到损害。此外，即使需要扫描，长期的吞吐量往往是使用随机选择而不是定向选择策略的最佳结果，所以只要适用，就会使用足够质量的廉价随机化。 各种 Marsaglia XorShifts（有些具有不同的移位常数）在使用点被内联。


WorkQueues are also used in a similar way for tasks submitted to the pool. We cannot mix these tasks in the same queues used by workers. Instead, we randomly associate submission queues with submitting threads, using a form of hashing.  The ThreadLocalRandom probe value serves as a hash code for choosing existing queues, and may be randomly repositioned upon contention with other submitters.  In essence, submitters act like workers except that they are restricted to executing local tasks that they submitted (or in the case of CountedCompleters, others with the same root task). Insertion of tasks in shared mode requires a lock (mainly to protect in the case of resizing) but we use only a simple spinlock (using field qlock), because submitters encountering a busy queue move on to try or create other queues -- they block only when creating and registering new queues. Additionally, "qlock" saturates to an unlockable value (-1) at shutdown. Unlocking still can be and is performed by cheaper ordered writes of "qlock" in successful cases, but uses CAS in unsuccessful cases.

WorkQueues 也以类似的方式用于提交给池的任务。我们不能将这些任务混在工人使用的相同队列中。相反，我们使用一种散列形式，将提交队列与提交线程随机地联系起来。 ThreadLocalRandom 探测值作为选择现有队列的散列代码，在与其他提交者争夺时可能会被随机重新定位。 从本质上讲，提交者的行为就像工人，只是他们被限制在执行他们提交的本地任务（或者在 CountedCompleters 的情况下，其他具有相同根任务的人）。在共享模式下插入任务需要一个锁（主要是为了在调整大小的情况下进行保护），但我们只使用一个简单的自旋锁（使用字段qlock），因为提交者遇到一个繁忙的队列就会继续尝试或创建其他队列--他们只在创建和注册新队列时阻塞。此外，"qlock "在关机时饱和为一个可解锁的值（-1）。在成功的情况下，解锁仍然可以由 "qlock "的更便宜的有序写入来进行，但在不成功的情况下，则使用 CAS。

#### Management
The main throughput advantages of work-stealing stem from decentralized control -- workers mostly take tasks from themselves or each other, at rates that can exceed a billion per second.  The pool itself creates, activates (enables scanning for and running tasks), deactivates, blocks, and terminates threads, all with minimal central information. There are only a few properties that we can globally track or maintain, so we pack them into a small number of variables, often maintaining atomicity without blocking or locking. Nearly all essentially atomic control state is held in two volatile variables that are by far most often read (not written) as status and consistency checks. (Also, field "config" holds unchanging configuration state.)

工作窃取的主要吞吐量优势来自于分散的控制--工人大多从自己或对方那里拿任务，速度可以超过每秒 10 亿。 池子本身创建、激活（启用扫描和运行任务）、停用、阻塞和终止线程，所有这些都只需要最小的中央信息。只有少数属性是我们可以全局跟踪或维护的，所以我们把它们打包到少量的变量中，通常在没有阻塞或锁定的情况下保持原子性。几乎所有本质上的原子控制状态都保存在两个 volatile 变量中，到目前为止，这两个变量最常被读取（而不是写入）作为状态和一致性检查。(另外，字段 "config "持有不变的配置状态）。

Field "ctl" contains 64 bits holding information needed to atomically decide to add, inactivate, enqueue (on an event queue), dequeue, and/or re-activate workers.  To enable this packing, we restrict maximum parallelism to (1<<15)-1 (which is far in excess of normal operating range) to allow ids, counts, and their negations (used for thresholding) to fit into 16bit subfields.

字段 "ctl "包含 64 位的信息，用于原子化地决定添加、不激活、排队（在事件队列上）、取消排队和/或重新激活工作者。 为了实现这种打包，我们将最大的并行性限制在（1 << 15）-1（这远远超过了正常的操作范围），以允许 id、count 和它们的否定（用于阈值处理）适合 16 位子字段。

Field "runState" holds lockable state bits (STARTED, STOP, etc) also protecting updates to the workQueues array.  When used as a lock, it is normally held only for a few instructions (the only exceptions are one-time array initialization and uncommon resizing), so is nearly always available after at most a brief spin. But to be extra-cautious, after spinning, method awaitRunStateLock (called only if an initial CAS fails), uses a wait/notify mechanics on a builtin monitor to block when (rarely) needed. This would be a terrible idea for a highly contended lock, but most pools run without the lock ever contending after the spin limit, so this works fine as a more conservative alternative. Because we don't otherwise have an internal Object to use as a monitor, the "stealCounter" (an AtomicLong) is used when available (it too must be lazily initialized; see externalSubmit).

字段 "runState "持有可锁定的状态位（STARTED、STOP等），也保护 workQueues 数组的更新。 当作为一个锁使用时，它通常只保留几个指令（唯一的例外是一次性的数组初始化和不常见的大小调整），所以几乎总是在最多短暂的旋转后可用。但为了格外谨慎，在旋转之后，方法 awaitRunStateLock（仅在初始 CAS 失败时才调用），在一个内置的监视器上使用等待/通知机制，在（很少）需要时进行阻塞。对于一个高度竞争的锁来说，这将是一个糟糕的主意，但大多数池子在旋转限制后运行时都没有锁的竞争，所以这作为一个更保守的替代方案，效果很好。因为我们没有一个内部对象作为监控器，"steaveCounter"（一个AtomicLong）在可用时被使用（它也必须被懒散地初始化；见 externalSubmit）。

Usages of "runState" vs "ctl" interact in only one case: deciding to add a worker thread (see tryAddWorker), in which case the ctl CAS is performed while the lock is held.

runState "与 "ctl "的使用只在一种情况下相互影响：决定添加一个工作线程（见tryAddWorker），在这种情况下，ctl CAS是在锁被持有时执行的。

Recording WorkQueues.  WorkQueues are recorded in the "workQueues" array. The array is created upon first use (see externalSubmit) and expanded if necessary.  Updates to the array while recording new workers and unrecording terminated ones are protected from each other by the runState lock, but the array is otherwise concurrently readable, and accessed directly. We also ensure that reads of the array reference itself never become too stale. To simplify index-based operations, the array size is always a power of two, and all readers must tolerate null slots. Worker queues are at odd indices. Shared (submission) queues are at even indices, up to a maximum of 64 slots, to limit growth even if array needs to expand to add more workers. Grouping them together in this way simplifies and speeds up task scanning.

记录工作线索。 工作线索被记录在 "workQueues "数组中。该数组在第一次使用时被创建（见externalSubmit），并在必要时被扩展。 在记录新的工作者和取消记录已终止的工作者时，对数组的更新受到runState 锁的保护，但除此之外，数组是可同时读取的，并可直接访问。我们还确保对数组引用本身的读取不会变得过于陈旧。为了简化基于索引的操作，数组的大小总是二的幂，而且所有的读取器必须容忍空槽。工作者队列处于奇数索引。共享（提交）队列位于偶数索引，最多有 64 个槽，以限制增长，即使阵列需要扩展以增加更多的工人。以这种方式将它们分组，可以简化和加快任务扫描的速度。

All worker thread creation is on-demand, triggered by task submissions, replacement of terminated workers, and/or compensation for blocked workers. However, all other support code is set up to work with other policies.  To ensure that we do not hold on to worker references that would prevent GC, All accesses to workQueues are via indices into the workQueues array (which is one source of some of the messy code constructions here). In essence, the workQueues array serves as a weak reference mechanism. Thus for example the stack top subfield of ctl stores indices, not references.

所有工人线程的创建都是按需进行的，由任务提交、替换被终止的工人和/或补偿被阻塞的工人来触发。然而，所有其他支持代码都被设置为与其他策略一起工作。 为了确保我们不保留会妨碍 GC 的工作者引用，所有对workQueues 的访问都是通过 workQueues 数组的索引进行的（这也是这里一些混乱的代码结构的来源之一）。从本质上讲，workQueues 数组是一种弱引用机制。因此，例如，ctl 的栈顶子字段存储索引，而不是引用。

Queuing Idle Workers. Unlike HPC work-stealing frameworks, we cannot let workers spin indefinitely scanning for tasks when none can be found immediately, and we cannot start/resume workers unless there appear to be tasks available.  On the other hand, we must quickly prod them into action when new tasks are submitted or generated. In many usages, ramp-up time to activate workers is the main limiting factor in overall performance, which is compounded at program start-up by JIT compilation and allocation. So we streamline this as much as possible.

排列闲置的工作者。与 HPC 工作窃取框架不同，我们不能让工人无限期地旋转扫描任务，如果不能立即找到任务，我们也不能启动/恢复工人，除非有任务可用。 另一方面，当有新的任务提交或产生时，我们必须迅速促使他们开始行动。在许多应用中，激活工人的时间是限制整体性能的主要因素，在程序启动时，JIT 编译和分配会使情况更加复杂。因此，我们尽可能地简化这一过程。

The "ctl" field atomically maintains active and total worker counts as well as a queue to place waiting threads so they can be located for signalling. Active counts also play the role of quiescence indicators, so are decremented when workers believe that there are no more tasks to execute. The "queue" is actually a form of Treiber stack.  A stack is ideal for activating threads in most-recently used order. This improves performance and locality, outweighing the disadvantages of being prone to contention and inability to release a worker unless it is topmost on stack.  We park/unpark workers after pushing on the idle worker stack (represented by the lower 32bit subfield of ctl) when they cannot find work.  The top stack state holds the value of the "scanState" field of the worker: its index and status, plus a version counter that, in addition to the count subfields (also serving as version stamps) provide protection against Treiber stack ABA effects.

“ctl" 字段以原子方式维护活跃的和总的工作者计数，以及一个用于放置等待线程的队列，以便它们可以被定位以发出信号。活跃计数也起到了静止指标的作用，所以当工作者认为没有更多的任务要执行时，就会被减去。队列 "实际上是 Treiber 栈的一种形式。 栈是按照最近使用的顺序激活线程的理想选择。这提高了性能和定位性，超过了容易发生争执和无法释放工作者的缺点，除非它在堆栈中处于最顶端。 当工人找不到工作时，我们在推入空闲工人栈（由 ctl 的低 32 位子字段表示）后停放/取消停放。 栈顶状态持有工作器的 "scanState "字段的值：它的索引和状态，加上一个版本计数器，除了计数子字段（也作为版本标记）外，还提供对 Treiber 栈 ABA 效应的保护。

Field scanState is used by both workers and the pool to manage and track whether a worker is INACTIVE (possibly blocked waiting for a signal), or SCANNING for tasks (when neither hold it is busy running tasks).  When a worker is inactivated, its scanState field is set, and is prevented from executing tasks, even though it must scan once for them to avoid queuing races. Note that scanState updates lag queue CAS releases so usage requires care. When queued, the lower 16 bits of scanState must hold its pool index. So we place the index there upon initialization (see registerWorker) and otherwise keep it there or restore it when necessary.

字段 scanState 被worker 和 pool 用来管理和跟踪 worker 是否处于不活动状态（可能被阻断了，在等待信号），或正在扫描任务（当两者都不保持时，它正忙于运行任务）。 当一个工作者不活跃时，它的 scanState 字段被设置，并被阻止执行任务，即使它必须为它们扫描一次以避免排队竞赛。请注意，scanState的更新滞后于队列 CAS 的释放，所以使用时需要注意。当排队时，scanState 的低 16 位必须保持其池索引。所以我们在初始化时把索引放在那里（见 registerWorker），否则就把它放在那里或在必要时恢复它。

Memory ordering.  See "Correct and Efficient Work-Stealing for Weak Memory Models" by Le, Pop, Cohen, and Nardelli, PPoPP 2013 (http://www.di.ens.fr/~zappa/readings/ppopp13.pdf) for an analysis of memory ordering requirements in work-stealing algorithms similar to the one used here.  We usually need stronger than minimal ordering because we must sometimes signal workers, requiring Dekker-like full-fences to avoid lost signals.  Arranging for enough ordering without expensive over-fencing requires tradeoffs among the supported means of expressing access constraints. The most central operations, taking from queues and updating ctl state, require full-fence CAS.  Array slots are read using the emulation of volatiles provided by Unsafe.  Access from other threads to WorkQueue base, top, and array requires a volatile load of the first of any of these read.  We use the convention of declaring the "base" index volatile, and always read it before other fields. The owner thread must ensure ordered updates, so writes use ordered intrinsics unless they can piggyback on those for other writes.  Similar conventions and rationales hold for other WorkQueue fields (such as "currentSteal") that are only written by owners but observed by others.

内存排序。 参见 Le, Pop, Cohen, and Nardelli的 "Correct and Efficient Work-Stealing for Weak Memory Models", PPoPP 2013 (http://www.di.ens.fr/~zappa/readings/ppopp13.pdf)，以了解对类似于这里使用的工作窃取算法的内存排序要求的分析。 我们通常需要比最小排序更强的排序，因为我们有时必须向工人发出信号，需要类似 Dekker 的全面屏障来避免丢失信号。 安排足够的排序而不需要昂贵的过度围栏，需要在所支持的表达访问约束的手段中进行权衡。最核心的操作，即从队列中提取和更新 ctl 状态，需要全屏障 CAS。 数组槽的读取是通过 Unsafe 提供的模拟 volatile 进行的。 其他线程对 WorkQueen base、top和array的访问需要对这些读数中的第一个进行 volatile load。 我们使用的惯例是将 "基数 "索引声明为 volatile，并且总是在其他字段之前读取它。主人线程必须确保有序的更新，所以写的时候要使用有序的本征，除非他们可以在其他写的时候捎带上这些本征。 类似的惯例和理由也适用于其他 WorkQueue 字段（如 "currentSteal"），这些字段只由所有者写入，但其他人可以观察到。

Creating workers. To create a worker, we pre-increment total count (serving as a reservation), and attempt to construct a ForkJoinWorkerThread via its factory. Upon construction, the new thread invokes registerWorker, where it constructs a WorkQueue and is assigned an index in the workQueues array (expanding the array if necessary). The thread is then started. Upon any exception across these steps, or null return from factory, deregisterWorker adjusts counts and records accordingly.  If a null return, the pool continues running with fewer than the target number workers. If exceptional, the exception is propagated, generally to some external caller. Worker index assignment avoids the bias in scanning that would occur if entries were sequentially packed starting at the front of the workQueues array. We treat the array as a simple power-of-two hash table, expanding as needed. The seedIndex increment ensures no collisions until a resize is needed or a worker is deregistered and replaced, and thereafter keeps probability of collision low. We cannot use ThreadLocalRandom.getProbe() for similar purposes here because the thread has not started yet, but do so for creating submission queues for existing external threads.

创建工作者。为了创建一个工作者，我们预先增加总计数（作为保留），并试图通过其工厂构建一个ForkJoinWorkerThread。建成后，新线程调用 registerWorker，在那里它构建了一个 WorkQueue，并在workQueues 数组中被分配了一个索引（如有必要，可扩展该数组）。然后，该线程被启动。在这些步骤中出现任何异常，或者从工厂返回空值时，deregisterWorker 会相应地调整计数和记录。 如果是空返回，池子继续运行，工人数量少于目标数量。如果是特殊情况，异常就会被传播，通常是传播给一些外部调用者。工作者索引分配避免了扫描中的偏差，如果条目按顺序从 workQueues 数组的前面开始打包，就会出现这种偏差。我们将数组视为一个简单的二乘法哈希表，根据需要进行扩展。seedIndex 的增量确保了在需要调整大小或取消注册并替换工作者之前不会发生碰撞，此后则保持较低的碰撞概率。我们不能在这里使用 ThreadLocalRandom.getProbe() 来达到类似的目的，因为线程还没有开始，但在为现有的外部线程创建提交队列时，我们会这样做。

Deactivation and waiting. Queuing encounters several intrinsic races; most notably that a task-producing thread can miss seeing (and signalling) another thread that gave up looking for work but has not yet entered the wait queue.  When a worker cannot find a task to steal, it deactivates and enqueues. Very often, the lack of tasks is transient due to GC or OS scheduling. To reduce false-alarm deactivation, scanners compute checksums of queue states during sweeps.  (The stability checks used here and elsewhere are probabilistic variants of snapshot techniques -- see Herlihy & Shavit.) Workers give up and try to deactivate only after the sum is stable across scans. Further, to avoid missed signals, they repeat this scanning process after successful enqueuing until again stable.  In this state, the worker cannot take/run a task it sees until it is released from the queue, so the worker itself eventually tries to release itself or any successor (see tryRelease).  Otherwise, upon an empty scan, a deactivated worker uses an adaptive local spin construction (see awaitWork) before blocking (via park). Note the unusual conventions about Thread.interrupts surrounding parking and other blocking: Because interrupts are used solely to alert threads to check termination, which is checked anyway upon blocking, we clear status (using Thread.interrupted) before any call to park, so that park does not immediately return due to status being set via some other unrelated call to interrupt in user code.

停用和等待。排队遇到了几个内在的竞赛；最明显的是，一个产生任务的线程可能会错过看到（并发出信号）另一个放弃寻找工作但尚未进入等待队列的线程。 当一个工作者找不到要偷的任务时，它就会停用并排队。很多时候，由于 GC 或操作系统的调度，缺乏任务是暂时性的。为了减少误报停用，扫描器在扫描期间计算队列状态的校验和。 (这里和其他地方使用的稳定性检查是快照技术的概率变体--见 Herlihy & Shavit）。工作者只有在整个扫描过程中总和稳定后才会放弃并尝试停用。此外，为了避免错过信号，他们在成功排队后重复这个扫描过程，直到再次稳定。 在这种状态下，工作器在从队列中释放之前不能采取/运行它所看到的任务，所以工作器本身最终会尝试释放自己或任何继任者（见 tryRelease）。 否则，在空扫描时，停用的工作器在阻塞（通过park）之前使用自适应的本地自旋构造（见 awaitWork）。请注意关于 Thread.interrupts 围绕停放和其他阻塞的不寻常的约定。因为中断只用于提醒线程检查终止，而终止在阻塞时也会被检查，所以我们在调用park之前清除状态（使用Thread.interrupted），这样 park 就不会因为用户代码中通过其他不相关的中断调用设置状态而立即返回。

Signalling and activation.  Workers are created or activated only when there appears to be at least one task they might be able to find and execute.  Upon push (either by a worker or an external submission) to a previously (possibly) empty queue， workers are signalled if idle, or created if fewer exist than the given parallelism level.  These primary signals are buttressed by others whenever other threads remove a task from a queue and notice that there are other tasks there as well. On most platforms, signalling (unpark) overhead time is noticeably long, and the time between signalling a thread and it actually making progress can be very noticeably long, so it is worth offloading these delays from critical paths as much as possible. Also, because inactive workers are often rescanning or spinning rather than blocking, we set and clear the "parker" field of WorkQueues to reduce unnecessary calls to unpark. (This requires a secondary recheck to avoid missed signals.)

信号和激活。 只有当看起来至少有一个任务可以找到并执行时，才会创建或激活工作者。 在向先前（可能）为空的队列推送任务时（由工作者或外部提交），如果工作者处于空闲状态，就会发出信号；如果存在的工作者少于给定的并行水平，就会创建。 当其他线程从队列中移除任务并注意到那里还有其他任务时，这些主要信号会得到其他信号的支持。在大多数平台上，发出信号（unpark）的开销时间明显较长，而且从发出信号到线程真正取得进展的时间也会非常明显，所以值得尽可能地从关键路径上卸载这些延迟。此外，由于不活动的工作者通常是在重新扫描或旋转，而不是阻塞，我们设置和清除 WorkQueues 的 "parker "字段，以减少不必要的解停调用。(这需要二次重新检查以避免错过信号）。

Trimming workers. To release resources after periods of lack of use, a worker starting to wait when the pool is quiescent will time out and terminate (see awaitWork) if the pool has remained quiescent for period IDLE_TIMEOUT, increasing the period as the number of threads decreases, eventually removing all workers. Also, when more than two spare threads exist, excess threads are immediately terminated at the next quiescent point. (Padding by two avoids hysteresis.)

修剪工作者。为了在缺乏使用的时期后释放资源，在池子静止时开始等待的工作者将超时并终止（见 awaitWork），如果池子在 IDLE_TIMEOUT 期间保持静止，随着线程数量的减少，周期会增加，最终删除所有工作者。另外，当存在两个以上的空闲线程时，多余的线程会在下一个静止点立即被终止。(填充两个可以避免滞后）。

Shutdown and Termination. A call to shutdownNow invokes tryTerminate to atomically set a runState bit. The calling thread, as well as every other worker thereafter terminating, helps terminate others by setting their (qlock) status, cancelling their unprocessed tasks, and waking them up, doing so repeatedly until stable (but with a loop bounded by the number of workers).  Calls to non-abrupt shutdown() preface this by checking whether termination should commence. This relies primarily on the active count bits of "ctl" maintaining consensus -- tryTerminate is called from awaitWork whenever quiescent. However, external submitters do not take part in this consensus.  So, tryTerminate sweeps through queues (until stable) to ensure lack of in-flight submissions and workers about to process them before triggering the "STOP" phase of termination. (Note: there is an intrinsic conflict if helpQuiescePool is called when shutdown is enabled. Both wait for quiescence, but tryTerminate is biased to not trigger until helpQuiescePool completes.)

关机和终止。对 shutdownNow 的调用会调用 tryTerminate 来原子化地设置一个运行状态位。调用线程，以及此后终止的其他每个工作者，通过设置他们的（qlock）状态，取消他们未处理的任务，并唤醒他们，反复这样做，直到稳定为止（但循环次数以工作者的数量为限），来帮助终止其他人。 对非中断的 shutdown() 的调用通过检查是否应该开始终止来进行。这主要依靠 "ctl "的活动计数位来维持共识--每当静止时，就从 awaitWork 中调用 tryTerminate。然而，外部提交者并不参与这一共识。 因此，tryTerminate 在触发终止的 "STOP "阶段之前，会对队列进行扫描（直到稳定），以确保没有飞行中的提交和即将处理它们的工作者。(注意：如果在启用关闭时调用 helpQuiescePool，会有一个内在的冲突。两者都在等待静止，但 tryTerminate 偏向于在helpQuiescePool 完成之前不触发）。

#### Joining Tasks
Any of several actions may be taken when one worker is waiting to join a task stolen (or always held) by another.  Because we are multiplexing many tasks on to a pool of workers, we can't just let them block (as in Thread.join).  We also cannot just reassign the joiner's run-time stack with another and replace it later, which would be a form of "continuation", that even if possible is not necessarily a good idea since we may need both an unblocked task and its continuation to progress.  Instead we combine two tactics:

当一个工作者等待加入被另一个工作者窃取（或一直持有）的任务时，可以采取几种行动中的任何一种。 因为我们正在将许多任务复用到一个工作池中，所以我们不能只是让它们阻塞（如 Thread.join）。 我们也不能只是把加入者的运行时堆栈重新分配给另一个，然后再把它替换掉，这将是一种 "延续 "的形式，即使有可能，也不一定是个好主意，因为我们可能需要一个无阻塞的任务和它的延续来进行。 相反，我们结合两种策略。

Helping: Arranging for the joiner to execute some task that it would be running if the steal had not occurred.

帮助。安排加入者执行一些任务，如果偷窃没有发生，它就会运行这些任务。

Compensating: Unless there are already enough live threads, method tryCompensate() may create or re-activate a spare thread to compensate for blocked joiners until they unblock.

补偿。除非已经有足够的活线程，否则 tryCompensate() 方法可能会创建或重新激活一个备用线程来补偿被阻塞的加入者，直到他们解除阻塞。

A third form (implemented in tryRemoveAndExec) amounts to helping a hypothetical compensator: If we can readily tell that a possible action of a compensator is to steal and execute the task being joined, the joining thread can do so directly, without the need for a compensation thread (although at the expense of larger run-time stacks, but the tradeoff is typically worthwhile).

第三种形式（在 tryRemoveAndExec 中实现）相当于帮助一个假想的补偿器。如果我们可以很容易地知道补偿器的一个可能的动作是窃取和执行被加入的任务，加入的线程可以直接这样做，而不需要补偿线程（尽管代价是更大的运行时堆栈，但这种折衷通常是值得的）。

The ManagedBlocker extension API can't use helping so relies only on compensation in method awaitBlocker.

ManagedBlocker 扩展 API 不能使用帮助，所以只依赖于方法 awaitBlocker 中的补偿。

The algorithm in helpStealer entails a form of "linear helping".  Each worker records (in field currentSteal) the most recent task it stole from some other worker (or a submission). It also records (in field currentJoin) the task it is currently actively joining. Method helpStealer uses these markers to try to find a worker to help (i.e., steal back a task from and execute it) that could hasten completion of the actively joined task.  Thus, the joiner executes a task that would be on its own local deque had the to-be-joined task not been stolen. This is a conservative variant of the approach described in Wagner & Calder "Leapfrogging: a portable technique for implementing efficient futures" SIGPLAN Notices, 1993 (http://portal.acm.org/citation.cfm?id=155354). It differs in that: (1) We only maintain dependency links across workers upon steals, rather than use per-task bookkeeping.  This sometimes requires a linear scan of workQueues array to locate stealers, but often doesn't because stealers leave hints (that may become stale/wrong) of where to locate them.  It is only a hint because a worker might have had multiple steals and the hint records only one of them (usually the most current).  Hinting isolates cost to when it is needed, rather than adding to per-task overhead.  (2) It is "shallow", ignoring nesting and potentially cyclic mutual steals.  (3) It is intentionally racy: field currentJoin is updated only while actively joining, which means that we miss links in the chain during long-lived tasks, GC stalls etc (which is OK since blocking in such cases is usually a good idea).  (4) We bound the number of attempts to find work using checksums and fall back to suspending the worker and if necessary replacing it with another.

helpStealer 的算法需要一种 "线性帮助 "的形式。 每个工人记录（在字段 currentSteal 中）它从其他工人（或提交）那里偷来的最近的任务。它还记录（在字段 currentJoin 中）它目前正在积极加入的任务。方法helpStealer使用这些标记，试图找到一个可以加速完成主动加入的任务的工作者来提供帮助（即，从那里偷回一个任务并执行它）。 因此，加入者执行一个任务，如果要加入的任务没有被窃取，这个任务就会在它自己的本地德克上。这是 Wagner & Calder "Leapfrogging: a portable technique for implementing efficient futures" SIGPLAN Notices, 1993 (http://portal.acm.org/citation.cfm?id=155354) 中描述的方法的一个保守的变体。它的不同之处在于。(1)我们只在偷窃时维护工作间的依赖联系，而不是使用每个任务的记账。 这有时需要对workQueues数组进行线性扫描来定位偷窃者，但通常不需要，因为偷窃者会留下提示（可能会变得陈旧/错误）来定位他们的位置。 这只是一个提示，因为一个工人可能有多次偷窃，而提示只记录了其中一个（通常是最新的）。 暗示将成本隔离到需要的时候，而不是增加每个任务的开销。 (2) 它是 "浅层 "的，忽略了嵌套和潜在的周期性相互窃取。 (3) 它是故意的：字段currentJoin只在主动加入时更新，这意味着我们会在长寿命任务、GC 停滞等情况下错过链中的环节（这没关系，因为在这种情况下阻塞通常是个好主意）。 (4) 我们使用校验和来约束寻找工作的尝试次数，并退回到暂停工作，如果有必要的话，用另一个工作代替它。

Helping actions for CountedCompleters do not require tracking currentJoins: Method helpComplete takes and executes any task with the same root as the task being waited on (preferring local pops to non-local polls). However, this still entails some traversal of completer chains, so is less efficient than using CountedCompleters without explicit joins.

对 CountedCompleters 的帮助行动不需要跟踪 currentJoins。方法 helpComplete 接收并执行任何与被等待的任务同根的任务（更倾向于本地弹出而不是非本地投票）。然而，这仍然需要对完成者链进行一些遍历，所以效率不如使用没有明确连接的 CountedCompleters。

Compensation does not aim to keep exactly the target parallelism number of unblocked threads running at any given time. Some previous versions of this class employed immediate compensations for any blocked join. However, in practice, the vast majority of blockages are transient byproducts of GC and other JVM or OS activities that are made worse by replacement. Currently, compensation is attempted only after validating that all purportedly active threads are processing tasks by checking field WorkQueue.scanState, which eliminates most false positives.  Also, compensation is bypassed (tolerating fewer threads) in the most common case in which it is rarely beneficial: when a worker with an empty queue (thus no continuation tasks) blocks on a join and there still remain enough threads to ensure liveness.

补偿的目的不是为了在任何时候都准确地保持无阻塞线程的目标并行数。这个类以前的一些版本对任何阻塞的连接采用了即时补偿。然而，在实践中，绝大多数阻塞是 GC 和其他 JVM 或 OS 活动的瞬时副产品，通过替换而变得更糟。目前，只有在通过检查字段 WorkQueue.scanState 来验证所有所谓的活动线程都在处理任务之后，才会尝试补偿，这就消除了大多数误报。 此外，在最常见的情况下，补偿会被绕过（容忍更少的线程），在这种情况下，补偿很少有好处：当一个工人的队列是空的（因此没有继续任务），在一个连接上阻塞，仍然有足够的线程来确保有效性。

The compensation mechanism may be bounded.  Bounds for the commonPool (see commonMaxSpares) better enable JVMs to cope with programming errors and abuse before running out of resources to do so. In other cases, users may supply factories that limit thread construction. The effects of bounding in this pool (like all others) is imprecise.  Total worker counts are decremented when threads deregister, not when they exit and resources are reclaimed by the JVM and OS. So the number of simultaneously live threads may transiently exceed bounds.

补偿机制可能是有界限的。 commonPool 的界限（见commonMaxSpares）可以更好地使 JVM 在耗尽资源之前应对编程错误和滥用。在其他情况下，用户可以提供限制线程构建的工厂。在这个池子里的约束效果（像所有其他池子一样）是不精确的。 当线程取消注册时，工作者的总计数会被递减，而不是当他们退出和资源被 JVM 和 OS 回收时。因此，同时运行的线程数量可能会暂时性地超过界限。

#### Common Pool
The static common pool always exists after static initialization.  Since it (or any other created pool) need never be used, we minimize initial construction overhead and footprint to the setup of about a dozen fields, with no nested allocation. Most bootstrapping occurs within method externalSubmit during the first submission to the pool.

静态公共池在静态初始化后始终存在。 由于它（或任何其他创建的池）永远不需要被使用，我们将初始构建的开销和足迹降到最低，只设置十几个字段，没有嵌套分配。大多数引导发生在方法externalSubmit中，在第一次提交给池子的时候。

When external threads submit to the common pool, they can perform subtask processing (see externalHelpComplete and related methods) upon joins.  This caller-helps policy makes it sensible to set common pool parallelism level to one (or more) less than the total number of available cores, or even zero for pure caller-runs.  We do not need to record whether external submissions are to the common pool -- if not, external help methods return quickly. These submitters would otherwise be blocked waiting for completion, so the extra effort (with liberally sprinkled task status checks) in inapplicable cases amounts to an odd form of limited spin-wait before blocking in ForkJoinTask.join.

当外部线程提交到公共池时，它们可以在加入时执行子任务处理（见 externalHelpComplete 和相关方法）。 这种调用者帮助政策使得将公共池的并行性水平设置为比可用内核总数少一个（或更多），或者对于纯调用者运行来说甚至是零是明智的。 我们不需要记录外部提交是否到了公共池中--如果不是，外部帮助方法会很快返回。否则这些提交者会被阻塞等待完成，所以在不适用的情况下，额外的努力（自由洒脱的任务状态检查）相当于ForkJoinTask.join 中阻塞前的一种奇怪的有限旋转等待形式。

As a more appropriate default in managed environments, unless overridden by system properties, we use workers of subclass InnocuousForkJoinWorkerThread when there is a SecurityManager present. These workers have no permissions set, do not belong to any user-defined ThreadGroup, and erase all ThreadLocals after executing any top-level task (see WorkQueue.runTask). The associated mechanics (mainly in ForkJoinWorkerThread) may be JVM-dependent and must access particular Thread class fields to achieve this effect.

作为管理环境中更合适的默认值，除非被系统属性覆盖，否则当有 SecurityManager 存在时，我们使用子类InnocuousForkJoinWorkerThread 的工作者。这些工作者没有设置任何权限，不属于任何用户定义的线程组，并且在执行任何顶层任务（见WorkQueue.runTask）后会清除所有的 ThreadLocals。相关的机制（主要是在ForkJoinWorkerThread中）可能是依赖于JVM的，并且必须访问特定的 Thread 类字段以实现这一效果。

#### Style notes
Memory ordering relies mainly on Unsafe intrinsics that carry the further responsibility of explicitly performing null- and bounds- checks otherwise carried out implicitly by JVMs.  This can be awkward and ugly, but also reflects the need to control outcomes across the unusual cases that arise in very racy code with very few invariants. So these explicit checks would exist in some form anyway.  All fields are read into locals before use, and null-checked if they are references.  This is usually done in a "C"-like style of listing declarations at the heads of methods or blocks, and using inline assignments on first encounter.  Array bounds-checks are usually performed by masking with array.length-1, which relies on the invariant that these arrays are created with positive lengths, which is itself paranoically checked. Nearly all explicit checks lead to bypass/return, not exception throws, because they may legitimately arise due to cancellation/revocation during shutdown.

内存排序主要依赖于不安全的内在因素，这些内在因素承担着明确执行空值和边界检查的进一步责任，否则JVM就会隐含地进行检查。 这可能很尴尬，也很难看，但也反映了控制结果的需要，这些不寻常的情况出现在具有非常少的不变因素的非常粗俗的代码中。所以这些显式检查无论如何都会以某种形式存在。 所有的字段在使用前都被读入locals，如果它们是引用的话，还要进行null检查。 这通常是以类似于 "C "的方式进行的，在方法或块的头部列出声明，并在第一次遇到时使用内联赋值。 数组的边界检查通常是通过使用array.length-1来进行的，这依赖于这些数组是以正数长度创建的这一不变性，而这一不变性本身就是被偏执地检查的。几乎所有的显式检查都会导致绕过/返回，而不是抛出异常，因为它们可能是由于关闭期间的取消/重设而合法产生的。

There is a lot of representation-level coupling among classes ForkJoinPool, ForkJoinWorkerThread, and ForkJoinTask. The fields of WorkQueue maintain data structures managed by ForkJoinPool, so are directly accessed.  There is little point trying to reduce this, since any associated future changes in representations will need to be accompanied by algorithmic changes anyway. Several methods intrinsically sprawl because they must accumulate sets of consistent reads of fields held in local variables.  There are also other coding oddities (including several unnecessary-looking hoisted null checks) that help some methods perform reasonably even when interpreted (not compiled).

在类 ForkJoinPool、ForkJoinWorkerThread 和 ForkJoinTask 之间有很多表示层面的耦合。WorkQueue 的字段维护着由 ForkJoinPool 管理的数据结构，所以被直接访问。试图减少这种情况是没有意义的，因为无论如何，未来任何相关的表示方法的变化都需要伴随着算法的变化。有几个方法本质上是蔓延的，因为它们必须积累在局部变量中持有的字段的一致读数集。 还有其他一些编码上的怪癖（包括几个看起来不必要的空值检查），帮助一些方法即使在解释（不是编译）时也能合理地执行。

The order of declarations in this file is (with a few exceptions):
(1) Static utility functions
(2) Nested (static) classes
(3) Static fields
(4) Fields, along with constants used when unpacking some of them
(5) Internal control methods
(6) Callbacks and other support for ForkJoinTask methods
(7) Exported methods
(8) Static block initializing statics in minimally dependent order

#### ForkJoinWorkerThread
A thread managed by a ForkJoinPool, which executes ForkJoinTasks. This class is subclassable solely for the sake of adding functionality -- there are no overridable methods dealing with scheduling or execution. However, you can override initialization and termination methods surrounding the main task processing loop. If you do create such a subclass, you will also need to supply a custom ForkJoinPool.ForkJoinWorkerThreadFactory to use it in a ForkJoinPool.

一个由 ForkJoinPool 管理的线程，它执行 ForkJoinTasks。这个类的可子类化仅仅是为了增加功能 -- 没有处理调度或执行的可重写方法。然而，你可以重写围绕主任务处理循环的初始化和终止方法。如果你真的创建了这样一个子类，你还需要提供一个自定义的 ForkJoinPool.ForkJoinWorkerThreadFactory 来在ForkJoinPool 中使用它。

ForkJoinWorkerThreads are managed by ForkJoinPools and perform ForkJoinTasks. For explanation, see the internal documentation of class ForkJoinPool.

ForkJoinWorkerThreads 由 ForkJoinPools 管理并执行 ForkJoinTasks。有关解释，请参见类ForkJoinPool 的内部文档。

This class just maintains links to its pool and WorkQueue. The pool field is set immediately upon construction, but the workQueue field is not set until a call to registerWorker completes. This leads to a visibility race, that is tolerated by requiring that the workQueue field is only accessed by the owning thread.

这个类只是维护与它的池和 WorkQueue 的链接。池字段在构造时立即被设置，但 workQueue 字段在调用 registerWorker 完成后才被设置。这导致了可见性竞争，通过要求 workQueue 字段只被拥有线程访问而被容忍。

Support for (non-public) subclass InnocuousForkJoinWorkerThread requires that we break quite a lot of encapsulation (via Unsafe) both here and in the subclass to access and set Thread fields.

支持（非公开的）子类 InnocuousForkJoinWorkerThread 需要我们在这里和子类中打破相当多的封装（通过 Unsafe）来访问和设置 Thread 字段。

##### Algorithm overview (Java 7)
1. Work-Stealing: Work-stealing queues are special forms of Deques that support only three of the four possible end-operations -- push, pop, and deq (aka steal), and only do so under the constraints that push and pop are called only from the owning thread, while deq may be called from other threads. (If you are unfamiliar with them, you probably want to read Herlihy and Shavit's book "The Art of Multiprocessor programming", chapter 16 describing these in more detail before proceeding.)  The main work-stealing queue design is roughly similar to "Dynamic Circular Work-Stealing Deque" by David Chase and Yossi Lev, SPAA 2005 (http://research.sun.com/scalable/pubs/index.html).  The main difference ultimately stems from gc requirements that we null out taken slots as soon as we can, to maintain as small a footprint as possible even in programs generating huge numbers of tasks. To accomplish this, we shift the CAS arbitrating pop vs deq (steal) from being on the indices ("base" and "sp") to the slots themselves (mainly via method "casSlotNull()"). So, both a successful pop and deq mainly entail CAS'ing a non-null slot to null.  Because we rely on CASes of references, we do not need tag bits on base or sp.  They are simple ints as used in any circular array-based queue (see for example ArrayDeque). Updates to the indices must still be ordered in a way that guarantees that (sp - base) > 0 means the queue is empty, but otherwise may err on the side of possibly making the queue appear nonempty when a push, pop, or deq have not fully committed. Note that this means that the deq operation, considered individually, is not wait-free. One thief cannot successfully continue until another in-progress one (or, if previously empty, a push) completes.  However, in the aggregate, we ensure at least probabilistic non-blockingness. If an attempted steal fails, a thief always chooses a different random victim target to try next. So, in order for one thief to progress, it suffices for any in-progress deq or new push on any empty queue to complete. One reason this works well here is that apparently-nonempty often means soon-to-be-stealable, which gives threads a chance to activate if necessary before stealing (see below).     

1. 工作窃取。工作窃取是 Deques 的特殊形式，它只支持四种可能的终端操作中的三种--push、pop 和 deq（又称窃取），并且只在 push 和 pop 只能从拥有线程中调用，而 deq 可以从其他线程中调用的限制下进行。(如果你对它们不熟悉，你可能想读读 Herlihy和Shavit的书 "The Art of Multiprocessor Programming"，在继续之前，第 16 章更详细地描述了这些内容）。 主要的工作窃取队列设计与 David Chase 和 Yossi Lev的 "Dynamic Circular Work-Stealing Deque "大致相似，SPAA 2005（http://research.sun.com/scalable/pubs/index.html）。 主要的区别最终来自于 GC 的要求，即我们要尽快清空所占用的槽，即使在产生大量任务的程序中，也要保持尽可能小的占用空间。为了达到这个目的，我们将 CAS 对 pop 和 deq（窃取）的仲裁从索引（"base"和 "sp"）上转移到槽本身（主要通过方法 "casSlotNull()"）。所以，一个成功的 pop 和 deq 主要需要将一个非空的插槽 CAS'ing 为空。 因为我们依赖于引用的 CAS，所以我们不需要 base 或 sp 上的标签位。它们是简单的 ints，就像在任何基于循环数组的队列中使用的那样（例如，见 ArrayDeque）。指数的更新仍然必须以保证(sp-base) > 0意味着队列是空的方式排序，但否则可能会在推送、弹出或删除未完全提交时使队列看起来不空。请注意，这意味着单独考虑的 deq 操作不是无等待的。在另一个正在进行的盗贼（或者，如果之前是空的，则是推送）完成之前，一个盗贼不能成功地继续。 然而，从总体上看，我们至少保证了概率上的非阻塞性。如果尝试偷窃失败，小偷总是选择一个不同的随机受害者目标来尝试下一个。因此，为了让一个盗贼取得进展，任何空队列上的任何正在进行的 deq 或新推送都足以完成。这在这里运作良好的一个原因是，表面上的非空往往意味着即将被偷，这让线程在偷窃前有机会在必要时激活（见下文）。    

This approach also enables support for "async mode" where local task processing is in FIFO, not LIFO order; simply by using a version of deq rather than pop when locallyFifo is true (as set by the ForkJoinPool).  This allows use in message-passing frameworks in which tasks are never joined.

这种方法还能够支持 "异步模式"，即本地任务处理是以先进先出而不是后进先出的顺序进行的；只需在localFifo 为真（由 ForkJoinPool 设置）时使用 deq 的版本而不是 pop。 这允许在消息传递框架中使用，在该框架中任务从未被加入。

Efficient implementation of this approach currently relies on an uncomfortable amount of "Unsafe" mechanics. To maintain correct orderings, reads and writes of variable base require volatile ordering.  Variable sp does not require volatile write but needs cheaper store-ordering on writes.  Because they are protected by volatile base reads, reads of the queue array and its slots do not need volatile load semantics, but writes (in push) require store order and CASes (in pop and deq) require (volatile) CAS semantics.  (See "Idempotent work stealing" by Michael, Saraswat, and Vechev, PPoPP 2009 http://portal.acm.org/citation.cfm?id=1504186 for an algorithm with similar properties, but without support for nulling slots.)  Since these combinations aren't supported using ordinary volatiles, the only way to accomplish these efficiently is to use direct Unsafe calls. (Using external AtomicIntegers and AtomicReferenceArrays for the indices and array is significantly slower because of memory locality and indirection effects.)

这种方法的有效实施目前依赖于令人不舒服的 "不安全 "机制。为了保持正确的排序，变量 base 的读写需要易失性排序。 变量 sp 不需要 volatile 写入，但在写入时需要更便宜的存储排序。 因为它们受到volatile base 读的保护，队列数组及其槽的读不需要 volatile load 语义，但是写（在 push 中）需要存储顺序，CAS（在 pop 和 deq 中）需要（volatile）CAS 语义。 (参见 Michael、Saraswat 和Vechev 的 "Idempotent work stealing"，PPoPP 2009 http://portal.acm.org/citation.cfm?id=1504186 ，了解具有类似属性的算法，但不支持空槽）。 由于这些组合不支持使用普通的 volatile，所以有效完成这些工作的唯一方法是使用直接的不安全调用。(使用外部的 AtomicIntegers 和AtomicReferenceArrays 作为索引和数组，由于内存定位和指示效应，速度会大大降低）。

Further, performance on most platforms is very sensitive to placement and sizing of the (resizable) queue array.  Even though these queues don't usually become all that big, the initial size must be large enough to counteract cache contention effects across multiple queues (especially in the presence of GC cardmarking). Also, to improve thread-locality, queues are currently initialized immediately after the thread gets the initial signal to start processing tasks.  However, all queue-related methods except pushTask are written in a way that allows them to instead be lazily allocated and/or disposed of when empty. All together, these low-level implementation choices produce as much as a factor of 4 performance improvement compared to naive implementations, and enable the processing of billions of tasks per second, sometimes at the expense of ugliness.

此外，大多数平台上的性能对（可调整大小的）队列阵列的位置和大小非常敏感。 尽管这些队列通常不会变得那么大，但初始大小必须足够大，以抵消多个队列之间的缓存竞争效应（尤其是在 GC 标记的情况下）。另外，为了提高线程的定位，目前在线程获得开始处理任务的初始信号后，立即初始化队列。 然而，除了pushTask 之外，所有与队列相关的方法的编写方式都允许它们在空时被懒散地分配和/或处置。总的来说，这些底层实现的选择与天真的实现相比，产生了多达 4 倍的性能改进，并且能够每秒处理数十亿个任务，有时是以丑陋为代价的。

2. Run control: The primary run control is based on a global counter (activeCount) held by the pool. It uses an algorithm similar to that in Herlihy and Shavit section 17.6 to cause threads to eventually block when all threads declare they are inactive. For this to work, threads must be declared active when executing tasks, and before stealing a task. They must be inactive before blocking on the Pool Barrier (awaiting a new submission or other Pool event). In between, there is some free play which we take advantage of to avoid contention and rapid flickering of the global activeCount: If inactive, we activate only if a victim queue appears to be nonempty (see above). Similarly, a thread tries to inactivate only after a full scan of other threads.  The net effect is that contention on activeCount is rarely a measurable performance issue. (There are also a few other cases where we scan for work rather than retry/block upon contention.)

2. 运行控制。主要的运行控制是基于一个由池子持有的全局计数器（activeCount）。它使用一种类似于Herlihy 和 Shavit 第17.6 节中的算法，当所有线程都宣布它们不活动时，就会导致线程最终阻塞。为了使其发挥作用，线程必须在执行任务时和窃取任务前被宣布为活跃。他们必须在池子障碍物上阻塞之前处于非活动状态（等待新的提交或其他池子事件）。在这两者之间，有一些自由发挥的机会，我们利用这些机会来避免争夺和全局 activeCount 的快速闪烁。如果不活动，我们只有在受害者队列出现非空时才会激活（见上文）。同样地，一个线程只有在对其他线程进行全面扫描后才会尝试不激活。 净效果是，对 activeCount的争夺很少是一个可衡量的性能问题。(还有一些其他情况，我们扫描工作，而不是在争用时重试/阻塞）。

3. Selection control. We maintain policy of always choosing to run local tasks rather than stealing, and always trying to steal tasks before trying to run a new submission. All steals are currently performed in randomly-chosen deq-order. It may be worthwhile to bias these with locality / anti-locality information, but doing this well probably requires more lower-level information from JVMs than currently provided.

3. 选择控制。我们的政策是总是选择运行本地任务而不是偷窃，并且总是在尝试运行新的提交之前尝试偷窃任务。目前所有的偷窃都是以随机选择的 deq-order 进行的。也许值得用局部性/反局部性信息来偏重这些任务，但要做好这一点可能需要JVM提供比目前更多的底层信息。

#### ForkJoinTask
Abstract base class for tasks that run within a ForkJoinPool. A ForkJoinTask is a thread-like entity that is much lighter weight than a normal thread. Huge numbers of tasks and subtasks may be hosted by a small number of actual threads in a ForkJoinPool, at the price of some usage limitations.

在 ForkJoinPool 中运行的任务的抽象基类。ForkJoinTask 是一个类似于线程的实体，它比普通线程的重量要轻得多。大量的任务和子任务可以由 ForkJoinPool 中少量的实际线程来托管，代价是一些使用限制。

A "main" ForkJoinTask begins execution when it is explicitly submitted to a ForkJoinPool, or, if not already engaged in a ForkJoin computation, commenced in the ForkJoinPool.commonPool() via fork, invoke, or related methods. Once started, it will usually in turn start other subtasks. As indicated by the name of this class, many programs using ForkJoinTask employ only methods fork and join, or derivatives such as invokeAll. However, this class also provides a number of other methods that can come into play in advanced usages, as well as extension mechanics that allow support of new forms of fork/join processing.

一个 "主" ForkJoinTask 在被明确提交给 ForkJoinPool 时开始执行，或者，如果还没有参与ForkJoin 计算，则通过 fork、invoke 或相关方法在 ForkJoinPool.commonPool() 中开始。一旦启动，它通常会依次启动其他子任务。正如这个类的名字所示，许多使用 ForkJoinTask 的程序只采用了 fork 和 join 方法，或诸如 invokeAll 这样的衍生物。然而，这个类还提供了一些其他的方法，这些方法在高级使用中可以发挥作用，还有一些扩展机制，允许支持新形式的 fork/join 处理。

A ForkJoinTask is a lightweight form of Future. The efficiency of ForkJoinTasks stems from a set of restrictions (that are only partially statically enforceable) reflecting their main use as computational tasks calculating pure functions or operating on purely isolated objects. The primary coordination mechanisms are fork, that arranges asynchronous execution, and join, that doesn't proceed until the task's result has been computed. Computations should ideally avoid synchronized methods or blocks, and should minimize other blocking synchronization apart from joining other tasks or using synchronizers such as Phasers that are advertised to cooperate with fork/join scheduling. Subdividable tasks should also not perform blocking I/O, and should ideally access variables that are completely independent of those accessed by other running tasks. These guidelines are loosely enforced by not permitting checked exceptions such as IOExceptions to be thrown. However, computations may still encounter unchecked exceptions, that are rethrown to callers attempting to join them. These exceptions may additionally include RejectedExecutionException stemming from internal resource exhaustion, such as failure to allocate internal task queues. Rethrown exceptions behave in the same way as regular exceptions, but, when possible, contain stack traces (as displayed for example using ex.printStackTrace()) of both the thread that initiated the computation as well as the thread actually encountering the exception; minimally only the latter.

ForkJoinTask 是 Future 的一种轻量级形式。ForkJoinTask 的效率源于一组限制（只有部分静态可执行），反映了它们作为计算纯函数或操作纯孤立对象的计算任务的主要用途。主要的协调机制是 fork（安排异步执行）和 join（在任务的结果被计算出来之前不继续执行）。计算最好避免同步方法或块，并且除了加入其他任务或使用同步器（如 Phasers）外，应尽量减少其他阻塞性同步，这些同步器被宣传为与 fork/join 调度合作。可细分的任务也不应该执行阻塞的 I/O，并且最好是访问完全独立于其他运行任务所访问的变量。这些准则通过不允许抛出 IOExceptions 等检查过的异常而得到宽松的执行。然而，计算仍然可能遇到未检查的异常，这些异常会被重新抛给试图加入它们的调用者。这些异常可能还包括源于内部资源耗尽的 RejectedExecutionException，例如未能分配内部任务队列。重新抛出的异常的行为方式与普通异常相同，但在可能的情况下，包含启动计算的线程和实际遇到异常的线程的堆栈痕迹（例如用 ex.printStackTrace()显示）；最小情况下只包含后者。

It is possible to define and use ForkJoinTasks that may block, but doing do requires three further considerations: (1) Completion of few if any other tasks should be dependent on a task that blocks on external synchronization or I/O. Event-style async tasks that are never joined (for example, those subclassing CountedCompleter) often fall into this category. (2) To minimize resource impact, tasks should be small; ideally performing only the (possibly) blocking action. (3) Unless the ForkJoinPool.ManagedBlocker API is used, or the number of possibly blocked tasks is known to be less than the pool's ForkJoinPool.getParallelism level, the pool cannot guarantee that enough threads will be available to ensure progress or good performance.

定义和使用可能阻塞的 ForkJoinTasks 是可能的，但这样做需要进一步考虑三个问题。(1) 其他任务的完成，如果有的话，应该依赖于一个在外部同步或 I/O 上阻塞的任务。那些从未被加入的事件式异步任务（例如，那些子类化的 CountedCompleter）往往属于这一类。(2) 为了最小化资源影响，任务应该是小的；最好只执行（可能）阻塞的动作。(3) 除非使用 ForkJoinPool.ManagedBlocker API，或者已知可能被阻塞的任务数量少于池的 ForkJoinPool.getParallelism 水平，否则池不能保证有足够的线程来保证进度或良好的性能。

The primary method for awaiting completion and extracting results of a task is join, but there are several variants: The Future.get methods support interruptible and/or timed waits for completion and report results using Future conventions. Method invoke is semantically equivalent to fork(); join() but always attempts to begin execution in the current thread. The "quiet" forms of these methods do not extract results or report exceptions. These may be useful when a set of tasks are being executed, and you need to delay processing of results or exceptions until all complete. Method invokeAll (available in multiple versions) performs the most common form of parallel invocation: forking a set of tasks and joining them all.

等待完成和提取任务结果的主要方法是 join，但也有几个变种。Future.get 方法支持可中断的和/或定时的等待完成，并使用 Future 惯例报告结果。方法 invoke 在语义上等同于 fork()；join()，但总是试图在当前线程中开始执行。这些方法的 "安静 "形式不提取结果或报告异常。当一组任务被执行时，这些方法可能很有用，你需要延迟处理结果或异常直到全部完成。方法 invokeAll（在多个版本中可用）执行最常见的并行调用形式：fork 一组任务并将其全部 join。

In the most typical usages, a fork-join pair act like a call (fork) and return (join) from a parallel recursive function. As is the case with other forms of recursive calls, returns (joins) should be performed innermost-first. For example, a.fork(); b.fork(); b.join(); a.join(); is likely to be substantially more efficient than joining a before b.

在最典型的使用中，fork-join 对就像一个并行递归函数的调用（fork）和返回（join）。就像其他形式的递归调用一样，返回（join）应该先执行最内层的。例如，a.fork(); b.fork(); b.join(); a.join(); 可能比在 b 之前连接 a 更有效率。

The execution status of tasks may be queried at several levels of detail: isDone is true if a task completed in any way (including the case where a task was cancelled without executing); isCompletedNormally is true if a task completed without cancellation or encountering an exception; isCancelled is true if the task was cancelled (in which case getException returns a CancellationException); and isCompletedAbnormally is true if a task was either cancelled or encountered an exception, in which case getException will return either the encountered exception or CancellationException.

任务的执行状态可以在几个细节层次上进行查询。如果一个任务以任何方式完成（包括任务没有执行就被取消的情况），则 isDone 为真；如果一个任务在没有取消或遇到异常的情况下完成，则  isCompletedNormally 为真；如果任务被取消（这种情况下 getException 返回CancellationException），则 isCancelled 为真；如果一个任务被取消或遇到异常，则isCompletedAbnormally 为真，在这种情况下 getException 将返回遇到的异常或 CancellationException。

The ForkJoinTask class is not usually directly subclassed. Instead, you subclass one of the abstract classes that support a particular style of fork/join processing, typically RecursiveAction for most computations that do not return results, RecursiveTask for those that do, and CountedCompleter for those in which completed actions trigger other actions. Normally, a concrete ForkJoinTask subclass declares fields comprising its parameters, established in a constructor, and then defines a compute method that somehow uses the control methods supplied by this base class.

ForkJoinTask 类通常不被直接子类化。相反，你要子类化一个支持特定风格的分叉/连接处理的抽象类，通常 RecursiveAction 用于大多数不返回结果的计算，RecursiveTask 用于那些返回结果的计算，而 CountedCompleter 用于那些完成的动作触发其他动作的计算。通常，一个具体的 ForkJoinTask 子类声明由其参数组成的字段，在构造函数中建立，然后定义一个计算方法，该方法以某种方式使用该基类提供的控制方法。

Method join and its variants are appropriate for use only when completion dependencies are acyclic; that is, the parallel computation can be described as a directed acyclic graph (DAG). Otherwise, executions may encounter a form of deadlock as tasks cyclically wait for each other. However, this framework supports other methods and techniques (for example the use of Phaser, helpQuiesce, and complete) that may be of use in constructing custom subclasses for problems that are not statically structured as DAGs. To support such usages, a ForkJoinTask may be atomically tagged with a short value using setForkJoinTaskTag or compareAndSetForkJoinTaskTag and checked using getForkJoinTaskTag. The ForkJoinTask implementation does not use these protected methods or tags for any purpose, but they may be of use in the construction of specialized subclasses. For example, parallel graph traversals can use the supplied methods to avoid revisiting nodes/tasks that have already been processed. (Method names for tagging are bulky in part to encourage definition of methods that reflect their usage patterns.)

join 方法及其变体只适合在完成依赖是无环的情况下使用；也就是说，并行计算可以被描述为一个有向无环图（DAG）。否则，由于任务之间的循环等待，执行可能会遇到某种形式的死锁。然而，这个框架支持其他的方法和技术（例如使用 Phaser、helpQuiesce 和 complete），这些方法和技术在为那些不是静态结构的 DAG的问题构建自定义子类时可能会有用。为了支持这种使用，ForkJoinTask 可以使用 setForkJoinTaskTag 或 compareAndSetForkJoinTaskTag 被原子化地标记为一个短值，并使用getForkJoinTaskTag 进行检查。ForkJoinTask 的实现并不使用这些受保护的方法或标签，但它们在构造专门的子类时可能有用。例如，并行图的遍历可以使用提供的方法来避免重新访问已经被处理过的节点/任务。(标签的方法名称很庞大，部分原因是为了鼓励对反映其使用模式的方法进行定义）。

Most base support methods are final, to prevent overriding of implementations that are intrinsically tied to the underlying lightweight task scheduling framework. Developers creating new basic styles of fork/join processing should minimally implement protected methods exec, setRawResult, and getRawResult, while also introducing an abstract computational method that can be implemented in its subclasses, possibly relying on other protected methods provided by this class.
ForkJoinTasks should perform relatively small amounts of computation. Large tasks should be split into smaller subtasks, usually via recursive decomposition. As a very rough rule of thumb, a task should perform more than 100 and less than 10000 basic computational steps, and should avoid indefinite looping. If tasks are too big, then parallelism cannot improve throughput. If too small, then memory and internal task maintenance overhead may overwhelm processing.

ForkJoinTasks 应该执行相对较小的计算量。大型任务应该被分割成更小的子任务，通常是通过递归分解。作为一个非常粗略的经验法则，一个任务应该执行超过 100 和少于 10000 的基本计算步骤，并且应该避免无限循环。如果任务太大，那么并行性就不能提高吞吐量。如果太小，那么内存和内部任务维护的开销可能会使处理工作不堪重负。

This class provides adapt methods for Runnable and Callable, that may be of use when mixing execution of ForkJoinTasks with other kinds of tasks. When all tasks are of this form, consider using a pool constructed in asyncMode.

这个类提供了 Runnable 和 Callable 的适应方法，在 ForkJoinTasks 与其他类型的任务混合执行时可能有用。当所有的任务都是这种形式时，可以考虑使用一个以 asyncMode 构建的池。

ForkJoinTasks are Serializable, which enables them to be used in extensions such as remote execution frameworks. It is sensible to serialize tasks only before or after, but not during, execution. Serialization is not relied on during execution itself.

ForkJoinTasks 是可序列化的，这使得它们可以在扩展中使用，如远程执行框架。只在执行之前或之后，而不是在执行过程中，对任务进行序列化是明智的。序列化在执行过程中是不被依赖的。

### 8. CompletionService
A service that decouples the production of new asynchronous tasks from the consumption of the results of completed tasks. Producers submit tasks for execution. Consumers take completed tasks and process their results in the order they complete. A CompletionService can for example be used to manage asynchronous I/O, in which tasks that perform reads are submitted in one part of a program or system, and then acted upon in a different part of the program when the reads complete, possibly in a different order than they were requested.

一种服务，将新的异步任务的生产与已完成任务的结果的消费解耦。生产者提交任务供执行。消费者接受已完成的任务，并按其完成的顺序处理其结果。例如，CompletionService 可以用来管理异步 I/O，其中执行读取的任务在程序或系统的一个部分提交，然后在程序的另一个部分在读取完成后进行操作，可能与请求的顺序不同。

Typically, a CompletionService relies on a separate Executor to actually execute the tasks, in which case the CompletionService only manages an internal completion queue. The ExecutorCompletionService class provides an implementation of this approach.

通常，一个 CompletionService 依赖于一个单独的 Executor 来实际执行任务，在这种情况下，CompletionService 只管理一个内部完成队列。ExecutorCompletionService 类提供了这种方法的一个实现。

Memory consistency effects: Actions in a thread prior to submitting a task to a CompletionService happen-before actions taken by that task, which in turn happen-before actions following a successful return from the corresponding take().

内存一致性影响。在向 CompletionService 提交任务之前，线程中的行动发生在该任务所采取的行动之前，而这些行动又发生在从相应的 take() 成功返回后的行动之前。

### 9. ExecutorCompletionService
A CompletionService that uses a supplied Executor to execute tasks. This class arranges that submitted tasks are, upon completion, placed on a queue accessible using take. The class is lightweight enough to be suitable for transient use when processing groups of tasks.

一个使用提供的 Executor 来执行任务的 CompletionService。该类安排提交的任务在完成后被放在一个可以使用 take 的队列中。该类是轻量级的，足以适合在处理任务组时临时使用。

#### Usage Examples
Suppose you have a set of solvers for a certain problem, each returning a value of some type Result, and would like to run them concurrently, processing the results of each of them that return a non-null value, in some method use(Result r). You could write this as:

假设你有一组针对某个问题的求解器，每个求解器都返回一个某种类型的结果值，并且想同时运行它们，在某个方法use(Result r) 中处理每个返回非空值的结果。你可以这样写。

```
    void solve(Executor e, Collection<Callable<Result>> solvers) 
        throws InterruptedException, ExecutionException {
        CompletionService<Result> ecs = new ExecutorCompletionService<>(e);
        for (Callable<Result> s : solvers)
            ecs.submit(s);
        int n = solvers.size();
        for (int i = 0; i < n; i++) {
            Result r = ecs.take().get();
            if (r != null)
                use(r);
        }
    }
```

Suppose instead that you would like to use the first non-null result of the set of tasks, ignoring any that encounter exceptions, and cancelling all other tasks when the first one is ready:

假设你想使用一组任务中第一个非空的结果，忽略任何遇到异常的任务，并在第一个任务准备好后取消所有其他任务。

```
    void solve(Executor e, Collection<Callable<Result>> solvers)
        throws InterruptedException {
        CompletionService<Result> ecs = new ExecutorCompletionService<>(e);
        int n = solvers.size();
        List<Future<Result>> futures = new ArrayList<>(n);
        Result result = null;
        try {
            for (Callable<Result> s : solvers)
                futures.add(ecs.submit(s));
            for (int i = 0; i < n; ++i) {
                try {
                    Result r = ecs.take().get();
                    if (r != null) {
                        result = r;
                        break;
                    }
                } catch(ExecutionException ignore) {}
            }
        } finally {
            for (Future<Result> f : futures)
                f.cancel(true);
        }
        if (result != null) 
            use(result);
    }
```