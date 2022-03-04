package com.future.concurrent.history.javaold;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

public abstract class Java7ForkJoinTask<V> implements Future<V> {

    /**
     * 运行控制状态位被打包成一个 int，以减少占用空间，并确保原子性（通过 CAS）。
     * status 最初为零，在完成之前为非负值，在完成之后，状态为 COMPLETED。CANCELLED，或 EXCEPTIONAL，使用前 3 位。
     * 被其他线程阻塞等待的任务有 SIGNAL_MASK 位设置--第 15 位用于外部（非 FJ）的等待，其余的是等待的 FJ 线程的计数。
     * (这种表示方法依赖于 ForkJoinPool 的最大线程限制）。
     * 完成一个设置了 SIGNAL_MASK 位的被盗任务会通过 notifyAll 唤醒等待者。
     * 尽管对于某些目的来说是次优的，但我们使用基本的内置等待/通知来利用 JVM 中的 "监视器膨胀"，
     * 否则我们就需要模拟，以避免进一步增加每个任务的记账开销。
     * 请注意，第 16-28 位目前是未使用的。另外，值 0x80000000 可以作为备用的 完成值。
     */

    volatile int status;

    static final int COMPLETION_MASK = 0xe0000000;
    static final int NORMAL = 0xe0000000;
    static final int CANCELLED = 0xc0000000;
    static final int EXCEPTIONAL = 0xa0000000;
    static final int SIGNAL_MASK = 0x0000ffff;
    static final int INTERNAL_SIGNAL_MASK = 0x00007fff;
    static final int EXTERNAL_SIGNAL = 0x00008000;


    static final Map<Java7ForkJoinTask<?>, Throwable> exceptionMap = Collections.synchronizedMap((new WeakHashMap<>()));

    final void setCompletion(int completion) {
        Java7ForkJoinPool pool = getPool();
        if (pool != null) {
            int s;
            do {
            } while ((s = status) >= 0 && !casStatus(s, completion));
            if ((s & SIGNAL_MASK) != 0) {
                if ((s &= INTERNAL_SIGNAL_MASK) != 0) {
                    pool.updateRunningCount(s);
                }
                synchronized (this) {
                    notifyAll();
                }
            }
        } else
            externallySetCompletion(completion);
    }

    /**
     * setCompletion的版本，用于非 FJ 线程。 留下了信号位，供未受阻的线程调整，并且总是通知。
     */
    private void externallySetCompletion(int completion) {
        int s;
        do {
        } while ((s = status) >= 0 && !casStatus(s, (s & SIGNAL_MASK) | completion));
        synchronized (this) {
            notifyAll();
        }
    }

    final void setNormalCompletion() {
        if (!UNSAFE.compareAndSwapInt(this, statusOffset, 0, NORMAL))
            setCompletion(NORMAL);
    }

    private void doAwaitDone() {
        try {
            while (status >= 0)
                synchronized (this) {
                    if (status >= 0) wait();
                }
        } catch (InterruptedException e) {
            onInterruptedWait();
        }
    }

    private void doAwaitDone(long startTime, long nanos) {
        synchronized (this) {
            try {
                while (status >= 0) {
                    long nt = nanos - (System.nanoTime() - startTime);
                    if (nt <= 0)
                        break;
                    wait(nt / 1000000, (int) (nt % 1000000));
                }
            } catch (InterruptedException ie) {
                onInterruptedWait();
            }
        }
    }

    private int awaitDone(Java7ForkJoinWorkerThread w, boolean maintainParallelism) {
        Java7ForkJoinPool pool = (w == null) ? null : w.pool;
        int s;
        while ((s = status) >= 0) {
            if (casStatus(s, (pool == null) ? s | EXTERNAL_SIGNAL : s + 1)) {
                if (pool == null || !pool.preJoin(this, maintainParallelism))
                    doAwaitDone();
                if (((s = status) & INTERNAL_SIGNAL_MASK) != 0)
                    adjustPoolCountsOnUnblock(pool);
                break;
            }
        }
        return s;
    }

