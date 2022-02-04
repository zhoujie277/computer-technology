package com.future.concurrent.history.java5;

import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A cancellable asynchronous computation.
 * This class provides a base implementation of  Future,
 * with methods to start and cancel a computation,
 * query to see if the computation is complete,
 * and retrieve the result of the computation.
 * The result can only be  retrieved when the computation has completed;
 * the get method will block if the computation has not yet completed.
 * Once the computation has completed,
 * the computation cannot be restarted or cancelled.
 */
@SuppressWarnings("unused")
public class Java5FutureTask<V> implements Future<V>, Runnable {

    private final class Sync extends AbstractQueuedSynchronizer {

        private static final int RUNNING = 1;
        private static final int RAN = 2;
        private static final int CANCELLED = 4;

        private final Callable<V> callable;

        private V result;

        private Throwable exception;

        private volatile Thread runner;

        Sync(Callable<V> callable) {
            this.callable = callable;
        }

        private boolean ranOrCancelled(int state) {
            return (state & (RAN | CANCELLED)) != 0;
        }

        boolean innerIsDone() {
            return ranOrCancelled(getState()) && runner == null;
        }

        boolean innerIsCancelled() {
            return getState() == CANCELLED;
        }

        protected int tryAcquireShared(int ignore) {
            return innerIsDone() ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            runner = null;
            return true;
        }

        V innerGet() throws InterruptedException, ExecutionException {
            acquireSharedInterruptibly(0);
            if (getState() == CANCELLED) {
                throw new CancellationException();
            }
            if (exception != null)
                throw new ExecutionException(exception);
            return result;
        }

        V innerGet(long nanosTimeout) throws InterruptedException, ExecutionException, TimeoutException {
            if (!tryAcquireSharedNanos(0, nanosTimeout))
                throw new TimeoutException();
            if (getState() == CANCELLED)
                throw new CancellationException();
            if (exception != null)
                throw new ExecutionException(exception);
            return result;
        }

        void innerSet(V v) {
            for (; ; ) {
                int s = getState();
                if (ranOrCancelled(s))
                    return;
                if (compareAndSetState(s, RAN))
                    break;
            }
            result = v;
            releaseShared(0);
            done();
        }

        void innerSetException(Throwable t) {
            for (; ; ) {
                int s = getState();
                if (ranOrCancelled(s))
                    return;
                if (compareAndSetState(s, RAN))
                    break;
            }
            exception = t;
            result = null;
            releaseShared(0);
            done();
        }

        boolean innerCancel(boolean mayInterruptIfRunning) {
            for (; ; ) {
                int s = getState();
                if (ranOrCancelled(s))
                    return false;
                if (compareAndSetState(s, CANCELLED))
                    break;
            }

            if (mayInterruptIfRunning) {
                Thread r = runner;
                if (r != null)
                    r.interrupt();
            }
            releaseShared(0);
            done();
            return true;
        }

        void innerRun() {
            if (!compareAndSetState(0, RUNNING))
                return;
            try {
                runner = Thread.currentThread();
                innerSet(callable.call());
            } catch (Throwable ex) {
                innerSetException(ex);
            }
        }

        boolean innerRunAndReset() {
            if (!compareAndSetState(0, RUNNING))
                return false;
            try {
                runner = Thread.currentThread();
                callable.call();    // don't set result
                runner = null;
                return compareAndSetState(RUNNING, 0);
            } catch (Throwable ex) {
                innerSetException(ex);
                return false;
            }
        }
    }

    private final Sync sync;

    public Java5FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        sync = new Sync(callable);
    }

    public Java5FutureTask(Runnable runnable, V result) {
        sync = new Sync(Executors.callable(runnable, result));
    }

    protected boolean runAndReset() {
        return sync.innerRunAndReset();
    }

    protected void done() {
    }

    protected void set(V v) {
        sync.innerSet(v);
    }

    protected void setException(Throwable t) {
        sync.innerSetException(t);
    }

    @Override
    public void run() {
        sync.innerRun();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return sync.innerCancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return sync.innerIsCancelled();
    }

    @Override
    public boolean isDone() {
        return sync.innerIsDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return sync.innerGet();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return sync.innerGet(unit.toNanos(timeout));
    }
}
