package com.future.concurrent.looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AsyncTask<Params, Result> {

    public enum Status {
        PENDING, RUNNING, FINISHED,
    }

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int BACKUP_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static ThreadPoolExecutor sBackupExecutor;
    private static LinkedBlockingQueue<Runnable> sBackupExecutorQueue;

    private static final RejectedExecutionHandler sRunOnSerialPolicy = new RejectedExecutionHandler() {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.warn("Exceeded ThreadPoolExecutor pool size");

            // As a last ditch fallback, run it on an executor with an unbounded queue.
            // Create this executor lazily, hopefully almost never.
            synchronized (this) {
                if (sBackupExecutor == null) {
                    sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                    sBackupExecutor = new ThreadPoolExecutor(BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                            TimeUnit.SECONDS, sBackupExecutorQueue, sThreadFactory);
                    sBackupExecutor.allowCoreThreadTimeOut(true);
                }
            }
            sBackupExecutor.execute(r);
        }
    };

    public static final Executor sDefaultExecutor;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(sRunOnSerialPolicy);
        sDefaultExecutor = threadPoolExecutor;
    }

    private static final int MESSAGE_POST_RESULT = 0x1;

    private final FutureTask<Result> mFuture;
    private volatile Status mStatus = Status.PENDING;
    private final AtomicBoolean mCancelled = new AtomicBoolean();

    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    private final WorkerRunnable mWorker = new WorkerRunnable();

    private final Handler mHandler;

    private Handler getHandler() {
        return mHandler;
    }

    public AsyncTask() {
        this(Looper.myLooper());
    }

    public AsyncTask(Looper callbackLooper) {
        mHandler = new InternalHandler<>(callbackLooper, this);

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()", e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT, result);
        message.sendToTarget();
        return result;
    }

    public final Status getStatus() {
        return mStatus;
    }

    protected abstract Result doInBackground(Params params);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onCancelled(Result result) {
        onCancelled();
    }

    protected void onCancelled() {
    }

    public final boolean isCancelled() {
        return mCancelled.get();
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    public final Result get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    public final AsyncTask<Params, Result> execute(Params params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    public final AsyncTask<Params, Result> executeOnExecutor(Executor exec, Params params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
            case RUNNING:
                throw new IllegalStateException("Cannot execute task:" + " the task is already running.");
            case FINISHED:
                throw new IllegalStateException("Cannot execute task:" + " the task has already been executed "
                        + "(a task can be executed only once)");
            default:
                break;
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        exec.execute(mFuture);

        return this;
    }

    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    private static class InternalHandler<Param, Result> extends Handler {

        private WeakReference<AsyncTask<Param, Result>> mTaskRef = null;

        public InternalHandler(Looper looper, AsyncTask<Param, Result> task) {
            super(looper);
            mTaskRef = new WeakReference<>(task);
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public void handleMessage(Message msg) {
            AsyncTask<Param, Result> asyncTask = mTaskRef.get();
            if (asyncTask == null)
                return;
            switch (msg.what) {
            case MESSAGE_POST_RESULT:
                asyncTask.finish((Result) msg.obj);
                break;
            }
        }
    }

    private class WorkerRunnable implements Callable<Result> {
        Params mParams;

        public Result call() throws Exception {
            mTaskInvoked.set(true);
            Result result = null;
            try {
                // noinspection unchecked
                result = doInBackground(mParams);
            } catch (Throwable tr) {
                mCancelled.set(true);
                throw tr;
            } finally {
                postResult(result);
            }
            return result;
        }
    }
}
