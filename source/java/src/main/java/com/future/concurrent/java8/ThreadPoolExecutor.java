package com.future.concurrent.java8;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池设计大纲
 * <p>
 * 1. shutdown/shutdownNow
 * <p>
 * 2. execute/submit/invokeAll/invokeAny
 * <p>
 * 3. constructor method
 * ** 1. core and maximum pool sizes
 * ** 2. On-demand construction
 * ** 3. Creating new threads
 * ** 4. Keep-alive times
 * ** 5. Queuing
 * ** 6. Rejected Tasks
 * <p>
 * 4. blocking queue
 * <p>
 * 5. reject policy
 * <p>
 * 6. exception in task
 * 7. Hook methods
 * 8. Queue maintenance
 * 9. Finalization
 *
 * @author future
 */
@SuppressWarnings("unused")
class ThreadPoolExecutor {
    /**
     * 线程池的控制状态
     * 高 3 位 rs 表示线程池状态: runState
     * 低 29 为 wc 表示工作线程的数量: workerCount
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    private static final int STOP = 1 << COUNT_BITS;
    private static final int TIDYING = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    /**
     * 高 3 位 rs 表示线程池状态: runState
     * 低 29 为 wc 表示工作线程的数量: workerCount
     */
    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    private void decrementWorkerCount() {
        //noinspection StatementWithEmptyBody
        while (!compareAndDecrementWorkerCount(ctl.get())) ;
    }

    private static final boolean ONLY_ONE = true;

    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile long keepAliveTime;
    private volatile ThreadFactory threadFactory;
    private volatile RejectedExecutionHandler handler;

    private final BlockingQueue<Runnable> workQueue;

    private final HashSet<Worker> workers = new HashSet<>();

    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    private int largestPoolSize;
    private long completedTaskCount;

