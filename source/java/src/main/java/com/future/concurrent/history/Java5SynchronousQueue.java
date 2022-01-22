package com.future.concurrent.fakelib;


import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 每个消费者线程一个 node，不共享状态。生产者线程亦如是
 * 但一个生产者线程和一个消费者线程可能会达成匹配，从而共享一个 node。
 *
 * 结点超时了仍然挂在队列上，所有 put/take 相关方法需要注意重试。
 */
@SuppressWarnings("unused")
public class Java5SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    static final class Node extends AbstractQueuedSynchronizer {
        private static final int ACK = 1;
        private static final int CANCEL = -1;

        Object item;

        Node next;

        Node(Object x) {
            item = x;
        }

        Node(Object x, Node n) {
            item = x;
            next = n;
        }

        protected boolean tryAcquire(int ignore) {
            return getState() != 0;
        }

        @Override
        protected boolean tryRelease(int arg) {
            return compareAndSetState(0, arg);
        }

        private Object extract() {
            Object x = item;
            item = null;
            return x;
        }

        private void checkCancellationOnInterrupt(InterruptedException ie) throws InterruptedException {
            if (release(CANCEL))
                throw ie;
            Thread.currentThread().interrupt();
        }

        boolean setItem(Object x) {
            item = x;
            return release(ACK);
        }

        Object getItem() {
            return release(ACK) ? extract() : null;
        }

        void waitForTake() throws InterruptedException {
            try {
                acquireInterruptibly(0);
            } catch (InterruptedException ie) {
                checkCancellationOnInterrupt(ie);
            }
        }

        Object waitForPut() throws InterruptedException {
            try {
                acquireInterruptibly(0);
            } catch (InterruptedException ie) {
                checkCancellationOnInterrupt(ie);
            }
            return extract();
        }

        boolean waitForTake(long nanos) throws InterruptedException {
            try {
                if (!tryAcquireNanos(0, nanos) && release(CANCEL))
                    return false;
            } catch (InterruptedException ie) {
                checkCancellationOnInterrupt(ie);
            }
            return true;
        }

        Object waitForPut(long nanos) throws InterruptedException {
            try {
                if (!tryAcquireNanos(0, nanos) && release(CANCEL)) {
                    return null;
                }
            } catch (InterruptedException ie) {
                checkCancellationOnInterrupt(ie);
            }
            return extract();
        }

    }

    static abstract class WaitQueue {
        abstract Node enq(Object x);

        abstract Node deq();
    }

    static final class FifoWaitQueue extends WaitQueue {

        private Node head;
        private Node last;

        @Override
        Node enq(Object x) {
            Node p = new Node(x);
            if (last == null)
                last = head = p;
            else
                last = last.next = p;
            return p;
        }

        @Override
        Node deq() {
            Node p = head;
            if (p != null) {
                if ((head = p.next) == null)
                    last = null;
                p.next = null;
            }
            return p;
        }
    }

    static final class LifoWaitQueue extends WaitQueue {
        private Node head;

        @Override
        Node enq(Object x) {
            head = new Node(x, head);
            return head;
        }

        @Override
        Node deq() {
            Node p = head;
            if (p != null) {
                head = p.next;
                p.next = null;
            }
            return p;
        }
    }

    private final ReentrantLock qlock;

    private final WaitQueue waitingProducers;

    private final WaitQueue waitingConsumers;

    public Java5SynchronousQueue() {
        this(false);
    }

    public Java5SynchronousQueue(boolean fair) {
        qlock = new ReentrantLock(fair);
        if (fair) {
            waitingProducers = new FifoWaitQueue();
            waitingConsumers = new FifoWaitQueue();
        } else {
            waitingProducers = new LifoWaitQueue();
            waitingConsumers = new LifoWaitQueue();
        }
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
        final ReentrantLock lock = this.qlock;
        for (; ; ) {
            Node node;
            boolean mustWait;
            if (Thread.interrupted()) throw new InterruptedException();
            lock.lock();
            try {
                node = waitingConsumers.deq();
                if ((mustWait = (node == null))) {
                    node = waitingProducers.enq(e);
                }
            } finally {
                lock.unlock();
            }
            if (mustWait) {
                node.waitForTake();
                return;
            } else if (node.setItem(e))
                return;
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.qlock;
        for (; ; ) {
            Node node;
            boolean mustWait;
            if (Thread.interrupted()) throw new InterruptedException();
            lock.lock();
            try {
                node = waitingConsumers.deq();
                if ((mustWait = (node == null))) {
                    node = waitingProducers.enq(e);
                }
            } finally {
                lock.unlock();
            }

            if (mustWait)
                return node.waitForTake(nanos);
            else if (node.setItem(e)) {
                return true;
            }

            // else consumer cancelled, so retry
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.qlock;
        for (; ; ) {
            Node node;
            boolean mustWait;

            if (Thread.interrupted()) throw new InterruptedException();
            lock.lock();
            try {
                node = waitingProducers.deq();
                if (mustWait = (node == null)) {
                    node = waitingConsumers.enq(null);
                }
            } finally {
                lock.unlock();
            }
            if (mustWait) {
                Object x = node.waitForPut();
                return (E) x;
            } else {
                Object x = node.getItem();
                if (x != null)
                    return (E) x;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.qlock;
        for (; ; ) {
            Node node;
            boolean mustWait;

            if (Thread.interrupted()) throw new InterruptedException();
            lock.lock();
            try {
                node = waitingProducers.deq();
                if ((mustWait = (node == null)))
                    node = waitingConsumers.enq(null);
            } finally {
                lock.unlock();
            }
            // 每个消费者线程一个 node，不共享状态。生产者亦如是
            if (mustWait) {
                return (E) node.waitForPut(nanos);
            } else {
                Object x = node.getItem();
                if (x != null)
                    return (E) x;
                // else cancelled, so retry
            }
        }
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
        if (e == null) throw new NullPointerException();
        final ReentrantLock qlock = this.qlock;
        for (; ; ) {
            Node node;
            qlock.lock();
            try {
                node = waitingConsumers.deq();
            } finally {
                qlock.unlock();
            }

            if (node == null)
                return false;
            else if (node.setItem(e))
                return true;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E poll() {
        final ReentrantLock lock = qlock;
        for (; ; ) {
            Node node;
            qlock.lock();
            try {
                node = waitingProducers.deq();
            } finally {
                qlock.unlock();
            }
            if (node == null)
                return null;
            else {
                Object x = node.getItem();
                if (x != null)
                    return (E) x;
            }
        }
    }

    @Override
    public E peek() {
        return null;
    }
}
