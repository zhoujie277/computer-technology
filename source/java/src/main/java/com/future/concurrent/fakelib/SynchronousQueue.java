package com.future.concurrent.fakelib;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 一种阻塞队列，其中每个插入操作必须等待另一个线程执行相应的删除操作，反之亦然。
 * 同步队列没有任何内部容量，甚至没有1的容量。您无法查看 (peek) 同步队列，
 * 因为只有在尝试删除某个元素时该元素才存在; 除非另一个线程试图删除某个元素，
 * 否则使用任何方法都不能插入一个元素；您不能迭代，因为没有可迭代的内容。
 * 队列头是第一个排队插入线程试图添加到队列中的元素；如果没有这样的排队线程，
 * 则没有可用于删除的元素，poll(）将返回 null。对于其他的集合方法，
 * 例如 contains，同步队列都被视作一个空集合。
 * 该队列不允许空元素。
 */

@SuppressWarnings("unused")
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private static final long serialVersionUID = -3223113410248163686L;

    abstract static class Transferer<E> {

        abstract E transfer(E e, boolean timed, long nanos);
    }

    static final int NCPU = Runtime.getRuntime().availableProcessors();

    static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;
    static final int maxUntimedSpins = maxTimedSpins * 16;

    static final long spinForTimeoutThreshold = 1000L;

    /**
     * Dual Stack
     */
    static final class TransferStack<E> extends Transferer<E> {

        static final int REQUEST = 0;

        static final int DATA = 1;

        static final int FULFILLING = 2;

        static boolean isFulfilling(int m) {
            return (m & FULFILLING) != 0;
        }

        static final class SNode {
            volatile SNode next;
            volatile SNode match;
            volatile Thread waiter;
            Object item;
            int mode;

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                return cmp == next && unsafe.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean tryMatch(SNode s) {
                if (match == null && unsafe.compareAndSwapObject(this, matchOffset, null, match)) {
                    Thread w = waiter;
                    if (w != null) {
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                return match == s;
            }

            void tryCancel() {
                unsafe.compareAndSwapObject(this, matchOffset, null, this);
            }

            boolean isCancelled() {
                return match == this;
            }

            private static final Unsafe unsafe;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                    matchOffset = unsafe.objectFieldOffset(SNode.class.getDeclaredField("match"));
                    nextOffset = unsafe.objectFieldOffset(SNode.class.getDeclaredField("next"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }

        volatile SNode head;

        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) s = new SNode(e);
            s.mode = mode;
            s.next = next;
            return s;
        }

        @SuppressWarnings("unchecked")
        @Override
        E transfer(E e, boolean timed, long nanos) {
            SNode s = null;
            int mode = (e == null) ? REQUEST : DATA;

            for (; ; ) {
                SNode h = head;
                if (h == null || h.mode == mode) {
                    if (timed && nanos <= 0) {
                        if (h != null && h.isCancelled())
                            casHead(h, h.next);
                        else
                            return null;
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        SNode m = awaitFulfill(s, timed, nanos);
                        if (m == s) {
                            clean(s);
                            return null;
                        }
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) {
                    if (h.isCancelled())
                        casHead(h, h.next);
                    else if (casHead(h, s = snode(s, e, h, FULFILLING | mode))) {
                        for (; ; ) {
                            SNode m = s.next;
                            if (m == null) {
                                casHead(s, null);
                                s = null;
                                break;
                            }
                            SNode mn = m.next;
                            if (m.tryMatch(s)) {
                                casHead(s, mn);
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else
                                s.casNext(m, mn);
                        }
                    }
                } else {
                    SNode m = h.next;
                    if (m == null)
                        casHead(m, null);
                    else {
                        SNode mn = m.next;
                        if (m.tryMatch(h))
                            casHead(h, mn);
                        else
                            h.casNext(m, mn);
                    }
                }
            }
        }

        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = shouldSpin(s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0;
            for (; ; ) {
                if (w.isInterrupted())
                    s.tryCancel();
                SNode m = s.match;
                if (m != null)
                    return m;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0)
                    spins = shouldSpin(s) ? (spins - 1) : 0;
                else if (s.waiter == null)
                    s.waiter = w;
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }

        void clean(SNode s) {
            s.item = null;
            s.waiter = null;

            SNode past = s.next;
            if (past != null && past.isCancelled()) {
                past = past.next;
            }

            SNode p;
            while ((p = head) != null && p != past && p.isCancelled())
                casHead(p, p.next);

            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled())
                    p.casNext(n, n.next);
                else
                    p = n;
            }
        }

        boolean casHead(SNode h, SNode val) {
            return h == head && unsafe.compareAndSwapObject(this, headOffset, h, val);
        }

        private static final Unsafe unsafe;
        private static final long headOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                headOffset = unsafe.objectFieldOffset(TransferStack.class.getDeclaredField("head"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    static final class TransferQueue<E> extends Transferer<E> {

        static final class QNode {
            volatile QNode next;
            volatile Object item;
            volatile Thread waiter;
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp && unsafe.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp && unsafe.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            void tryCancel(Object cmp) {
                unsafe.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            boolean isCancelled() {
                return item == this;
            }

            boolean isOffList() {
                return next == this;
            }

            private static final Unsafe unsafe;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                    itemOffset = unsafe.objectFieldOffset(QNode.class.getDeclaredField("item"));
                    nextOffset = unsafe.objectFieldOffset(QNode.class.getDeclaredField("next"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }

        transient volatile QNode head;
        transient volatile QNode tail;

        transient volatile QNode cleanMe;

        TransferQueue() {
            QNode h = new QNode(null, false);
            head = h;
            tail = h;
        }

        void advanceHead(QNode h, QNode nh) {
            if (h == head && unsafe.compareAndSwapObject(this, headOffset, h, nh)) {
                h.next = h; // forget old next
            }
        }

        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                unsafe.compareAndSwapObject(this, tailOffset, t, nt);
        }

        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp && unsafe.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        @SuppressWarnings("unchecked")
        @Override
        E transfer(E e, boolean timed, long nanos) {
            QNode s = null;
            boolean isData = (e != null);
            for (; ; ) {
                QNode t = tail;
                QNode h = head;
                if (t == null || h == null)
                    continue;
                if (h == t || t.isData == isData) {
                    QNode tn = t.next;
                    if (t != tail)
                        continue;
                    if (tn != null) {
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)
                        return null;
                    if (s == null)
                        s = new QNode(e, isData);
                    if (!t.casNext(null, s))
                        continue;

                    advanceTail(t, s);
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) {
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {
                        advanceHead(t, s);
                        if (x != null)
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E) x : e;
                } else {
                    QNode m = h.next;
                    if (t != tail || m == null || h != head)
                        continue;
                    Object x = m.item;
                    if (isData == (x != null) || x == m || !m.casItem(x, e)) {
                        advanceHead(h, m);
                        continue;
                    }

                    advanceHead(h, m);
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E) x : e;
                }
            }
        }

        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            final long deadLine = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (; ; ) {
                if (w.isInterrupted())
                    s.tryCancel(e);
                Object x = s.item;
                if (x != e)
                    return x;
                if (timed) {
                    nanos = deadLine - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0)
                    --spins;
                else if (s.waiter == null)
                    s.waiter = w;
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        void clean(QNode pred, QNode s) {
            s.waiter = null;

            while (pred.next == s) {
                QNode h = head;
                QNode hn = h.next;
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;
                if (t == h)
                    return;
                QNode tn = t.next;
                if (t != tail)
                    continue;
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                QNode dp = cleanMe;
                if (dp != null) {
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null || d == dp || !d.isCancelled()
                            || (d != t && (dn = d.next) != null && dn != d && dp.casNext(d, dn))) {
                        casCleanMe(dp, null);
                    }
                    if (dp == pred)
                        return;
                } else if (casCleanMe(null, pred))
                    return;
            }
        }

        private static final Unsafe unsafe;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                headOffset = unsafe.objectFieldOffset(TransferQueue.class.getDeclaredField("head"));
                tailOffset = unsafe.objectFieldOffset(TransferQueue.class.getDeclaredField("tail"));
                cleanMeOffset = unsafe.objectFieldOffset(TransferQueue.class.getDeclaredField("cleanMe"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    private transient volatile Transferer<E> transferer;

    public SynchronousQueue() {
        this(false);
    }

    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<>() : new TransferStack<>();
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        }
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    @Override
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return transferer.transfer(null, true, 0);
    }

    @Override
    public E peek() {
        return null;
    }
}
