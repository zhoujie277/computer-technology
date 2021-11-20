package com.future.concurrent.looper;

public final class Message {

    static final int FLAG_IN_USE = 1 << 0;

    static final int FLAG_ASYNCHRONOUS = 1 << 1;

    /** Flags to clear in the copyFrom method */
    static final int FLAGS_TO_CLEAR_ON_COPY_FROM = FLAG_IN_USE;

    public int what;

    public int arg1;

    public int arg2;

    public Object obj;

    public long when;

    Handler target;

    Runnable callback;

    Message next;

    public static final Object sPoolSync = new Object();
    private static Message sPool;
    private static int sPoolSize = 0;
    int flags;

    private static final int MAX_POOL_SIZE = 50;

    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.flags = 0; // clear in-use flag
                sPoolSize--;
                return m;
            }
        }
        return new Message();
    }

    public static Message obtain(Message orig) {
        Message m = obtain();
        m.what = orig.what;
        m.arg1 = orig.arg1;
        m.arg2 = orig.arg2;
        m.obj = orig.obj;
        m.target = orig.target;
        m.callback = orig.callback;
        return m;
    }

    public static Message obtain(Handler h) {
        Message m = obtain();
        m.target = h;

        return m;
    }

    public static Message obtain(Handler h, Runnable callback) {
        Message m = obtain();
        m.target = h;
        m.callback = callback;
        return m;
    }

    public static Message obtain(Handler h, int what) {
        Message m = obtain();
        m.target = h;
        m.what = what;
        return m;
    }

    public static Message obtain(Handler h, int what, Object obj) {
        Message m = obtain();
        m.target = h;
        m.what = what;
        m.obj = obj;
        return m;
    }

    public static Message obtain(Handler h, int what, int arg1, int arg2) {
        Message m = obtain();
        m.target = h;
        m.what = what;
        m.arg1 = arg1;
        m.arg2 = arg2;
        return m;
    }

    public static Message obtain(Handler h, int what, int arg1, int arg2, Object obj) {
        Message m = obtain();
        m.target = h;
        m.what = what;
        m.arg1 = arg1;
        m.arg2 = arg2;
        m.obj = obj;
        return m;
    }

    public long getWhen() {
        return when;
    }

    public void setTarget(Handler target) {
        this.target = target;
    }

    public Handler getTarget() {
        return target;
    }

    public Runnable getCallback() {
        return callback;
    }

    public Message setCallback(Runnable r) {
        callback = r;
        return this;
    }

    public Message setWhat(int what) {
        this.what = what;
        return this;
    }

    public void sendToTarget() {
        target.sendMessage(this);
    }

    public boolean isAsynchronous() {
        return (flags & FLAG_ASYNCHRONOUS) != 0;
    }

    public void setAsynchronous(boolean async) {
        if (async) {
            flags |= FLAG_ASYNCHRONOUS;
        } else {
            flags &= ~FLAG_ASYNCHRONOUS;
        }
    }

    boolean isInUse() {
        return ((flags & FLAG_IN_USE) == FLAG_IN_USE);
    }

    void markInUse() {
        flags |= FLAG_IN_USE;
    }

    public void recycle() {
        if (isInUse()) {
            throw new IllegalStateException("This message cannot be recycled because it " + "is still in use.");
        }
        recycleUnchecked();
    }

    void recycleUnchecked() {
        // Mark the message as in use while it remains in the recycled object pool.
        // Clear out all other details.
        flags = FLAG_IN_USE;
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        when = 0;
        target = null;
        callback = null;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }
}
