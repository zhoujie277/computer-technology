package com.future.concurrent.java8;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 一个可取消的异步计算。
 * 这个类提供了一个 Future 的基础实现，它的方法可以启动和取消计算，查询计算是否完成，以及检索计算的结果。
 * 只有在计算完成后才能检索到结果；如果计算尚未完成，get 方法将被阻塞。
 * 一旦计算完成，计算就不能重启或取消（除非计算是用 runAndReset 调用的）。
 * FutureTask 可以用来包装一个 Callable 或 Runnable 对象。
 * 因为 FutureTask 实现了 Runnable，所以 FutureTask 可以被提交给一个执行器执行。
 * 除了作为一个独立的类之外，这个类还提供了受保护的功能，在创建自定义的任务类时可能很有用。
 */
@SuppressWarnings("all")
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * 修订说明。这与该类以前的版本不同，
     * 它依赖于 AbstractQueuedSynchronizer，主要是为了避免在取消竞争时保留中断状态，让用户感到奇怪。
     * '在目前的设计中，同步控制依赖于一个通过 CAS 更新的 "状态 "字段来跟踪完成情况，
     * 以及 和一个简单的 Treiber 堆栈来容纳等待的线程。
     */

    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;

        WaitNode() {
            thread = Thread.currentThread();
        }
    }

    /**
     * 这个任务的运行状态，最初是新的。
     * 运行状态只在 set、setException 和 cancel 等方法中过渡到终端状态。
     * 在完成过程中，状态可能采取 COMPLETING（当结果被设置时）或 INTERRUPTING（仅当中断运行器以满足 cancel(true)时）的瞬态值。
     * 从这些中间状态到最终状态的转换使用更便宜的有序/快速写入，因为值是唯一的，不能被进一步修改。
     * 可能的状态转换。新的->完成的->正常的 新的->完成的->特殊的 新的->取消的 新的->中断的->中断的
     */
    private volatile int state;

    private static final int NEW = 0;
    private static final int COMPLETEING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED = 6;

    private Callable<V> callable;

    private Object outcome; // non-volatile, protected by state reads/writes

    private volatile Thread runner;

    private volatile WaitNode waiters;

    protected V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) {
            return (V) x;
        }
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable) x);
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
    }

    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null; ) {
            if (unsafe.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (; ; ) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null;      // unlink to help GC
                    q = next;
                }
                break;
            }
        }
        done();
        callable = null;
    }

    protected void done() {
    }

    protected void setException(Throwable t) {
        if (unsafe.compareAndSwapInt(this, stateOffset, NEW, COMPLETEING)) {
            outcome = t;
            unsafe.putOrderedInt(this, stateOffset, EXCEPTIONAL);
            finishCompletion();
        }
    }

    protected void set(V v) {
        if (Unsafe.getUnsafe().compareAndSwapInt(this, stateOffset, NEW, COMPLETEING)) {
            outcome = v;
            unsafe.putOrderedInt(this, stateOffset, NORMAL);
            finishCompletion();
        }
    }

    private void handlePossibleCancellationInterrupt(int s) {
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield();

        // 我们想清除我们可能收到的任何中断 cancel(true)。
        // 然而，允许使用中断作为一个独立的机制，让任务与它的调用者进行通信，并且没有办法只清除 取消中断。
    }

    @Override
    public void run() {
        if (state != NEW || !unsafe.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && unsafe.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally {
                    unsafe.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETEING)
            s = awaitDone(false, 0L);
        return report(s);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETEING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETEING)
            throw new TimeoutException();
        return report(s);
    }

    /**
     * 试图解除一个超时或中断的等待节点的链接，以避免积累垃圾。
     * 内部节点在没有 CAS 的情况下被简单地取消拼接，因为如果它们被释放者遍历的话，是无害的。
     * 为了避免从已经删除的节点中解拼的影响，在出现明显的竞赛的情况下，列表会被重新遍历。
     * 当有很多节点时，这样做很慢，但我们并不期望列表长到足以超过较高的开销方案。
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (; ; ) {
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null)
                            continue retry;
                    } else if (!unsafe.compareAndSwapObject(this, waitersOffset, q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (; ; ) {
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETEING) {
                if (q != null)
                    q.thread = null;
                return s;
            } else if (s == COMPLETEING) {
                Thread.yield();
            } else if (q == null) {
                q = new WaitNode();
            } else if (!queued) {
                queued = unsafe.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            } else {
                LockSupport.park(this);
            }
        }
    }


    private static final Unsafe unsafe;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            stateOffset = unsafe.objectFieldOffset(FutureTask.class.getDeclaredField("state"));
            runnerOffset = unsafe.objectFieldOffset(FutureTask.class.getDeclaredField("runner"));
            waitersOffset = unsafe.objectFieldOffset(FutureTask.class.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}