    private int awaitDone(Java7ForkJoinWorkerThread w, long nanos) {
        Java7ForkJoinPool pool = (w == null) ? null : w.pool;
        int s;
        while ((s = status) >= 0) {
            if (casStatus(s, (pool == null) ? s | EXTERNAL_SIGNAL : s + 1)) {
                long startTime = System.nanoTime();
                if (pool == null || !pool.preJoin(this, false))
                    doAwaitDone(startTime, nanos);
                if ((s = status) >= 0) {
                    adjustPoolCountsOnCancelledWait(pool);
                    s = status;
                }
                if (s < 0 && (s & INTERNAL_SIGNAL_MASK) != 0)
                    adjustPoolCountsOnUnblock(pool);
                break;
            }
        }
        return s;
    }

    private void adjustPoolCountsOnUnblock(Java7ForkJoinPool pool) {
        int s;
        do {
        } while ((s = status) < 0 && !casStatus(s, s & COMPLETION_MASK));
        if (pool != null && (s &= INTERNAL_SIGNAL_MASK) != 0)
            pool.updateRunningCount(s);
    }

    /**
     * Notifies pool to adjust counts on cancelled or timed out wait.
     */
    private void adjustPoolCountsOnCancelledWait(Java7ForkJoinPool pool) {
        if (pool != null) {
            int s;
            while ((s = status) >= 0 && (s & INTERNAL_SIGNAL_MASK) != 0) {
                if (casStatus(s, s - 1)) {
                    pool.updateRunningCount(1);
                    break;
                }
            }
        }
    }

    private void onInterruptedWait() {
        Java7ForkJoinWorkerThread w = getWorker();
        if (w == null)
            Thread.currentThread().interrupt();
        else if (w.isTerminating())
            cancelIgnoringExceptions();
    }

    private void setDoneExceptionally(Throwable rex) {
        exceptionMap.put(this, rex);
        setCompletion(EXCEPTIONAL);
    }

    private void reportException(int s) {
        if ((s &= COMPLETION_MASK) < NORMAL) {
            if (s == CANCELLED)
                throw new CancellationException();
            else
                rethrowException(exceptionMap.get(this));
        }
    }

    private V reportFutureResult() throws InterruptedException, ExecutionException {
        if (Thread.interrupted())
            throw new InterruptedException();
        int s = status & COMPLETION_MASK;
        if (s < NORMAL) {
            Throwable ex;
            if (s == CANCELLED)
                throw new CancellationException();
            if (s == EXCEPTIONAL && (ex = exceptionMap.get(this)) != null) {
                throw new ExecutionException(ex);
            }
        }
        return getRawResult();
    }

    private V reportTimedFutureResult() throws InterruptedException, ExecutionException, TimeoutException {
        if (Thread.interrupted())
            throw new InterruptedException();
        Throwable ex;
        int s = status & COMPLETION_MASK;
        if (s == NORMAL)
            return getRawResult();
        else if (s == CANCELLED)
            throw new CancellationException();
        else if (s == EXCEPTIONAL && (ex = exceptionMap.get(this)) != null)
            throw new ExecutionException(ex);
        else
            throw new TimeoutException();
    }

    private boolean tryExec() {
        try {
            if (!exec())
                return false;
        } catch (Throwable rex) {
            setDoneExceptionally(rex);
            rethrowException(rex);
            return false;
        }
        setNormalCompletion();
        return true;
    }

    final void quietlyExec() {
        if (status >= 0) {
            try {
                if (!exec())
                    return;
            } catch (Throwable rex) {
                setDoneExceptionally(rex);
                return;
            }
            setNormalCompletion();
        }
    }

    private boolean tryQuietlyInvoke() {
        try {
            if (!exec())
                return false;
        } catch (Throwable rex) {
            setDoneExceptionally(rex);
            return false;
        }
        setNormalCompletion();
        return true;
    }

