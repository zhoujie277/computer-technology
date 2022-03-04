package com.future.concurrent.history.javaold;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RejectedExecutionException;

public class Java7ForkJoinWorkerThread extends Thread {

    /**
     * 初始化时窃取队列的容量。必须是 2 的幂。初始大小必须至少为 2，但会被填充以减少缓存影响。
     */
    private static final int INITIAL_QUEUE_CAPACITY = 1 << 13;

    /**
     * 窃取队列的最大尺寸。 必须小于或等于 1 << 28，以确保没有索引绕行。
     * (这比通常的界限要小，因为我们需要左移 3 是在 int 范围内）。
     */
    private static final int MAXIMUM_QUEUE_CAPACITY = 1 << 28;

    final Java7ForkJoinPool pool;

    /**
     * 窃取队列数组。大小必须是2的幂。在线程启动时被初始化，以提高内存定位性。
     */
    private Java7ForkJoinTask<?>[] queue;


    /**
     * 下一个队列槽的索引（mod queue.length），以推送到或弹出。
     * 它只由所有者线程通过有序存储写入。
     * sp 和 base 都允许在溢出时环绕，但（sp - base）仍然估计大小。
     */
    private volatile int sp;

    /**
     * 最小有效队列槽的索引（mod queue.length），它是 始终是下一个要窃取的位置，如果不是空的。
     */
    private volatile int base;

    /**
     * 活动状态。当为真时，这个 worker 被认为是活跃的。构造时必须为假。
     * 在执行任务时，以及在窃取任务之前，它必须为真。在调用 pool.sync 之前，它必须为假。
     */
    private boolean active;

    /**
     * 该工作者的运行状态。支持通常的简单版本的 shutdown/shutdownNow控制。
     */
    private volatile int runState;

    /**
     * 用于选择偷窃受害者的随机数发生器的种子。使用 Marsaglia xorshift。初始化时必须为非零。
     */
    private int seed;

    /**
     * 窃取次数，空闲时转入池中
     */
    private int stealCount;

    /**
     * 该工作者在池数组中的索引。在运行前由池设置一次，并在清理等过程中由池直接访问。
     */
    int poolIndex;

    /**
     * 最后等待的障碍事件。在池的回调中被访问 方法中访问，但只能由当前线程访问。
     */
    long lastEventCount;

    /**
     * 如果使用本地 fifo，而不是默认的 lifo，用于本地轮询，则为真。
     */
    private boolean locallyFifo;

    /**
     * 创建一个在给定池中运行的ForkJoinWorkerThread。
     */
    public Java7ForkJoinWorkerThread(Java7ForkJoinPool pool) {
        if (pool == null) throw new NullPointerException();
        this.pool = pool;
    }

    public Java7ForkJoinPool getPool() {
        return pool;
    }

    /**
     * 返回该线程在其池中的索引号。
     * 返回值的范围是从零到池中曾经创建过的最大线程数（减去 1）。
     * 这个方法可能对跟踪状态或收集结果的应用程序很有用。
     * 收集结果的应用程序来说是非常有用的。
     */
    public int getPoolIndex() {
        return poolIndex;
    }

    /**
     * 建立本地先入先出的调度模式，用于 fork 的任务的本地先入先出调度模式，这些任务从未被加入。
     */
    void setAsyncMode(boolean async) {
        locallyFifo = async;
    }

    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int TERMINATING = 2;
    private static final int TERMINATED = 3;

    final boolean isShutdown() {
        return runState >= SHUTDOWN;
    }

    final boolean isTerminating() {
        return runState >= TERMINATING;
    }

    final boolean isTerminated() {
        return runState == TERMINATED;
    }

    final boolean shutdown() {
        return transitionRunStateTo(SHUTDOWN);
    }

    final boolean shutdownNow() {
        return transitionRunStateTo(TERMINATING);
    }

    private boolean transitionRunStateTo(int state) {
        for (; ; ) {
            int s = runState;
            if (s >= state)
                return false;
            if (U.compareAndSwapInt(this, runStateOffset, s, state))
                return true;
        }
    }

