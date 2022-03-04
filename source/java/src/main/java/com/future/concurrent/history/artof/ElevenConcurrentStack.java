package com.future.concurrent.history.artof;

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
 * 第十一章 并发栈和消除
 * <p>
 * 基于 TreiberStack 随着核心数扩展带来的总线争用开销骤然增加。
 * 该算法演示了指数退避和消除退避来消除总线争用的开销。
 *
 * @author future
 */
@SuppressWarnings("all")
class ElevenConcurrentStack {

    static class RangePolicy {

        int getRange() {
            return 199;
        }

        public void recordEliminationSuccess() {

        }

        public void recordEliminationTimeout() {

        }
    }

    /**
     * 采用消除数组退避来减小总线争用开销。
     */
    static class EliminationBackoffStack<T> extends LockFreeStack<T> {
        static final int capacity = 10;
        EliminationArray<T> eliminationArray = new EliminationArray<>(capacity);

        static ThreadLocal<RangePolicy> policy = new ThreadLocal<RangePolicy>() {
            @Override
            protected synchronized RangePolicy initialValue() {
                return new RangePolicy();
            }
        };

        public void push(T value) {
            RangePolicy rangePolicy = policy.get();
            Node<T> node = new Node<>(value);
            while (true) {
                if (tryPush(node)) {
                    return;
                } else {
                    try {
                        T otherValue = eliminationArray.visit(value, rangePolicy.getRange());
                        if (otherValue == null) {
                            rangePolicy.recordEliminationSuccess();
                            return; // exchanged with pop
                        }
                    } catch (TimeoutException e) {
                        rangePolicy.recordEliminationTimeout();
                    }
                }
            }
        }

        public T pop() {
            RangePolicy rangePolicy = policy.get();
            while (true) {
                Node<T> n = tryPop();
                if (n != null) {
                    return n.value;
                } else {
                    T otherValue;
                    try {
                        otherValue = eliminationArray.visit(null, rangePolicy.getRange());
                        if (otherValue != null) {
                            rangePolicy.recordEliminationSuccess();
                            return otherValue;
                        }
                    } catch (TimeoutException e) {
                        rangePolicy.recordEliminationTimeout();
                    }
                }
            }
        }
    }

    static class EliminationArray<T> {
        private static final int duration = 1000;
        LockFreeExchanger<T>[] exchanger;
        Random random;

        public EliminationArray(int capacity) {
            exchanger = new LockFreeExchanger[capacity];
            for (int i = 0; i < capacity; i++) {
                exchanger[i] = new LockFreeExchanger<>();
            }
            random = new Random();
        }

        public T visit(T value, int range) throws TimeoutException {
            int slot = random.nextInt(range);
            return (exchanger[slot].exchange(value, duration, TimeUnit.MILLISECONDS));
        }
    }

    static class LockFreeExchanger<T> {
        static final int EMPTY = 1, WAITING = 2, BUSY = 3;
        AtomicStampedReference<T> slot = new AtomicStampedReference<>(null, 0);

        public T exchange(T myitem, long timeout, TimeUnit unit) throws TimeoutException {
            long nanos = unit.toNanos(timeout);
            long timeBound = System.nanoTime() + nanos;
            int[] stampHolder = {EMPTY};
            while (true) {
                if (System.nanoTime() > timeBound)
                    throw new TimeoutException();
                T yrItem = slot.get(stampHolder);
                int stamp = stampHolder[0];
                switch (stamp) {
                    case EMPTY:
                        if (slot.compareAndSet(yrItem, myitem, EMPTY, WAITING)) {
                            while (System.nanoTime() < timeBound) {
                                yrItem = slot.get(stampHolder);
                                if (stampHolder[0] == BUSY) {
                                    slot.set(null, EMPTY);
                                    return yrItem;
                                }
                            }
                            if (slot.compareAndSet(myitem, null, WAITING, EMPTY)) {
                                throw new TimeoutException();
                            } else {
                                yrItem = slot.get(stampHolder);
                                slot.set(null, EMPTY);
                                return yrItem;
                            }
                        }
                        break;
                    case WAITING:
                        if (slot.compareAndSet(yrItem, myitem, WAITING, BUSY))
                            return yrItem;
                        break;
                    case BUSY:
                        break;
                    default:// impossible
                        break;
                }
            }
        }
    }

    /**
     * 采用指数退避策略来减少总线争用的开销
     */
    static class LockFreeStack<T> {
        AtomicReference<Node<T>> top = new AtomicReference<>();

        static final int MIN_DELAY = 10;
        static final int MAX_DELAY = 2000;
        BackOff backOff = new ExponentialBackOff(MIN_DELAY, MAX_DELAY);

        protected boolean tryPush(Node<T> node) {
            Node<T> oldTop = top.get();
            node.next = oldTop;
            return top.compareAndSet(oldTop, node);
        }

        public void push(T value) {
            Node<T> node = new Node<>(value);
            while (true) {
                if (tryPush(node)) {
                    return;
                }
                backOff.backOff();
            }
        }

        protected Node<T> tryPop() {
            Node<T> t = top.get();
            if (t == null) return null;
            Node<T> next = t.next;
            if (top.compareAndSet(t, next)) {
                return t;
            }
            return null;
        }

        public T pop() {
            while (true) {
                Node<T> node = tryPop();
                if (node != null) {
                    return node.value;
                } else {
                    backOff.backOff();
                }
            }
        }
    }

    static class Node<T> {
        public T value;
        public Node<T> next;

        public Node(T value) {
            this.value = value;
            next = null;
        }
    }
}