    final void cancelIgnoringExceptions() {
        try {
            cancel(false);
        } catch (Throwable ignore) {
        }
    }

    private int busyJoin(Java7ForkJoinWorkerThread w) {
        int s;
        Java7ForkJoinTask<?> t;
        while ((s = status) >= 0 && (t = w.scanWhileJoining(this)) != null)
            t.quietlyExec();
        return (s >= 0) ? awaitDone(w, false) : s;
    }

    /**
     * 安排异步地执行这个任务。
     * 虽然不一定强制执行，但一个任务分叉一次以上是一个使用错误，除非它已经完成并被重新初始化。
     * 对该任务的状态或其操作的任何数据的后续修改，除了执行该任务的线程外，
     * 其他线程不一定能持续观察到，除非之前调用了{@link #join}或相关方法，
     * 或者调用了{@link #isDone}返回{@code true}。
     * 这个方法只能在{@code Java7ForkJoinTask} 的计算中被调用
     * （可以使用方法{@link Java7ForkJoinPool} 确定）。
     * 试图在其他情况下调用会导致异常或错误，可能包括 {@code ClassCastException}。
     */
    public final Java7ForkJoinTask<V> fork() {
        ((Java7ForkJoinWorkerThread) Thread.currentThread()).pushTask(this);
        return this;
    }

    /**
     * 当计算 {@link #isDone is done} 完成时，返回计算的结果。这个方法与 {@link #get()} 不同的是，
     * 异常的完成会导致 {@code RuntimeException} 或 {@code Error}，
     * 而不是 {@code ExecutionException}。
     */
    public final V join() {
        Java7ForkJoinWorkerThread w = getWorker();
        if (w == null || status < 0 || !w.unpushTask(this) || !tryExec())
            reportException(awaitDone(w, true));
        return getRawResult();
    }

    public final V invoke() {
        if (status >= 0 && tryExec())
            return getRawResult();
        return join();
    }

    /**
     * 在给定的任务中，当 {@code isDone} 在每个任务中成立或遇到（未检查的）异常时返回，
     * 在这种情况下，异常被重新抛出。 如果其中一个任务遇到了异常，另一个任务可能会被取消，但不保证会被取消。
     * 如果两个任务都出现了异常，那么这个方法就会抛出其中一个。
     * 每个任务的单独状态可以 使用 getException() 和相关方法检查每个任务的状态。
     * <p>
     * 他的方法只能在 {@code ForkJoinTask}的计算中被调用
     * （可以使用方法{@link Java7ForkJoinPool}确定）。
     * 试图在其他情况下调用会导致异常或错误，可能包括 {@code ClassCastException}。
     */
    public static void invokeAll(Java7ForkJoinTask<?> t1, Java7ForkJoinTask<?> t2) {
        t2.fork();
        t1.invoke();
        t2.join();
    }

