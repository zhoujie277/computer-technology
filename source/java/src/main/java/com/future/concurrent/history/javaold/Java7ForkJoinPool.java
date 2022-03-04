package com.future.concurrent.history.javaold;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({"unused", "StatementWithEmptyBody"})
public
class Java7ForkJoinPool extends AbstractExecutorService {

    private static final int shortMask = 0xffff;

    private static final int MAX_THREADS = 0x7FFF;

    public interface ForkJoinWorkerThreadFactory {
        Java7ForkJoinWorkerThread newThread(Java7ForkJoinPool pool);
    }

    static class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        public Java7ForkJoinWorkerThread newThread(Java7ForkJoinPool pool) {
            try {
                return new Java7ForkJoinWorkerThread(pool);
            } catch (OutOfMemoryError oom) {
                return null;
            }
        }
    }

    private static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();

    private static final RuntimePermission modifyThreadPermission = new RuntimePermission("modifyThread");

    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(modifyThreadPermission);
    }

    private static final AtomicInteger poolNumberGenerator = new AtomicInteger();

    /**
     * 保存池中所有工作线程的数组。第一次使用时初始化。数组大小必须是 2 的幂。
     * 更新和替换受到 workerLock 的保护，但它总是保持在足够一致的状态下，
     * 可以随机访问而无需 锁的情况下，被执行工作偷窃的工作者随机访问。
     */
    volatile Java7ForkJoinWorkerThread[] workers;

    private final ReentrantLock workerLock;

    private final Condition termination;

    private Thread.UncaughtExceptionHandler ueh;

    private final ForkJoinWorkerThreadFactory factory;

    /**
     * 线程堆栈的头部，这些线程是为了在其他线程阻塞时维持并行性而创建的，
     * 但在其他线程阻塞时已经 暂停了，当平行度上升时。
     */
    private volatile WaitQueueNode spareStack;

    /**
     * Sum of per-thread steal counts, updated only when threads are
     * idle or terminating.
     */
    private final AtomicLong stealCount;

    /**
     * Queue for external submissions.
     */
    private final LinkedTransferQueue<Java7ForkJoinTask<?>> submissionQueue;

    /**
     * Head of Treiber stack for barrier sync. See below for explanation.
     */
    private volatile WaitQueueNode syncStack;

    /**
     * The count for event barrier
     */
    private volatile long eventCount;

    /**
     * Pool number, just for assigning useful names to worker threads
     */
    private final int poolNumber;

    /**
     * The maximum allowed pool size
     */
    private volatile int maxPoolSize;

    /**
     * The desired parallelism level, updated only under workerLock.
     */
    private volatile int parallelism;

    /**
     * True if use local fifo, not default lifo, for local polling
     */
    private volatile boolean locallyFifo;

    /**
     * 保存总线程（即已创建且尚未终止）和运行线程（即在连接或其他管理同步中未受阻）的数量，打包成一个 int，
     * 以确保在做出创建和暂停备用线程的决定时有一致的快照。仅由 CAS 更新。
     * 注意：在 updateRunningCount 和 preJoin 中的 CAS 假定运行中的活动计数 处于低位，
     * 所以如果这一点发生变化，就需要进行修改。
     */
    private volatile int workerCounts;

    private static int totalCountOf(int s) {
        return s >>> 16;
    }

    private static int runningCountOf(int s) {
        return s & shortMask;
    }

    private static int workerCountsFor(int t, int r) {
        return (t << 16) + r;
    }

    /**
     * 将 delta（可能是负数）添加到运行计数中。
     * 必须在任何管理性同步（即主要是连接）之前（负参数）和之后（正参数）调用此功能。
     * 任何管理性同步（即主要是 join ）之前和之后调用。
     */
    final void updateRunningCount(int delta) {
        int s;
        do {
        } while (!casWorkerCounts(s = workerCounts, s + delta));
    }

    private void updateWorkerCount(int delta) {
        int d = delta + (delta << 16);
        int s;
        do {
        } while (!casWorkerCounts(s = workerCounts, s + d));
    }

    /**
     * 生命周期控制。高字包含 runState，低字包含正在（可能）执行任务的工作者的数量。
     * 这个值在 worker 获得任务运行之前被原子化地递增，而当 worker 没有任务且找不到任务时则被递减。
     * 这两个字段被捆绑在一起，以支持正确的终止触发。
     * 注意：activeCount CAS'es 通过假定活动计数在低字段中作弊，
     * 所以如果这一点发生变化，需要修改
     */
    private volatile int runControl;

    // RunState values. Order among values matters
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int TERMINATING = 2;
    private static final int TERMINATED = 3;

    private static int runStateOf(int c) {
        return c >>> 16;
    }

    private static int activeCountOf(int c) {
        return c & shortMask;
    }

    private static int runControlFor(int r, int a) {
        return (r << 16) + a;
    }

    final boolean tryIncrementActiveCount() {
        int c = runControl;
        return casRunControl(c, c + 1);
    }

    /**
     * 尝试递减活动计数；在争论中失败。 成功时可能会触发终止。当工作者找不到任务时，会被调用。
     */
    final boolean tryDecrementActiveCount() {
        int c = runControl;
        int nextc = c - 1;
        if (!casRunControl(c, nextc))
            return false;
        if (canTerminateOnShutdown(nextc))
            terminateOnShutdown();
        return true;
    }

    /**
     * 如果参数代表零活动计数和非零运行状态，这是在关机时终止的触发条件。
     */
    private static boolean canTerminateOnShutdown(int c) {
        return ((c & -c) >>> 16) != 0;
    }

    private boolean transitionRunStateTo(int state) {
        for (; ; ) {
            int c = runControl;
            if (runStateOf(c) >= state)
                return false;
            if (casRunControl(c, runControlFor(state, activeCountOf(c))))
                return true;
        }
    }

    /**
     * 控制是否增加备用以保持并行性
     */
    private volatile boolean maintainsParallelism;

    public Java7ForkJoinPool() {
        this(Runtime.getRuntime().availableProcessors(), defaultForkJoinWorkerThreadFactory);
    }

    public Java7ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory);
    }

    public Java7ForkJoinPool(ForkJoinWorkerThreadFactory factory) {
        this(Runtime.getRuntime().availableProcessors(), factory);
    }

    public Java7ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory) {
        if (parallelism <= 0 || parallelism > MAX_THREADS)
            throw new IllegalArgumentException();
        if (factory == null)
            throw new NullPointerException();
        checkPermission();
        this.factory = factory;
        this.parallelism = parallelism;
        this.maxPoolSize = MAX_THREADS;
        this.maintainsParallelism = true;
        this.poolNumber = poolNumberGenerator.incrementAndGet();
        this.workerLock = new ReentrantLock();
        this.termination = workerLock.newCondition();
        this.stealCount = new AtomicLong();
        this.submissionQueue = new LinkedTransferQueue<>();
    }

    private Java7ForkJoinWorkerThread createWorker(int index) {
        Thread.UncaughtExceptionHandler h = ueh;
        Java7ForkJoinWorkerThread w = factory.newThread(this);
        if (w != null) {
            w.poolIndex = index;
            w.setDaemon(true);
            w.setAsyncMode(locallyFifo);
            w.setName("ForkJoinPool-" + poolNumber + "-worker-" + index);
            if (h != null)
                w.setUncaughtExceptionHandler(h);
        }
        return w;
    }

    /**
     * 返回给定池子大小的 Worker 数组的一个好的大小。目前要求大小为 2 的幂。
     */
    private static int arraySizeFor(int poolSize) {
        if (poolSize <= 1)
            return 1;
        int c = poolSize >= MAX_THREADS ? MAX_THREADS : (poolSize - 1);
        c |= c >>> 1;
        c |= c >>> 2;
        c |= c >>> 4;
        c |= c >>> 8;
        c |= c >>> 16;
        return c + 1;
    }

    private Java7ForkJoinWorkerThread[] ensureWorkerArrayCapacity(int newLength) {
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws == null)
            return workers = new Java7ForkJoinWorkerThread[arraySizeFor(newLength)];
        else if (newLength > ws.length) {
            return workers = Arrays.copyOf(ws, arraySizeFor(newLength));
        }
        return ws;
    }

    /**
     * 试图在一个或多个终止后将工人缩减为更小的数组。
     */
    private void tryShrinkWorkerArray() {
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws != null) {
            int len = ws.length;
            int last = len - 1;
            while (last >= 0 && ws[last] == null)
                --last;
            int newLength = arraySizeFor(last + 1);
            if (newLength < len)
                workers = Arrays.copyOf(ws, newLength);
        }
    }

    final void ensureWorkerInitialization() {
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws == null) {
            final ReentrantLock lock = this.workerLock;
            lock.lock();
            try {
                ws = workers;
                if (ws == null) {
                    int ps = parallelism;
                    ws = ensureWorkerArrayCapacity(ps);
                    for (int i = 0; i < ps; i++) {
                        Java7ForkJoinWorkerThread w = createWorker(i);
                        if (w != null) {
                            ws[i] = w;
                            w.start();
                            updateWorkerCount(1);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void createAndStartAddedWorkers() {
        resumeAllSpares();
        int ps = parallelism;
        Java7ForkJoinWorkerThread[] ws = ensureWorkerArrayCapacity(ps);
        int len = ws.length;
        int k = 0;
        while (k < len) {
            if (ws[k] != null) {
                ++k;
                continue;
            }
            int s = workerCounts;
            int tc = totalCountOf(s);
            int rc = runningCountOf(s);
            if (rc >= ps || tc >= ps)
                break;
            if (casWorkerCounts(s, workerCountsFor(tc + 1, rc + 1))) {
                Java7ForkJoinWorkerThread w = createWorker(k);
                if (w != null) {
                    ws[k++] = w;
                    w.start();
                } else {
                    updateWorkerCount(-1);
                    break;
                }
            }
        }
    }

    private <T> void doSubmit(Java7ForkJoinTask<T> task) {
        if (task == null)
            throw new NullPointerException();
        if (isShutdown())
            throw new RejectedExecutionException();
        if (workers == null)
            ensureWorkerInitialization();
        submissionQueue.offer(task);
        signalIdleWorkers();
    }

    private void signalIdleWorkers() {
        long c;
        do {
        } while (!casEventCount(c = eventCount, c + 1));
        ensureSync();
    }

    /**
     * 确保没有线程在等待计数从进入此方法时读取的 eventCount 的当前值前进，必要时释放等待的线程。
     */
    final long ensureSync() {
        long c = eventCount;
        WaitQueueNode q;
        while ((q = syncStack) != null && q.count < c) {
            if (casBarrierStack(q, null)) {
                do {
                    q.signal();
                } while ((q = q.next) != null);
                break;
            }
        }
        return c;
    }

    final void signalWork() {
        long c;
        WaitQueueNode q;
        if (syncStack != null && casEventCount(c = eventCount, c + 1)
                && (((q = syncStack) != null && q.count <= c) &&
                (!casBarrierStack(q, q.next) || !q.signal())))
            ensureSync();
    }

    private void terminate() {
        if (transitionRunStateTo(TERMINATING)) {
            stopAllWorkers();
            resumeAllSpares();
            signalIdleWorkers();
            cancelQueuedSubmissions();
            cancelQueuedWorkerTasks();
            interruptUnterminatedWorkers();
            signalIdleWorkers();
        }
    }

    private void terminateOnShutdown() {
        if (!hasQueuedSubmissions() && canTerminateOnShutdown(runControl))
            terminate();
    }

    @Override
    public void shutdown() {
        checkPermission();
        transitionRunStateTo(SHUTDOWN);
        if (canTerminateOnShutdown(runControl)) {
            if (workers == null) {
                final ReentrantLock lock = this.workerLock;
                lock.lock();
                try {
                    if (workers == null) {
                        terminate();
                        transitionRunStateTo(TERMINATED);
                        termination.signalAll();
                    }
                } finally {
                    lock.unlock();
                }
            }
            terminateOnShutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        checkPermission();
        terminate();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return runStateOf(runControl) >= SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return runStateOf(runControl) == TERMINATED;
    }

    public boolean isTerminating() {
        return runStateOf(runControl) == TERMINATING;
    }

    final boolean isProcessingTasks() {
        return runStateOf(runControl) < TERMINATING;
    }

    /**
     * 阻塞，直到所有任务在关闭请求后完成执行，或超时发生，或当前线程被中断，以先发生者为准。
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            for (; ; ) {
                if (isTerminated())
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 终止工作者的回调。空出相应的工作者槽，如果是终止，则尝试终止；否则试图缩减工作者阵列。
     */
    final void workerTerminated(Java7ForkJoinWorkerThread w) {
        updateStealCount(w);
        updateWorkerCount(-1);
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            Java7ForkJoinWorkerThread[] ws = workers;
            if (ws != null) {
                int idx = w.poolIndex;
                if (idx >= 0 && idx < ws.length && ws[idx] == w)
                    ws[idx] = null;
                if (totalCountOf(workerCounts) == 0) {
                    terminate();
                    transitionRunStateTo(TERMINATED);
                    termination.signalAll();
                } else if (isProcessingTasks()) {
                    tryShrinkWorkerArray();
                    tryResumeSpare(true);
                }
            }
        } finally {
            lock.unlock();
        }
        signalIdleWorkers();
    }

    private void cancelQueuedSubmissions() {
        Java7ForkJoinTask<?> task;
        while ((task = pollSubmission()) != null)
            task.cancel(false);
    }

    public void execute(Java7ForkJoinTask<?> task) {
        doSubmit(task);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(Runnable task) {
        Java7ForkJoinTask<?> job;
        if (task instanceof Java7ForkJoinTask<?>)
            job = (Java7ForkJoinTask<?>) task;
        else
            job = Java7ForkJoinTask.adapt(task, null);
        doSubmit(job);
    }

    @Override
    public <T> Java7ForkJoinTask<T> submit(Callable<T> task) {
        Java7ForkJoinTask<T> job = Java7ForkJoinTask.adapt(task);
        doSubmit(job);
        return job;
    }

    @Override
    public <T> Java7ForkJoinTask<T> submit(Runnable task, T result) {
        Java7ForkJoinTask<T> job = Java7ForkJoinTask.adapt(task, result);
        doSubmit(job);
        return job;
    }

    @Override
    public Java7ForkJoinTask<?> submit(Runnable task) {
        Java7ForkJoinTask<?> job;
        if (task instanceof Java7ForkJoinTask<?>)
            job = (Java7ForkJoinTask<?>) task;
        else
            job = Java7ForkJoinTask.adapt(task, null);
        doSubmit(job);
        return job;
    }

    public <T> Java7ForkJoinTask<T> submit(Java7ForkJoinTask<T> task) {
        doSubmit(task);
        return task;
    }

    public <T> T invoke(Java7ForkJoinTask<T> task) {
        doSubmit(task);
        return task.join();
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler h;
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            h = ueh;
        } finally {
            lock.unlock();
        }
        return h;
    }

    public Thread.UncaughtExceptionHandler setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler h) {
        checkPermission();
        Thread.UncaughtExceptionHandler old = null;
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            old = ueh;
            ueh = h;
            Java7ForkJoinWorkerThread[] ws = workers;
            if (ws != null) {
                for (Java7ForkJoinWorkerThread w : ws) {
                    if (w != null)
                        w.setUncaughtExceptionHandler(h);
                }
            }
        } finally {
            lock.unlock();
        }
        return old;
    }

    public void setParallelism(int parallelism) {
        checkPermission();
        if (parallelism <= 0 || parallelism > maxPoolSize)
            throw new IllegalArgumentException();
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            if (isProcessingTasks()) {
                int p = this.parallelism;
                this.parallelism = parallelism;
                if (parallelism > p)
                    createAndStartAddedWorkers();
                else
                    trimSpares();
            }
        } finally {
            lock.unlock();
        }
        signalIdleWorkers();
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getPoolSize() {
        return totalCountOf(workerCounts);
    }

    public int getMaximumPoolSize() {
        return maxPoolSize;
    }

    public void setMaximumPoolSize(int newMax) {
        if (newMax < 0 || newMax > MAX_THREADS)
            throw new IllegalArgumentException();
        maxPoolSize = newMax;
    }

    /**
     * 如果这个池子动态地保持其 目标平行度水平 则返回 true。
     * 如果是 false，新线程的加入只是为了避免可能出现的饥饿现象。
     */
    public boolean getMaintainsParallelism() {
        return maintainsParallelism;
    }

    public void setMaintainsParallelism(boolean maintainsParallelism) {
        this.maintainsParallelism = maintainsParallelism;
    }

    public boolean setAsyncMode(boolean async) {
        boolean oldMode = locallyFifo;
        locallyFifo = async;
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws != null) {
            for (Java7ForkJoinWorkerThread t : ws) {
                if (t != null)
                    t.setAsyncMode(async);
            }
        }
        return oldMode;
    }

    /**
     * 如果这个池子使用本地先入先出的调度模式，则返回 {@code true}，因为 fork 的任务从未被加入。
     */
    public boolean getAsyncMode() {
        return locallyFifo;
    }

    /**
     * 返回一个估计的工人线程数量，这些线程没有被阻塞，正在等待加入任务或其他管理同步。
     */
    public int getRunningThreadCount() {
        return runningCountOf(workerCounts);
    }

    /**
     * 返回当前正在窃取或执行任务的线程的估计数量。这个方法可能会高估活动线程的数量。
     */
    public int getActiveThreadCount() {
        return activeCountOf(runControl);
    }

    /**
     * 返回当前闲置的等待任务的线程数的估计值。这个方法可能会低估空闲线程的数量。
     */
    final int getIdleThreadCount() {
        int c = runningCountOf(workerCounts) - activeCountOf(runControl);
        return Math.max(c, 0);
    }

    /**
     * 如果所有的工作线程目前都处于空闲状态，则返回{@code true}。
     * 闲置的工作线程是指无法获得任务执行的工作线程，
     * 因为没有任何任务可以从其他线程那里窃取，也没有任何待提交的任务提交给池。
     * 这个方法是保守的；它可能不会在所有线程都空闲时立即返回 {@code true}，但如果 线程仍然不活跃。
     */
    public boolean isQuiescent() {
        return activeCountOf(runControl) == 0;
    }

    /**
     * 返回一个线程的工作队列中被另一个线程窃取的任务总数的估计值。
     * 报告的值低估了在池子没有静止时的实际偷窃总数。
     * 这个值对于监控和调整 fork/join 程序可能很有用：
     * 一般来说，窃取次数应该足够多，以保持线程的忙碌，但又足够少，以避免开销和线程间的争夺。
     */
    public long getStealCount() {
        return stealCount.get();
    }

    private void updateStealCount(Java7ForkJoinWorkerThread w) {
        int sc = w.getAndClearStealCount();
        if (sc != 0)
            stealCount.addAndGet(sc);
    }

    public long getQueuedTaskCount() {
        long count = 0;
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws != null) {
            for (Java7ForkJoinWorkerThread t : ws) {
                if (t != null)
                    count += t.getQueueSize();
            }
        }
        return count;
    }

    public int getQueuedSubmissionCount() {
        return submissionQueue.size();
    }

    public boolean hasQueuedSubmissions() {
        return !submissionQueue.isEmpty();
    }

    /**
     * 移除并返回下一个未执行的提交，如果有的话。
     * 这个方法在这个类的扩展中可能很有用，可以在有多个池的系统中重新分配工作。
     */
    protected Java7ForkJoinTask<?> pollSubmission() {
        return submissionQueue.poll();
    }

    private void cancelQueuedWorkerTasks() {
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            Java7ForkJoinWorkerThread[] ws = workers;
            if (ws != null) {
                for (Java7ForkJoinWorkerThread t : ws) {
                    if (t != null)
                        t.cancelTasks();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从调度队列中移除所有可用的未执行的提交任务和分叉任务，并将其添加到给定的集合中，但不改变其执行状态。
     * 这些任务可能包括人工生成或包装的任务。
     * 这个方法被设计为只在已知池子处于静止状态时才被调用。
     * 在其他时间的调用可能不会删除所有的任务。
     * 当试图将元素添加到集合 {@code c} 中时遇到的失败可能会导致元素既不在集合中，也不在任何一个集合中，
     * 或者当相关的异常被抛出时，两个集合都在。
     * 如果在操作过程中，指定的集合被修改，那么这个操作的行为将无法定义。
     * 操作过程中，如果指定的集合被修改，那么这个操作的行为将无法定义。
     */
    protected int drainTasksTo(Collection<? super Java7ForkJoinTask<?>> c) {
        int n = submissionQueue.drainTo(c);
        Java7ForkJoinWorkerThread[] ws = workers;
        if (ws != null) {
            for (Java7ForkJoinWorkerThread w : ws) {
                if (w != null)
                    n += w.drainTasksTo(c);
            }
        }
        return n;
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state, parallelism level, and
     * worker and task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        int ps = parallelism;
        int wc = workerCounts;
        int rc = runControl;
        long st = getStealCount();
        long qt = getQueuedTaskCount();
        long qs = getQueuedSubmissionCount();
        return super.toString() +
                "[" + runStateToString(runStateOf(rc)) +
                ", parallelism = " + ps +
                ", size = " + totalCountOf(wc) +
                ", active = " + activeCountOf(rc) +
                ", running = " + runningCountOf(wc) +
                ", steals = " + st +
                ", tasks = " + qt +
                ", submissions = " + qs +
                "]";
    }

    private static String runStateToString(int rs) {
        switch (rs) {
            case RUNNING:
                return "Running";
            case SHUTDOWN:
                return "Shutting down";
            case TERMINATING:
                return "Terminating";
            case TERMINATED:
                return "Terminated";
            default:
                throw new Error("Unknown run state");
        }
    }

    /**
     * 如果等待同步的工作者可以继续，则返回 {@code true}。
     * - on signal (thread == null)
     * - on event count advance (winning race to notify vs signaller)
     * - on interrupt
     * - if the first queued node, we find work available
     * 如果节点没有得到信号，退出时事件计数没有提前。
     * 那么我们也会帮助推进事件计数。
     */
    final boolean syncIsReleasable(WaitQueueNode node) {
        long prev = node.count;
        if (!Thread.interrupted() && node.thread != null &&
                (node.next != null || !Java7ForkJoinWorkerThread.hasQueuedTasks(workers))
                && eventCount == prev)
            return false;
        if (node.thread != null) {
            node.thread = null;
            long ec = eventCount;
            if (prev <= ec)
                casEventCount(ec, ec + 1);
        }
        return true;
    }

    final boolean hasNewSyncEvent(Java7ForkJoinWorkerThread w) {
        long lc = w.lastEventCount;
        long ec = ensureSync();
        if (ec == lc)
            return false;
        w.lastEventCount = ec;
        return true;
    }

    /**
     * 等待，直到事件计数从调用者持有的最后一个值开始前进，或者如果线程过多，调用者被恢复为备用，
     * 或者调用者或池子正在终止。在退出时更新调用者的事件。
     */
    final void sync(Java7ForkJoinWorkerThread w) {
        updateStealCount(w);
        while (!w.isShutdown() && isProcessingTasks() && !suspendIfSpare(w)) {
            long prev = w.lastEventCount;
            WaitQueueNode node = null;
            WaitQueueNode h;
            while (eventCount == prev && ((h = syncStack) == null || h.count == prev)) {
                if (node == null)
                    node = new WaitQueueNode(prev, w);
                if (casBarrierStack(node.next = h, node)) {
                    node.awaitSyncRelease(this);
                    break;
                }
            }
            long ec = ensureSync();
            if (ec != prev) {
                w.lastEventCount = ec;
                break;
            }
        }
    }

    /**
     * 从概念上讲，我们在这里需要做的就是在一个线程即将阻塞时添加或恢复一个备用线程
     * （并在以后解除阻塞时删除或暂停它--见 suspendIfSpare）。
     * 然而，实现这个想法需要应对几个问题：我们有关于线程状态的不完美信息。
     * 一些计数的更新可能而且通常会滞后于运行状态的变化，尽管有安排来保持它们的准确性
     * （例如，在可能的情况下，在发出信号或恢复之前更新计数），特别是在动态JVM上运行时，
     * 没有优化更新计数的不频繁的路径。产生过多的线程会使这些问题变得更糟，
     * 因为多余的线程更有可能与其他线程进行上下文交换，使它们都变慢，
     * 特别是在没有工作可用的情况下，所以所有的线程都忙于扫描或空闲。
     * 另外，多余的空闲线程只能在空闲时被暂停或删除，而不是在不需要它们时立即被删除。
     * 因此，增加线程会提高平行度水平，而不是必要的时间。 此外，FJ 应用程序经常遇到高度瞬时的高峰，
     * 当许多线程被阻断加入时，但时间比创建或恢复备用线程所需的时间要短。
     * 创建或恢复备用线程所需的时间。
     */
    final boolean preJoin(Java7ForkJoinTask<?> joinMe, boolean maintainParallelism) {
        maintainParallelism &= maintainsParallelism;
        boolean dec = false;
        while (spareStack != null || !tryResumeSpare(dec)) {
            int counts = workerCounts;
            if (dec || (dec = casWorkerCounts(counts, --counts))) {
                if (!needSpare(counts, maintainParallelism))
                    break;
                if (joinMe.status < 0)
                    return true;
                if (tryAddSpare(counts))
                    break;
            }
        }
        return false;
    }

    /**
     * 减少运行计数；如果太低，则增加备用计数。
     */
    final boolean preBlock(ManagedBlocker blocker, boolean maintainParallelism) {
        maintainParallelism &= maintainsParallelism;
        boolean dec = false;
        while (spareStack == null || !tryResumeSpare(dec)) {
            int counts = workerCounts;
            if (dec || (dec = casWorkerCounts(counts, --counts))) {
                if (!needSpare(counts, maintainParallelism))
                    break;
                if (blocker.isReleasable())
                    return true;
                if (tryAddSpare(counts))
                    break;
            }
        }
        return false;
    }


    /**
     * 如果出现需要一个备用线程，则返回{@code true}。
     * 如果保持并行，当运行线程的赤字超过总线程的盈余，并且显然有一些工作要做，则返回 true。
     * 这个自我限制的规则意味着，已经添加的线程越多，则 在添加另一个线程之前，我们将容忍更少的并行性。
     */
    private boolean needSpare(int counts, boolean maintainParallelism) {
        int ps = parallelism;
        int rc = runningCountOf(counts);
        int tc = totalCountOf(counts);
        int runningDeficit = ps - rc;
        int totalSurplus = tc - ps;
        return (tc < maxPoolSize && (rc == 0 || totalSurplus < 0
                || (maintainParallelism && runningDeficit > totalSurplus && Java7ForkJoinWorkerThread.hasQueuedTasks(workers))));
    }

    private boolean tryAddSpare(int expectedCounts) {
        final ReentrantLock lock = this.workerLock;
        int expectedRunning = runningCountOf(expectedCounts);
        int expectedTotal = totalCountOf(expectedCounts);
        boolean success = false;
        boolean locked = false;
        try {
            for (; ; ) {
                int s = workerCounts;
                int tc = totalCountOf(s);
                int rc = runningCountOf(s);
                if (rc > expectedRunning || tc > expectedTotal)
                    break;
                if (!locked && !(locked = lock.tryLock()))
                    break;
                if (casWorkerCounts(s, workerCountsFor(tc + 1, rc + 1))) {
                    createAndStartSpare(tc);
                    success = true;
                    break;
                }
            }
        } finally {
            if (locked)
                lock.unlock();
        }
        return success;
    }

    /**
     * 添加第 k 个备用工。在进入时，池计数已经被调整以反映增加的情况。
     */
    private void createAndStartSpare(int k) {
        Java7ForkJoinWorkerThread w = null;
        Java7ForkJoinWorkerThread[] ws = ensureWorkerArrayCapacity(k + 1);
        int len = ws.length;
        // Probably, we can place at slot k. If not, find empty slot
        if (k < len && ws[k] != null) {
            for (k = 0; k < len && ws[k] != null; ++k) ;
        }
        if (k < len && isProcessingTasks() && (w = createWorker(k)) != null) {
            ws[k] = w;
            w.start();
        } else
            updateWorkerCount(-1);
        signalIdleWorkers();
    }

    /**
     * 如果有多余的线程，则暂停调用线程 w。 只从同步中调用。
     * 备用程序在 Treiber 堆栈中使用与障碍相同的 WaitQueueNodes 进行排队。
     * 它们主要在 preJoin 中被恢复，但也会在需要所有线程检查运行状态的池事件中被唤醒。线程检查运行状态。
     */
    private boolean suspendIfSpare(Java7ForkJoinWorkerThread w) {
        WaitQueueNode node = null;
        int s;
        while (parallelism < runningCountOf(s = workerCounts)) {
            if (node == null)
                node = new WaitQueueNode(0, w);
            if (casWorkerCounts(s, s - 1)) {
                do {
                } while (!casSpareStack(node.next = spareStack, node));
                node.awaitSpareRelease();
                return true;
            }
        }
        return false;
    }

    private boolean tryResumeSpare(boolean updateCount) {
        WaitQueueNode q;
        while ((q = spareStack) != null) {
            if (casSpareStack(q, q.next)) {
                if (updateCount)
                    updateRunningCount(1);
                q.signal();
                return true;
            }
        }
        return false;
    }

    private void stopAllWorkers() {
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            Java7ForkJoinWorkerThread[] ws = workers;
            if (ws != null) {
                for (Java7ForkJoinWorkerThread t : ws) {
                    if (t != null)
                        t.shutdownNow();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void interruptUnterminatedWorkers() {
        final ReentrantLock lock = this.workerLock;
        lock.lock();
        try {
            Java7ForkJoinWorkerThread[] ws = workers;
            if (ws != null) {
                for (Java7ForkJoinWorkerThread t : ws) {
                    if (t != null && !t.isTerminated()) {
                        t.interrupt();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Java7ForkJoinTask<T>> forkJoinTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            forkJoinTasks.add(Java7ForkJoinTask.adapt(task));
        }
        invoke(new InvokeAll<T>(forkJoinTasks));
        @SuppressWarnings({"unchecked", "rawTypes", "all"})
        List<Future<T>> futures = (List<Future<T>>) (List) forkJoinTasks;
        return futures;
    }

    static final class InvokeAll<T> extends Java7RecursiveAction {
        final ArrayList<Java7ForkJoinTask<T>> tasks;

        InvokeAll(ArrayList<Java7ForkJoinTask<T>> tasks) {
            this.tasks = tasks;
        }

        @Override
        protected void compute() {
            try {
                invokeAll(tasks);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 事件屏障的节点，用于管理空闲的线程。 队列节点是基本的 Treiber 堆栈节点，也用于备用堆栈。
     * <p>
     * 事件屏障有一个事件计数和一个等待队列（实际上是一个 Treiber 栈）。
     * 当事件计数递增时，工人就可以开始寻找工作。如果他们没能找到工作，他们可以等待下一次计数。
     * 释放后，线程帮助其他人唤醒 起。
     * <p>
     * 同步事件只发生在足够的情况下，以 保持整体的灵活性。
     * -  提交一个新的任务到池中
     * - 调整工人数组的大小或其他变化
     * - 池终止
     * - 一个工人在一个空队列上推送一个任务
     * <p>
     * 推送任务的情况经常发生，而且与简单的栈推送相比，它足够重，因此需要特殊处理。
     * 如果队列看起来是空的，方法 signalWork 会返回而不推进计数。
     * 这通常会导致竞争，使一些排队的等待者不能被唤醒。
     * 为了避免这种情况，方法 sync（见 syncIsReleasable）中排队的第一个工作者在被排队后重新扫描任务，
     * 如果发现任何任务，就帮助发出信号。
     * 这很好，因为工作者没有更好的事情可做，所以不妨帮助减轻实际工作的线程的开销和争论。
     * 另外，由于任务可用性的事件计数增量是为了保持有效性（而不是为了强制刷新等），
     * 所以在以下情况下，调用者可以提前退出 争夺另一个信号器是可以的。
     */
    static final class WaitQueueNode {
        WaitQueueNode next;
        volatile Java7ForkJoinWorkerThread thread;
        final long count;

        WaitQueueNode(long c, Java7ForkJoinWorkerThread w) {
            count = c;
            thread = w;
        }

        boolean signal() {
            Java7ForkJoinWorkerThread t = thread;
            if (t == null)
                return false;
            thread = null;
            LockSupport.unpark(t);
            return true;
        }

        void awaitSyncRelease(Java7ForkJoinPool p) {
            while (thread != null && !p.syncIsReleasable(this))
                LockSupport.park(this);
        }

        /**
         * 等待恢复为备用。
         */
        void awaitSpareRelease() {
            while (thread != null) {
                if (!Thread.interrupted())
                    LockSupport.park(this);
            }
        }

    }

    private boolean resumeAllSpares() {
        WaitQueueNode q;
        while ((q = spareStack) != null) {
            if (casSpareStack(q, null)) {
                do {
                    updateRunningCount(1);
                    q.signal();
                } while ((q = q.next) != null);
                return true;
            }
        }
        return false;
    }

    private void trimSpares() {
        int surplus = totalCountOf(workerCounts) - parallelism;
        WaitQueueNode q;
        while (surplus > 0 && (q = spareStack) != null) {
            if (casSpareStack(q, null)) {
                do {
                    updateRunningCount(1);
                    Java7ForkJoinWorkerThread w = q.thread;
                    if (w != null && surplus > 0 && runningCountOf(workerCounts) > 0 && w.shutdown())
                        --surplus;
                    q.signal();
                } while ((q = q.next) != null);
            }
        }
    }

    public static void managedBlock(ManagedBlocker blocker, boolean maintainParallelism) throws InterruptedException {
        Thread t = Thread.currentThread();
        Java7ForkJoinPool pool = ((t instanceof Java7ForkJoinWorkerThread) ? ((Java7ForkJoinWorkerThread) t).pool : null);
        if (!blocker.isReleasable()) {
            try {
                if (pool == null || !pool.preBlock(blocker, maintainParallelism)) {
                    awaitBlocker(blocker);
                }
            } finally {
                if (pool != null)
                    pool.updateRunningCount(1);
            }
        }
    }

    private static void awaitBlocker(ManagedBlocker blocker) throws InterruptedException {
        do {
        } while (!blocker.isReleasable() && !blocker.block());
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return (RunnableFuture<T>) Java7ForkJoinTask.adapt(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return (RunnableFuture<T>) Java7ForkJoinTask.adapt(runnable, value);
    }

    interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
    }


    private boolean casWorkerCounts(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, workerCountOffset, cmp, val);
    }

    private boolean casEventCount(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, eventCountOffset, cmp, val);
    }

    private boolean casSpareStack(WaitQueueNode cmp, WaitQueueNode val) {
        return UNSAFE.compareAndSwapObject(this, spareStackOffset, cmp, val);
    }

    private boolean casBarrierStack(WaitQueueNode cmp, WaitQueueNode val) {
        return UNSAFE.compareAndSwapObject(this, syncStackOffset, cmp, val);
    }

    private boolean casRunControl(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, runControlOffset, cmp, val);
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long eventCountOffset;
    private static final long workerCountOffset;
    private static final long runControlOffset;
    private static final long syncStackOffset;
    private static final long spareStackOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            eventCountOffset = UNSAFE.objectFieldOffset(Java7ForkJoinPool.class.getDeclaredField("eventCount"));
            workerCountOffset = UNSAFE.objectFieldOffset(Java7ForkJoinPool.class.getDeclaredField("workerCounts"));
            runControlOffset = UNSAFE.objectFieldOffset(Java7ForkJoinPool.class.getDeclaredField("runControl"));
            syncStackOffset = UNSAFE.objectFieldOffset(Java7ForkJoinPool.class.getDeclaredField("syncStack"));
            spareStackOffset = UNSAFE.objectFieldOffset(Java7ForkJoinPool.class.getDeclaredField("spareStack"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }

    }
}








