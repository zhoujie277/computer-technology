package com.future.concurrent.history.artof;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The art of multiprocessor programming
 * 第 10 章 并行队列和 ABA 问题
 *
 * @author future
 */
@SuppressWarnings("unused")
class TenParallelQueue {

    /**
     * ABA 问题解决方案演示。
     * 该算法假设每个线程都使用了循环列表，
     * 入队时从循环列表取结点；
     * 出队时将结点放入循环队列。
     */
    static class LockFreeQueueRecycle<T> {

        class N {
            T value;
            AtomicStampedReference<N> next;

            public N(T x) {
                value = x;
                next = new AtomicStampedReference<>(null, 0);
            }
        }

        AtomicStampedReference<N> head, tail;

        public T deq() {
            int[] lastStamp = new int[1];
            int[] firstStamp = new int[1];
            int[] nextStamp = new int[1];
            while (true) {
                N first = head.get(firstStamp);
                N last = tail.get(lastStamp);
                N next = first.next.get(nextStamp);
                if (first == last) {
                    if (next == null)
                        return null;
                    tail.compareAndSet(last, next, lastStamp[0], lastStamp[0] + 1);
                } else {
                    T value = next.value;
                    if (head.compareAndSet(first, next, firstStamp[0], firstStamp[0] + 1)) {
                        free(first);
                        return value;
                    }
                }
            }
        }

        public void free(N n) {
            // free n to recycle
        }
    }


    static class LockFreeQueue<T> {

        static class Nod<T> {
            T value;
            AtomicReference<Nod<T>> next;

            public Nod(T x) {
                value = x;
                next = new AtomicReference<>(null);
            }
        }

        AtomicReference<Nod<T>> head, tail;

        public LockFreeQueue() {
            Nod<T> n = new Nod<>(null);
            head = new AtomicReference<>(n);
            tail = head;
        }

        public void enq(T x) {
            Nod<T> node = new Nod<>(x);
            while (true) {
                Nod<T> t = tail.get();
                Nod<T> next = t.next.get();
                if (t == tail.get()) {
                    if (next != null) {
                        tail.compareAndSet(t, next);
                    } else {
                        if (t.next.compareAndSet(null, node)) {
                            tail.compareAndSet(t, node);
                            return;
                        }
                    }
                }
            }
        }

        public T deq() {
            while (true) {
                Nod<T> first = head.get();
                Nod<T> last = tail.get();
                Nod<T> next = first.next.get();
                if (first == head.get()) {
                    if (first == last) {
                        if (next == null)
                            return null;
                        tail.compareAndSet(last, next);
                    } else {
                        T value = next.value;
                        if (head.compareAndSet(first, next))
                            return value;
                    }
                }
            }
        }
    }

    static class UnboundedQueue<T> {
        ReentrantLock enqLock = new ReentrantLock();
        ReentrantLock deqLock = new ReentrantLock();
        volatile Node<T> head, tail;

        UnboundedQueue() {
            head = new Node<>(null);
            tail = head;
        }

        public void enq(T x) {
            Node<T> node = new Node<>(x);
            enqLock.lock();
            try {
                tail.next = node;
                tail = node;
            } finally {
                enqLock.unlock();
            }
        }

        public T deq() {
            T result;
            deqLock.lock();
            try {
                Node<T> next = head.next;
                if (next == null)
                    return null;
                result = next.value;
                head = next;
            } finally {
                deqLock.unlock();
            }
            return result;
        }
    }

    static class BoundedQueue<T> {
        ReentrantLock enqLock, deqLock;
        Condition notEmpty, notFull;
        AtomicInteger size;
        volatile Node<T> head, tail;
        int capacity;

        public BoundedQueue(int capacity) {
            this.capacity = capacity;
            head = new Node<>(null);
            tail = head;
            size = new AtomicInteger(0);
            enqLock = new ReentrantLock();
            deqLock = new ReentrantLock();
            notFull = enqLock.newCondition();
            notEmpty = deqLock.newCondition();
        }

        public void enq(T x) {
            boolean mustWakeDequeuers = false;
            enqLock.lock();
            try {
                while (size.get() == capacity) {
                    try {
                        notFull.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Node<T> e = new Node<>(x);
                tail.next = tail;
                tail = e;
                if (size.getAndIncrement() == 0) {
                    mustWakeDequeuers = true;
                }
            } finally {
                enqLock.unlock();
            }
            if (mustWakeDequeuers) {
                deqLock.lock();
                try {
                    notEmpty.signalAll();
                } finally {
                    deqLock.unlock();
                }
            }
        }

        public T deq() {
            T result;
            boolean mustWakeEnqueuers = false;
            deqLock.lock();
            try {
                while (size.get() == 0) {
                    try {
                        notEmpty.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Node<T> next = head.next;
                result = next.value;
                head = next;
                if (size.getAndDecrement() == capacity) {
                    mustWakeEnqueuers = true;
                }
            } finally {
                deqLock.unlock();
            }
            if (mustWakeEnqueuers) {
                enqLock.unlock();
                try {
                    notFull.signalAll();
                } finally {
                    enqLock.unlock();
                }
            }
            return result;
        }
    }

    static class Node<T> {
        public T value;
        public volatile Node<T> next;

        public Node(T x) {
            value = x;
            next = null;
        }
    }

}