    /**
     * 试图将状态设置为不活动；在争论中失败。
     */
    private boolean tryActivate() {
        if (!active) {
            if (!pool.tryIncrementActiveCount())
                return false;
            active = false;
        }
        return true;
    }

    private boolean tryInactivate() {
        if (active) {
            if (!pool.tryDecrementActiveCount())
                return false;
            active = false;
        }
        return true;
    }

    /**
     * 计算随机受害者探针的下一个值。
     * 扫描不需要一个非常高质量的发生器，
     * 但也不需要一个蹩脚的一个。
     * Marsaglia xor-shift很便宜而且效果很好。
     */
    private static int xorShift(int r) {
        r ^= (r << 13);
        r ^= (r >>> 17);
        return r ^ (r << 5);
    }

    final int getAndClearStealCount() {
        int sc = stealCount;
        stealCount = 0;
        return sc;
    }

    /**
     * 返回队列中任务数量的估计值。
     */
    final int getQueueSize() {
        // suppress momentarily negative values
        return Math.max(0, sp - base);
    }

    @Override
    public void run() {
        Throwable exception = null;
        try {
            onStart();
            pool.sync(this);
            mainLoop();
        } catch (Throwable ex) {
            exception = ex;
        } finally {
            onTermination(exception);
        }
    }

    private void mainLoop() {
        while (!isShutdown()) {
            Java7ForkJoinTask<?> t = pollTask();
            if (t != null || (t = pollSubmission()) != null)
                t.quietlyExec();
            else if (tryInactivate())
                pool.sync(this);
        }
    }

    /**
     * 在构造之后但在处理任何任务之前初始化内部状态。
     * 如果你覆盖了这个方法，
     * 你必须在该方法的开头调用 super.onStart()。初始化需要注意。
     * 大多数字段必须有合法的默认值，以确保在本线程开始处理任务之前，来自其他线程的尝试性访问就能正常进行。
     * 处理任务之前就能正常工作。
     */
    protected void onStart() {
        queue = new Java7ForkJoinTask<?>[INITIAL_QUEUE_CAPACITY];
        int p = poolIndex + 1;
        seed = p + (p << 8) + (p << 16) + (p << 24);
    }

    /**
     * 执行与该工作线程的终止相关的清理工作。
     * 如果你覆盖了这个方法，你必须在覆盖方法的末尾调用 {@code super.onTermination}。
     */
    protected void onTermination(Throwable exception) {
        while (exception == null && pool.isProcessingTasks() && base != sp) {
            try {
                Java7ForkJoinTask<?> t = popTask();
                if (t != null)
                    t.quietlyExec();
            } catch (Throwable ex) {
                exception = ex;
            }
        }

        try {
            do {
            } while (!tryInactivate());
            cancelTasks();
            runState = TERMINATED;
            pool.workerTerminated(this);
        } catch (Throwable ex) {
            if (exception == null)
                exception = ex;
        } finally {
            if (exception != null)
                Java7ForkJoinTask.rethrowException(exception);
        }
    }

    final void cancelTasks() {
        Java7ForkJoinTask<?> t;
        while (base != sp && (t = deqTask()) != null)
            t.cancelIgnoringExceptions();
    }

    final int drainTasksTo(Collection<? super Java7ForkJoinTask<?>> c) {
        int n = 0;
        Java7ForkJoinTask<?> t;
        while (base != sp && (t = deqTask()) != null) {
            c.add(t);
            ++n;
        }
        return n;
    }

    final void pushTask(Java7ForkJoinTask<?> t) {
        Java7ForkJoinTask<?>[] q = queue;
        int mask = q.length - 1;
        int s = sp;
        setSlot(q, s & mask, t);
        storeSp(++s);
        if ((s -= base) == 1)
            pool.signalWork();
        else if (s >= mask)
            growQueue();
    }

