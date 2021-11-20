package com.future.concurrent.looper;

public class Looper {

    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();

    private static Looper sMainLooper;

    final MessageQueue mQueue;

    private boolean mInLoop;

    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
    }

    private static boolean loopOnce(final Looper me) {
        Message msg = me.mQueue.next(); // might block
        if (msg == null) {
            // No message indicates that the message queue is quitting.
            return false;
        }
        msg.target.dispatchMessage(msg);
        msg.recycleUnchecked();
        return true;
    }

    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        if (me.mInLoop) {
            System.out.println("Loop again would have the queued messages be executed" + " before this one completed.");
        }

        me.mInLoop = true;

        for (;;) {
            if (!loopOnce(me)) {
                // exit loop
                return;
            }
        }
    }

    public static void prepareMainLoop() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }

    public static void prepare() {
        prepare(true);
    }

    public static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }

    public static Looper myLooper() {
        return sThreadLocal.get();
    }
}