    private volatile boolean allowCoreThreadTimeOut;

    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();

    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker worker : workers) {
                worker.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     *
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) {
                    // 只尝试打断一个线程
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                // 要么线程池没有被关闭，要么正在关闭中，则退出
                return;
            }
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * 通常使用 drainTo 将任务队列排入新列表。
     * 但是，如果队列是 DelayQueue 或任何其他类型的队列，poll 或 drainTo 可能无法删除其中的某些元素，
     * 那么它会逐个删除它们。
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) taskList.add(r);
            }
        }
        return taskList;
    }

    /**
     * 检查是否可以根据当前池状态和给定绑定（核心或最大值）添加新的辅助线程。
     * 如果是这样，将相应地调整工作进程计数，
     * 并且如果可能，将创建并启动一个新的工作线程，并将firstTask作为其第一个任务运行。
     * 如果池已停止或符合关闭条件，则此方法返回false。
     * 如果线程工厂在被请求时未能创建线程，它也会返回false。
     * 如果线程创建失败，或者是由于线程工厂返回null，
     * 或者是由于异常（通常是thread.start（）中的OutOfMemoryError），我们将完全回滚。
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);

            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()))
                return false;
            for (; ; ) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                if (compareAndIncrementWorkerCount(c)) break retry;
                c = ctl.get();
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;

        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    int rs = runStateOf(ctl.get());
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) throw new IllegalThreadStateException();
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }

        } finally {
            if (!workerStarted) addWorkerFailed(w);
        }
        return workerStarted;
    }

    private void addWorkerFailed(Worker worker) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (worker != null) {
                workers.remove(worker);
            }
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 为垂死的工人进行清理和记账。仅从工作线程调用。
     * 除非设置了 completedAbruptly，否则假定 workerCount 已被调整以考虑退出。
     * 此方法从工作线程集中删除线程，
     * 如果由于用户任务异常而退出工作线程，或者如果正在运行的工作线程少于corePoolSize，
     * 或者队列为非空但没有工作线程，则可能终止池或替换工作线程。
     * <p>
     * Abruptly: 突然地
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min)
                    return;
            }
            addWorker(null, false);
        }
    }

    /**
     * 根据当前配置设置对任务执行阻塞或定时等待，或者如果此工作进程由于以下任何原因必须退出，则返回 null:
     * 1。有超过 maximumPoolSize 工作线程（由于调用了 setMaximumPoolSize）。
     * 2.池已被停止。
     * 3.池已关闭，队列为空。
     * 4.此工作线程在等待任务时超时，超时工作线程在超时等待前后都会被终止
     * （即allowCoreThreadTimeOut | | workerCount>corePoolSize），
     * 如果队列非空，则此工作线程不是池中的最后一个线程。
     */
    private Runnable getTask() {
        boolean timeOut = false;

        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);

            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timeOut)) && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c)) return null;
                continue;
            }

            try {
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) return r;
                timeOut = true;
            } catch (InterruptedException retry) {
                timeOut = false;
            }
        }
    }

    /**
     * 主工作运行循环。重复地从队列中获取任务并执行它们，同时处理许多问题：
     * 1。我们可以从一个初始任务开始，在这种情况下，我们不需要得到第一个任务。
     * ** 否则，只要池在运行，我们就可以从getTask获取任务。
     * ** 如果返回 null，则工作进程将由于池状态或配置参数的更改而退出。
     * ** 其他退出源于外部代码中的异常抛出，在这种情况下 completedAbruptly 保持不变，
     * ** 这通常会导致 processWorkerExit 替换此线程。
     * <p>
     * 2.在运行任何任务之前，获取锁以防止任务执行时其他池中断，然后我们确保除非池停止，否则此线程没有中断设置。
     * <p>
     * 3.每个任务运行之前都会调用 beforeExecute，这可能会引发异常，在这种情况下，我们会导致线程死亡
     * ** （在不处理任务的情况下中断 completedAbruptly true的循环）。
     * <p>
     * 4.假设 beforeExecute 正常完成，我们运行任务，收集其抛出的任何异常以发送给 afterExecute。
     * ** 我们分别处理 RuntimeException、Error（这两个规范都保证我们可以捕获）和任意丢弃。
     * ** 因为我们无法在 Runnable 内重新播放丢弃的内容。运行时，我们将它们包装在退出时的错误中（到线程的 UncaughtExceptionHandler）。
     * ** 任何抛出的异常也会保守地导致线程死亡。
     * <p>
     * 5. 在 task.run 完成后，我们调用 afterExecute，这也可能引发异常，这也将导致线程死亡。
     * ** 根据 JLS 第 14.20 节，即使 task.run() throws, 此例外情况也将生效。
     * ** 异常机制的净效果是 afterExecute 和线程的 UncaughtExceptionHandler 具有尽可能准确的信息，
     * ** 我们可以提供有关用户代码遇到的任何问题的信息。
     */
    final void runWorker(Worker worker) {
        Thread wt = Thread.currentThread();
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        worker.unlock(); // allow interrupts
        boolean completeAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                worker.lock();

                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)))
                        && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException | Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    worker.completedTasks++;
                    worker.unlock();
                }
            }
            completeAbruptly = false;
        } finally {
            processWorkerExit(worker, completeAbruptly);
        }
    }

    /**
     * 在将来的某个时候执行给定的任务。任务可以在新线程或现有池线程中执行。
     * 如果由于此执行器已关闭或已达到其容量而无法提交任务执行，
     * 则该任务将由当前 RejectedExecutionHandler 处理。
     * <p>
     * Proceed in 3 steps
     * <p>
     * 1. 如果正在运行的线程少于 corePoolSize，请尝试以给定命令作为第一个线程启动新线程任务。
     * 对 addWorker 的调用以原子方式检查运行状态和 workerCount，
     * 从而防止可能增加在不应该的情况下，通过返回 false 执行线程。
     * <p>
     * 2.如果任务可以成功排队，那么我们仍然需要再次检查是否应该添加线程（因为自上次检查以来，可能已有的已死亡）
     * 或自进入此方法后，池已关闭。
     * 所以我们重新检查状态，如有必要，在以下情况下回滚排队已停止，
     * 如果没有线程，则启动新线程。
     * <p>
     * 3.如果无法对任务排队，则尝试添加新任务线程
     * 如果失败了，我们知道我们已经被关闭或饱和了所以拒绝这个任务。
     */
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                addWorker(null, false);
            }
        } else if (!addWorker(command, false))
            reject(command);
    }

    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty.
        return removed;
    }

    /**
     * 启动有序关机，执行以前提交的任务，但不接受新任务。如果调用已经关闭，则调用没有其他效果。
     * 此方法不会等待以前提交的任务完成执行。使用此选项可以完成此操作。
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        ;
        try {
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutDown();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize && addWorker(null, true);
    }

    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true)) {
            ++n;
        }
        return n;
    }

    public boolean allowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) {
            interruptIdleWorkers();
        }
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) throw new IllegalArgumentException();
        if (time == 0 && allowCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0) interruptIdleWorkers();
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
        } else if (delta > 0) {
            int k = Math.max(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 尝试从工作队列中删除所有已取消的 Future 任务。此方法可用于存储回收操作，对功能没有其他影响。
     * 取消的任务永远不会执行，但可能会累积在工作队列中，直到工作线程可以主动删除它们。
     * 调用此方法将尝试立即删除它们。
     * 但是，如果存在其他线程的干扰，此方法可能无法删除任务。
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            q.removeIf(r -> r instanceof Future<?> && ((Future<?>) r).isCancelled());
        } catch (ConcurrentModificationException fallThrough) {
            for (Object r : q.toArray()) {
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }
        tryTerminate();
    }

    /* Statistics */

    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }
    /* end */

    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                        "Shutting down"));
        return super.toString() +
                "[" + rs +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    void onShutDown() {
    }

    // start of hook methods.

    /**
     * Hook Method
     */
    protected void terminated() {
    }

    protected void afterExecute(Runnable r, Throwable t) {
    }

    protected void beforeExecute(Thread t, Runnable r) {
    }

    // end of hook methods.

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = keepAliveTime;
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;

        final Thread thread;
        Runnable firstTask;
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        @Override
        public void run() {
            runWorker(this);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    static class CallerRunsPolicy implements RejectedExecutionHandler {
        public CallerRunsPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }

    static class AbortPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

    static class DiscardPolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    static class DiscardOldestPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                executor.getQueue().poll();
                executor.execute(r);
            }
        }
    }
}