    public static void invokeAll(Java7ForkJoinTask<?>... tasks) {
        Throwable ex = null;
        int last = tasks.length - 1;
        for (int i = last; i >= 0; i--) {
            Java7ForkJoinTask<?> t = tasks[i];
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            } else if (i != 0) {
                t.fork();
            } else {
                t.quietlyInvoke();
                if (ex == null)
                    ex = t.getException();
            }
        }
        for (int i = 1; i <= last; i++) {
            Java7ForkJoinTask<?> t = tasks[i];
            if (t != null) {
                if (ex != null)
                    t.cancel(false);
                else {
                    t.quietlyJoin();
                    if (ex == null)
                        ex = t.getException();
                }
            }
        }
        if (ex != null)
            rethrowException(ex);
    }

    public static <T extends Java7ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if (!(tasks instanceof RandomAccess) || !(tasks instanceof List<?>)) {
            invokeAll(tasks.toArray(new Java7ForkJoinTask<?>[tasks.size()]));
            return tasks;
        }
        List<? extends Java7ForkJoinTask<?>> ts =
                (List<? extends Java7ForkJoinTask<?>>) tasks;
        Throwable ex = null;
        int last = ts.size() - 1;
        for (int i = last; i >= 0; --i) {
            Java7ForkJoinTask<?> t = ts.get(i);
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            } else if (i != 0)
                t.fork();
            else {
                t.quietlyInvoke();
                if (ex == null)
                    ex = t.getException();
            }
        }
        for (int i = 1; i <= last; ++i) {
            Java7ForkJoinTask<?> t = ts.get(i);
            if (t != null) {
                if (ex != null)
                    t.cancel(false);
                else {
                    t.quietlyJoin();
                    if (ex == null)
                        ex = t.getException();
                }
            }
        }
        if (ex != null)
            rethrowException(ex);
        return tasks;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        setCompletion(CANCELLED);
        return (status & COMPLETION_MASK) == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status < 0;
    }

    @Override
    public boolean isCancelled() {
        return (status & COMPLETION_MASK) == CANCELLED;
    }

    public final boolean isCompleteAbnormally() {
        return (status & COMPLETION_MASK) < NORMAL;
    }

    public final boolean isCompletedNormally() {
        return (status & COMPLETION_MASK) == NORMAL;
    }

    public void completeExceptionally(Throwable ex) {
        setDoneExceptionally((ex instanceof RuntimeException) || (ex instanceof Error) ? ex : new RuntimeException(ex));
    }

    public void complete(V value) {
        try {
            setRawResult(value);
        } catch (Throwable rex) {
            setDoneExceptionally(rex);
            return;
        }
        setNormalCompletion();
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        Java7ForkJoinWorkerThread w = getWorker();
        if (w == null || status < 0 || !w.unpushTask(this) || !tryQuietlyInvoke())
            awaitDone(w, true);
        return reportFutureResult();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        Java7ForkJoinWorkerThread w = getWorker();
        if (w == null || status < 0 || !w.unpushTask(this) || !tryQuietlyInvoke())
            awaitDone(w, nanos);
        return reportTimedFutureResult();
    }

    /**
     * 可能会执行其他任务，直到这个任务 {@link #isDone is done}，然后返回计算的结果。
     * 这种方法可能比 {@code join} 更有效，但只适用于当前任务的继续执行与任何其他任务的继续执行之间没有潜在的依赖关系的情况下。
     * (这通常适用于纯粹的 分割和征服任务）。
     */
    public final V helpJoin() {
        Java7ForkJoinWorkerThread w = (Java7ForkJoinWorkerThread) Thread.currentThread();
        if (status < 0 || w.unpushTask(this) || !tryExec())
            reportException(busyJoin(w));
        return getRawResult();
    }

    public final void quietlyHelpJoin() {
        if (status > 0) {
            Java7ForkJoinWorkerThread w = (Java7ForkJoinWorkerThread) Thread.currentThread();
            if (!w.unpushTask(this) || !tryQuietlyInvoke())
                busyJoin(w);
        }
    }

    public final void quietlyJoin() {
        if (status > 0) {
            Java7ForkJoinWorkerThread w = getWorker();
            if (w == null || !w.unpushTask(this) || !tryQuietlyInvoke())
                awaitDone(w, true);
        }
    }

    /**
     * 开始执行这个任务，如果需要的话，等待其完成，但不返回其结果或抛出一个异常。
     * 这个方法在处理任务集合时可能很有用，因为有些任务已经被取消或以其他方式中止了。
     * 在处理任务集合时，当一些任务被取消或以其他方式被告知已经中止时，这个方法可能很有用。
     */
    public final void quietlyInvoke() {
        if (status >= 0 && !tryQuietlyInvoke())
            quietlyJoin();
    }

    public static void helpQuiesce() {
        ((Java7ForkJoinWorkerThread) Thread.currentThread()).helpQuiescePool();
    }

    public void reinitialize() {
        if ((status & COMPLETION_MASK) == EXCEPTIONAL)
            exceptionMap.remove(this);
        status = 0;
    }

    /**
     * 试图取消这个任务的执行计划。如果这个任务是当前线程最近分叉的任务，
     * 并且没有在其他线程中开始执行，这个方法通常会成功。
     * 这个方法在安排替代性的本地任务处理时可能很有用，这些任务本可以被窃取，但是 但没有被窃取。
     * <p>
     * 这个方法只能在{@code ForkJoinTask}的计算中被调用
     * （可以使用方法{@link #inForkJoinPool}确定）。
     * 试图在其他情况下调用会导致异常或错误，可能包括{@code ClassCastException}。
     */
    public boolean tryUnfork() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).unpushTask(this);
    }

    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof Java7ForkJoinWorkerThread;
    }

    public static int getQueuedTaskCount() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).getQueueSize();
    }

    public static int getSurplusQueuedTaskCount() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).getEstimatedSurplusTaskCount();
    }

    public final Throwable getException() {
        int s = status & COMPLETION_MASK;
        return ((s >= NORMAL) ? null : (s == CANCELLED) ? new CancellationException() : exceptionMap.get(this));
    }

    protected static Java7ForkJoinTask<?> peekNextLocalTask() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).peekTask();
    }

    protected static Java7ForkJoinTask<?> pollNextLocalTask() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).pollLocalTask();
    }

    protected static Java7ForkJoinTask<?> pollTask() {
        return ((Java7ForkJoinWorkerThread) Thread.currentThread()).popTask();
    }

    public static Java7ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        return (t instanceof Java7ForkJoinWorkerThread) ? ((Java7ForkJoinWorkerThread) t).pool : null;
    }

    static Java7ForkJoinWorkerThread getWorker() {
        Thread t = Thread.currentThread();
        return (t instanceof Java7ForkJoinWorkerThread) ? (Java7ForkJoinWorkerThread) t : null;
    }

    static void rethrowException(Throwable ex) {
        if (ex != null)
            UNSAFE.throwException(ex);
    }

    final boolean casStatus(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, statusOffset, cmp, val);
    }

    public abstract V getRawResult();

    protected abstract void setRawResult(V value);

    protected abstract boolean exec();

    private static final sun.misc.Unsafe UNSAFE;
    private static final long statusOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            statusOffset = UNSAFE.objectFieldOffset(Java7ForkJoinTask.class.getDeclaredField("status"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static <T> Java7ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable<T>(callable);
    }

    public static Java7ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnable<Void>(runnable, null);
    }

    public static <T> Java7ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable<>(runnable, result);
    }

    static final class AdaptedRunnable<T> extends Java7ForkJoinTask<T> implements RunnableFuture<T> {
        final Runnable runnable;
        T result;
        final T resultOnCompletion;

        AdaptedRunnable(Runnable runnable, T result) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
            this.resultOnCompletion = result;
        }

        public final T getRawResult() {
            return result;
        }

        public final void setRawResult(T v) {
            result = v;
        }

        public final boolean exec() {
            runnable.run();
            result = resultOnCompletion;
            return true;
        }

        @Override
        public void run() {
            invoke();
        }
    }

    static final class AdaptedCallable<T> extends Java7ForkJoinTask<T> implements RunnableFuture<T> {

        final Callable<? extends T> callable;
        T result;

        AdaptedCallable(Callable<? extends T> callable) {
            if (callable == null) throw new NullPointerException();
            this.callable = callable;
        }

        @Override
        public T getRawResult() {
            return result;
        }

        @Override
        protected void setRawResult(T value) {
            result = value;
        }

        @Override
        protected boolean exec() {
            try {
                result = callable.call();
                return true;
            } catch (Error err) {
                throw err;
            } catch (RuntimeException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void run() {
            invoke();
        }
    }
}





















