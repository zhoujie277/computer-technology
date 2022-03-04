package com.future.concurrent.history;

import com.future.concurrent.history.backoff.BackOff;
import com.future.concurrent.history.backoff.ExponentialBackOff;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 7 章 自旋锁与争用
 * 复合锁及快速路径复合锁
 *
 * @author future
 */
@SuppressWarnings("unused")
class CompositeLock {

    static class CompositeFastPathLock extends CompositeLock {
        private static final int FASTPATH = 1;

        private boolean fastPathLock() {
            int oldStamp, newStamp;
            int[] stamp = {0};
            QNode qnode;
            qnode = tail.get(stamp);
            oldStamp = stamp[0];
            if (qnode != null)
                return false;
            if ((oldStamp & FASTPATH) != 0) {
                return false;
            }
            newStamp = (oldStamp + 1) | FASTPATH;
            return tail.compareAndSet(qnode, null, oldStamp, newStamp);
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            if (fastPathLock()) {
                return true;
            }
            if (super.tryLock(time, unit)) {
                while ((tail.getStamp() & FASTPATH) != 0) ;
                return true;
            }
            return false;
        }
    }

    private static final int SIZE = 10;
    private static final int MIN_BACKOFF = 100;
    private static final int MAX_BACKOFF = 2000;

    ThreadLocal<QNode> myNode = ThreadLocal.withInitial(() -> null);
    AtomicStampedReference<QNode> tail;
    QNode[] waiting;
    Random random;

    public CompositeLock() {
        tail = new AtomicStampedReference<>(null, 0);
        waiting = new QNode[SIZE];
        for (int i = 0; i < waiting.length; i++) {
            waiting[i] = new QNode();
        }
        random = new Random();
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long patience = TimeUnit.MILLISECONDS.convert(time, unit);
        long startTime = System.currentTimeMillis();
        BackOff backOff = new ExponentialBackOff(MIN_BACKOFF, MAX_BACKOFF);
        try {
            QNode node = acquireNode(backOff, startTime, patience);
            QNode pred = spliceQNode(node, startTime, patience);
            waitForPredecessor(pred, node, startTime, patience);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void unlock() {
        QNode acqNode = myNode.get();
        acqNode.state.set(State.RELEASED);
        myNode.set(null);
    }

    private QNode acquireNode(BackOff backOff, long startTime, long patience) throws TimeoutException, InterruptedException {
        QNode node = waiting[random.nextInt(SIZE)];
        QNode currTail;
        int[] currStamp = new int[1];
        while (true) {
            if (node.state.compareAndSet(State.FREE, State.WAITING)) {
                return node;
            }
            currTail = tail.get(currStamp);
            State state = node.state.get();
            if (state == State.ABORTED || state == State.RELEASED) {
                if (node == currTail) {
                    QNode myPred = null;
                    if (state == State.ABORTED) {
                        myPred = node.pred;
                    }
                    if (tail.compareAndSet(currTail, myPred, currStamp[0], currStamp[0] + 1)) {
                        node.state.set(State.WAITING);
                        return node;
                    }
                }
            }
            backOff.backOff();
            if (timeout(patience, startTime)) {
                throw new TimeoutException();
            }
        }
    }

    private boolean timeout(long patience, long startTime) {
        return false;
    }

    private QNode spliceQNode(QNode node, long startTime, long patience) throws TimeoutException {
        int[] currStamp = new int[1];
        QNode currTail;
        do {
            currTail = tail.get(currStamp);
            if (timeout(startTime, patience)) {
                node.state.set(State.FREE);
                throw new TimeoutException();
            }
        } while (!tail.compareAndSet(currTail, node, currStamp[0], currStamp[0] + 1));
        return currTail;
    }

    private void waitForPredecessor(QNode pred, QNode node, long startTime, long patience) throws TimeoutException {
        int[] stamp = {0};
        if (pred == null) {
            myNode.set(node);
            return;
        }
        State predState = pred.state.get();
        while (predState != State.RELEASED) {
            if (predState == State.ABORTED) {
                QNode temp = pred;
                pred = pred.pred;
                temp.state.set(State.FREE);
            }
            if (timeout(patience, startTime)) {
                node.pred = pred;
                node.state.set(State.ABORTED);
                throw new TimeoutException();
            }
            predState = pred.state.get();
        }
        pred.state.set(State.FREE);
        myNode.set(node);
    }

    enum State {
        FREE, WAITING, RELEASED, ABORTED
    }

    static class QNode {
        AtomicReference<State> state;
        QNode pred;

        public QNode() {
            state = new AtomicReference<>(State.FREE);
        }
    }

}