    /**
     * 如果在给定的数组中至少有一个工作者，返回 {@code true}。似乎至少有一个排队的任务。
     */
    static boolean hasQueuedTasks(Java7ForkJoinWorkerThread[] ws) {
        if (ws != null) {
            int len = ws.length;
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < len; i++) {
                    Java7ForkJoinWorkerThread w = ws[i];
                    if (w != null && w.sp != w.base)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回任务数的估计值，由闲置工人数的函数抵消。
     */
    final int getEstimatedSurplusTaskCount() {
        return (sp - base) - (pool.getIdleThreadCount() >>> 1);
    }

    final Java7ForkJoinTask<?> scanWhileJoining(Java7ForkJoinTask<?> joinMe) {
        Java7ForkJoinTask<?> t = pollTask();
        if (t != null && joinMe.status < 0 && sp == base) {
            pushTask(t);
            t = null;
        }
        return t;
    }

    final void helpQuiescePool() {
        for (; ; ) {
            Java7ForkJoinTask<?> t = pollTask();
            if (t != null)
                t.quietlyExec();
            else if (tryInactivate() && pool.isQuiescent())
                break;
        }
        do {
        } while (!tryActivate());
    }

    final Java7ForkJoinTask<?> pollTask() {
        Java7ForkJoinTask<?> t = locallyFifo ? locallyDeqTask() : popTask();
        if (t == null && (t = scan()) != null)
            ++stealCount;
        return t;
    }

    final Java7ForkJoinTask<?> pollLocalTask() {
        return locallyFifo ? locallyDeqTask() : popTask();
    }

    private Java7ForkJoinTask<?> pollSubmission() {
        Java7ForkJoinPool p = pool;
        while (p.hasQueuedSubmissions()) {
            Java7ForkJoinTask<?> t;
            if (tryActivate() && (t = p.pollSubmission()) != null)
                return t;
        }
        return null;
    }

    final Java7ForkJoinTask<?> popTask() {
        int s = sp;
        while (s != base) {
            if (tryActivate()) {
                Java7ForkJoinTask<?>[] q = queue;
                int mask = q.length - 1;
                int i = (s - 1) & mask;
                Java7ForkJoinTask<?> t = q[i];
                if (t == null || !casSlotNull(q, i, t))
                    break;
                storeSp(s - 1);
                return t;
            }
        }
        return null;
    }

    final Java7ForkJoinTask<?> peekTask() {
        Java7ForkJoinTask<?>[] q = queue;
        if (q == null)
            return null;
        int mask = q.length - 1;
        int i = locallyFifo ? base : (sp - 1);
        return q[i & mask];
    }

    /**
     * popTask 的专门版本，只在最上面的元素是给定的任务时弹出。只在活动时由当前线程调用。
     */
    final boolean unpushTask(Java7ForkJoinTask<?> t) {
        Java7ForkJoinTask<?>[] q = queue;
        int mask = q.length - 1;
        int s = sp - 1;
        if (casSlotNull(q, s & mask, t)) {
            storeSp(s);
            return true;
        }
        return false;
    }

    /**
     * 试图从自己的队列中抽取一个任务，必要时激活，只有在空时才失败。仅由当前线程调用。
     */
    final Java7ForkJoinTask<?> locallyDeqTask() {
        int b;
        while (sp != (b = base)) {
            if (tryActivate()) {
                Java7ForkJoinTask<?>[] q = queue;
                int i = (q.length - 1) & b;
                Java7ForkJoinTask<?> t = q[i];
                if (t != null && casSlotNull(q, i, t)) {
                    base = b + 1;
                    return t;
                }
            }
        }
        return null;
    }

    final Java7ForkJoinTask<?> deqTask() {
        Java7ForkJoinTask<?> t;
        Java7ForkJoinTask<?>[] q;
        int i, b;
        if (sp != (b = base) && (q = queue) != null && (t = q[i = (q.length - 1) & b]) != null && casSlotNull(q, i, t)) {
            base = b + 1;
            return t;
        }
        return null;
    }

    /**
     * 使队列大小翻倍。通过模拟从旧数组中窃取（deqs）并将最旧的放在新数组中，来转移元素。
     */
    private void growQueue() {
        Java7ForkJoinTask<?>[] oldQ = queue;
        int oldSize = oldQ.length;
        int newSize = oldSize << 1;
        if (newSize > MAXIMUM_QUEUE_CAPACITY)
            throw new RejectedExecutionException("Queue capacity exceeded");
        Java7ForkJoinTask<?>[] newQ = queue = new Java7ForkJoinTask[newSize];

        int b = base;
        int bf = b + oldSize;
        int oldMask = oldSize - 1;
        int newMask = newSize - 1;
        do {
            int oldIndex = b & oldMask;
            Java7ForkJoinTask<?> t = oldQ[oldIndex];
            if (t != null && !casSlotNull(oldQ, oldIndex, t))
                t = null;
            setSlot(newQ, b & newMask, t);
        } while (++b != bf);
        pool.signalWork();
    }

    /**
     * 试图从另一个工人那里偷一个任务。
     * 从工作者数组的一个随机索引开始，探测工作者，直到找到一个有非空队列的工作者或发现所有工作者都是空的。
     * 它随机选择前 N 个探测点。如果这些都是空的，它就进行全面的循环遍历，这对于调用者准确设置活动状态是必要的。
     * 如果上次扫描后发生了池子事件，它也会重新启动，这就迫使刷新了 工人数组的刷新，以防障碍与调整大小有关。
     * <p>
     * 这种方法必须既快又安静 -- 通常避免可能破坏缓存共享的内存访问，等等，
     * 除了检查和接受任务所需的访问。除其他事项外，这还包括就地更新随机种子，而不将其存储到退出。
     * 储存它直到退出。
     */
    private Java7ForkJoinTask<?> scan() {
        Java7ForkJoinTask<?> t = null;
        int r = seed;
        Java7ForkJoinWorkerThread[] ws;
        int mask;
        outer:
        do {
            if ((ws = pool.workers) != null && (mask = ws.length - 1) > 0) {
                int idx = r;
                int probes = ~mask;     // use random index while negative
                for (; ; ) {
                    r = xorShift(r);     // update random seed
                    Java7ForkJoinWorkerThread v = ws[mask & idx];
                    if (v == null || v.sp == v.base) {
                        if (probes <= mask)
                            idx = (probes++ < 0) ? r : (idx + 1);
                        else
                            break;
                    } else if (!tryActivate() || (t = v.deqTask()) == null) {
                        continue outer;
                    } else
                        break outer;
                }
            }
        } while (pool.hasNewSyncEvent(this));
        seed = r;
        return t;
    }

    private static void setSlot(Java7ForkJoinTask<?>[] q, int i, Java7ForkJoinTask<?> t) {
        U.putOrderedObject(q, slotOffset(i), t);
    }

    private static boolean casSlotNull(Java7ForkJoinTask<?>[] q, int i, Java7ForkJoinTask<?> t) {
        return U.compareAndSwapObject(q, slotOffset(i), t, null);
    }

    private void storeSp(int s) {
        U.putOrderedInt(this, spOffset, s);
    }

    private static long slotOffset(int i) {
        return ((long) i << qShift) + qBase;
    }

    private static final sun.misc.Unsafe U;
    private static final long spOffset;
    private static final long runStateOffset;
    private static final long qBase;
    private static final int qShift;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
            qBase = U.arrayBaseOffset(ForkJoinTask[].class);
            int s = U.arrayIndexScale(ForkJoinTask[].class);
            if ((s & (s - 1)) != 0)
                throw new Error("data type scale not a power of two");
            qShift = 31 - Integer.numberOfLeadingZeros(s);
            spOffset = U.objectFieldOffset(Java7ForkJoinWorkerThread.class.getDeclaredField("sp"));
            runStateOffset = U.objectFieldOffset(Java7ForkJoinWorkerThread.class.getDeclaredField("runState"));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new Error(e);
        }
    }
}
















