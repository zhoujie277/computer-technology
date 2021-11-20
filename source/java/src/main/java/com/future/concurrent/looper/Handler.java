package com.future.concurrent.looper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler {
    private static Handler MAIN_THREAD_HANDLER = null;

    public static Handler getMain() {
        if (MAIN_THREAD_HANDLER == null) {
            MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());
        }
        return MAIN_THREAD_HANDLER;
    }

    public static Handler createAsync(@NonNull Looper looper) {
        return new Handler(looper, null, true);
    }

    public final Message obtainMessage() {
        return Message.obtain(this);
    }

    public final Message obtainMessage(int what) {
        return Message.obtain(this, what);
    }

    public final Message obtainMessage(int what, Object obj) {
        return Message.obtain(this, what, obj);
    }

    public final boolean postAtTime(@NonNull Runnable r, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r), uptimeMillis);
    }

    public final boolean post(@NonNull Runnable r) {
        return sendMessageDelayed(getPostMessage(r), 0);
    }

    public final boolean postDelayed(@NonNull Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r), delayMillis);
    }

    public final boolean postDelayed(Runnable r, int what, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r).setWhat(what), delayMillis);
    }

    public final boolean postAtFrontOfQueue(@NonNull Runnable r) {
        return sendMessageAtFrontOfQueue(getPostMessage(r));
    }

    public final boolean runWithScissors(@NonNull Runnable r, long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }

        if (Looper.myLooper() == mLooper) {
            r.run();
            return true;
        }

        BlockingRunnable br = new BlockingRunnable(r);
        return br.postAndWait(this, timeout);
    }

    public final void removeCallbacks(@NonNull Runnable r) {
        mQueue.removeMessages(this, r, null);
    }

    public final boolean sendMessage(@NonNull Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what, 0);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageDelayed(msg, delayMillis);
    }

    public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageAtTime(msg, uptimeMillis);
    }

    public final boolean sendMessageDelayed(@NonNull Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, System.currentTimeMillis() + delayMillis);
    }

    public final boolean sendMessageAtFrontOfQueue(@NonNull Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(this + " sendMessageAtTime() called with no mQueue");
            log.error(e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, 0);
    }

    public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(this + " sendMessageAtTime() called with no mQueue");
            log.error(e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }

    public final boolean executeOrSendMessage(@NonNull Message msg) {
        if (mLooper == Looper.myLooper()) {
            dispatchMessage(msg);
            return true;
        }
        return sendMessage(msg);
    }

    private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg, long uptimeMillis) {
        msg.target = this;

        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }

    public final void removeMessages(int what) {
        mQueue.removeMessages(this, what, null);
    }

    public final void removeMessages(int what, Object object) {
        mQueue.removeMessages(this, what, object);
    }

    public final void removeEqualMessages(int what, Object object) {
        mQueue.removeEqualMessages(this, what, object);
    }

    public final boolean hasMessages(int what) {
        return mQueue.hasMessages(this, what, null);
    }

    public final boolean hasMessagesOrCallbacks() {
        return mQueue.hasMessages(this);
    }

    public final boolean hasMessages(int what, Object object) {
        return mQueue.hasMessages(this, what, object);
    }

    public final boolean hasEqualMessages(int what, Object object) {
        return mQueue.hasEqualMessages(this, what, object);
    }

    public final boolean hasCallbacks(@NonNull Runnable r) {
        return mQueue.hasMessages(this, r, null);
    }

    @NonNull
    public final Looper getLooper() {
        return mLooper;
    }

    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }

    public interface Callback {
        boolean handleMessage(@NonNull Message msg);
    }

    private static void handleCallback(Message message) {
        message.callback.run();
    }

    final Looper mLooper;
    final MessageQueue mQueue;
    final Callback mCallback;
    final boolean mAsynchronous;

    public Handler(@NonNull Looper looper) {
        this(looper, null, false);
    }

    public Handler(boolean async) {
        this(null, async);
    }

    public Handler(@NonNull Looper looper, Callback callback) {
        this(looper, callback, false);
    }

    public Handler(Callback callback, boolean async) {
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException("Can't create handler inside thread " + Thread.currentThread()
                    + " that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }

    public Handler(@NonNull Looper looper, Callback callback, boolean async) {
        mLooper = looper;
        mQueue = looper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }

    /**
     * Subclasses must implement this to receive messages.
     */
    public void handleMessage(@NonNull Message msg) {
    }

    /**
     * Handle system messages here.
     */
    public void dispatchMessage(@NonNull Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }

    private static final class BlockingRunnable implements Runnable {
        private final Runnable mTask;
        private boolean mDone;

        public BlockingRunnable(Runnable task) {
            mTask = task;
        }

        @Override
        public void run() {
            try {
                mTask.run();
            } finally {
                synchronized (this) {
                    mDone = true;
                    notifyAll();
                }
            }
        }

        public boolean postAndWait(Handler handler, long timeout) {
            if (!handler.post(this)) {
                return false;
            }
            synchronized (this) {
                if (timeout > 0) {
                    final long expirationTime = System.currentTimeMillis() + timeout;
                    while (!mDone) {
                        long delay = expirationTime - System.currentTimeMillis();
                        if (delay <= 0) {
                            return false; // timeout
                        }
                        try {
                            wait(delay);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    while (!mDone) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
            return true;
        }
    }
}