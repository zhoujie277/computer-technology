package com.future.concurrent.history.artof;

import com.future.concurrent.java8.atomic.AtomicMarkableReference;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 9 章 链表：锁的作用
 * 演示代码：针对链表不同的粒度同步技术
 *
 * @author future
 */
@SuppressWarnings("unused")
class NineFunctionOfLock {

    static class Nod<E> {

        int key;
        E item;
        AtomicMarkableReference<Nod<E>> next;

        public Nod(int key) {
            this.key = key;
        }

        public Nod(E item) {
            this.item = item;
            this.key = item.hashCode();
        }

    }

    static class Window<E> {

        Nod<E> pred, curr;

        Window(Nod<E> myPred, Nod<E> myCurr) {
            pred = myPred;
            curr = myCurr;
        }

    }

    /**
     * 非阻塞算法的演示
     */
    static class NonBlockingList<E> implements Set<E> {

        private final Nod<E> head;

        NonBlockingList(E item) {
            head = new Nod<>(Integer.MIN_VALUE);
            Nod<E> nod = new Nod<>(Integer.MAX_VALUE);
            head.next = new AtomicMarkableReference<>(nod, false);
        }

        @Override
        public boolean add(E x) {
            int key = x.hashCode();
            while (true) {
                Window<E> window = find(head, key);
                Nod<E> pred = window.pred, curr = window.curr;
                if (curr.key == key) {
                    return false;
                } else {
                    Nod<E> node = new Nod<>(x);
                    node.next = new AtomicMarkableReference<>(curr, false);
                    if (pred.next.compareAndSet(curr, node, false, false)) {
                        return true;
                    }
                }
            }
        }

        @Override
        public boolean remove(E x) {
            int key = x.hashCode();
            boolean snip;
            while (true) {
                Window<E> window = find(head, key);
                Nod<E> pred = window.pred, curr = window.curr;
                if (curr.key != key) {
                    return false;
                } else {
                    Nod<E> succ = curr.next.getReference();
                    snip = curr.next.compareAndSet(succ, succ, false, true);
                    if (!snip)
                        continue;
                    pred.next.compareAndSet(curr, succ, false, false);
                    return true;
                }
            }
        }

        @Override
        public boolean contains(E x) {
            boolean[] marked = {false};
            int key = x.hashCode();
            Nod<E> curr = head;
            while (curr.key < key) {
                curr = curr.next.getReference();
                Nod<E> succ = curr.next.get(marked);
            }
            return (curr.key == key && !marked[0]);
        }

        Window<E> find(Nod<E> head, int key) {
            Nod<E> pred, curr, succ;
            boolean[] marked = {false};
            boolean snip;
            retry:
            while (true) {
                pred = head;
                curr = pred.next.getReference();
                while (true) {
                    succ = curr.next.get(marked);
                    while (marked[0]) {
                        snip = pred.next.compareAndSet(curr, succ, false, false);
                        if (!snip) continue retry;
                        curr = succ;
                        succ = curr.next.get(marked);
                    }
                    if (curr.key >= key)
                        return new Window<>(pred, curr);
                    pred = curr;
                    curr = succ;
                }
            }
        }

    }

    /**
     * 根据 key 从小到大的有序链表
     */
    static class Node<E> {
        E item;
        int key;
        volatile Node<E> next;
        volatile boolean marked = false;

        public Node(int key) {
            this.key = key;
        }

        public Node(E item) {
            this.item = item;
            this.key = item.hashCode();
        }

        private final Lock lock = new ReentrantLock();

        /**
         * 用于细粒度同步
         */
        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }
    }

    /**
     * 惰性同步
     * 相比于 惰性同步，消除了 contains 的加锁逻辑。
     * 对于一个集合来说，contains 的查询可能很频繁，会称为瓶颈。
     */
    static class LazyList<E> implements Set<E> {


        private final Node<E> head;

        LazyList(E item) {
            head = new Node<>(Integer.MIN_VALUE);
            head.next = new Node<>(Integer.MAX_VALUE);
        }

        @Override
        public boolean add(E x) {
            int key = x.hashCode();
            while (true) {
                Node<E> pred = head;
                Node<E> curr = head.next;
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next;
                }
                pred.lock();
                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (curr.key == key) return false;
                            Node<E> n = new Node<>(x);
                            n.next = curr;
                            pred.next = n;
                            return true;
                        }
                    } finally {
                        curr.unlock();
                    }
                } finally {
                    pred.unlock();
                }
            }
        }

        boolean validate(Node<E> pred, Node<E> curr) {
            return !pred.marked && !curr.marked && pred.next == curr;
        }

        @Override
        public boolean remove(E x) {
            int key = x.hashCode();
            while (true) {
                Node<E> pred = head;
                Node<E> curr = head.next;
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next;
                }
                pred.lock();
                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (curr.key == key) {
                                curr.marked = true;
                                pred.next = curr.next;
                                return true;
                            }
                            return false;
                        }
                    } finally {
                        curr.unlock();
                    }
                } finally {
                    pred.unlock();
                }
            }
        }

        @Override
        public boolean contains(E x) {
            int key = x.hashCode();
            Node<E> curr = head;
            while (curr.key < key)
                curr = curr.next;
            return curr.key == key && !curr.marked;
        }
    }

    /**
     * 乐观同步
     * 遍历队列的过程不上锁。
     * 等到需要操作的时候上锁，上锁成功后，先校验是否没有被更改过，如果被更改过则重试。
     */
    static class OptimisticList<E> implements Set<E> {

        private final Node<E> head;

        OptimisticList(E item) {
            head = new Node<>(Integer.MIN_VALUE);
            head.next = new Node<>(Integer.MAX_VALUE);
        }

        @Override
        public boolean add(E x) {
            int key = x.hashCode();
            while (true) {
                Node<E> pred = head;
                Node<E> curr = pred.next;
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next;
                }
                pred.lock();
                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (curr.key == key) return false;
                            Node<E> node = new Node<>(x);
                            node.next = curr;
                            pred.next = node;
                            return true;
                        }
                    } finally {
                        curr.unlock();
                    }
                } finally {
                    pred.unlock();
                }
            }
        }

        boolean validate(Node<E> pred, Node<E> curr) {
            for (Node<E> p = head.next; p.key <= pred.key; p = p.next) {
                if (p == pred && p.next == curr) return true;
            }
            return false;
        }

        @Override
        public boolean remove(E x) {
            int key = x.hashCode();
            while (true) {
                Node<E> pred = head;
                Node<E> curr = pred.next;
                while (curr.key < key) {
                    pred = pred.next;
                    curr = curr.next;
                }
                pred.lock();
                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (key == curr.key) {
                                pred.next = curr.next;
                                return true;
                            }
                            return false;
                        }
                    } finally {
                        curr.unlock();
                    }
                } finally {
                    pred.unlock();
                }
            }
        }

        @Override
        public boolean contains(E x) {
            int key = x.hashCode();
            while (true) {
                Node<E> pred = head;
                Node<E> curr = pred.next;
                while (curr.key < key) {
                    pred = pred.next;
                    curr = curr.next;
                }
                pred.lock();
                curr.lock();
                try {
                    if (validate(pred, curr)) {
                        return curr.key == key;
                    }
                } finally {
                    pred.unlock();
                    curr.unlock();
                }
            }
        }
    }

    /**
     * 细粒度同步
     * 只在 pred、curr 结点上锁
     */
    static class FineGrained<E> implements Set<E> {

        private final Node<E> head;
        private final Lock lock = new ReentrantLock();

        public FineGrained() {
            head = new Node<>(Integer.MIN_VALUE);
            head.next = new Node<>(Integer.MAX_VALUE);
        }

        @Override
        public boolean add(E x) {
            int key = x.hashCode();
            head.lock();
            Node<E> pred = head;
            try {
                Node<E> curr = pred.next;
                curr.lock();
                try {
                    // 交叉上锁
                    while (curr.key < key) {
                        pred.unlock();
                        pred = curr;
                        curr = curr.next;
                        curr.lock();
                    }
                    if (curr.key == key) {
                        return false;
                    }
                    Node<E> node = new Node<>(x);
                    node.next = curr;
                    pred.next = node;
                    return true;
                } finally {
                    curr.unlock();
                }
            } finally {
                pred.unlock();
            }
        }

        @Override
        public boolean remove(E x) {
            int key = x.hashCode();
            head.lock();
            Node<E> pred = head;
            try {
                Node<E> curr = pred.next;
                curr.lock();
                try {
                    while (curr.key < key) {
                        pred.unlock();
                        pred = curr;
                        curr = curr.next;
                        curr.lock();
                    }
                    if (curr.key == key) {
                        pred.next = curr.next;
                        return true;
                    }
                    return false;
                } finally {
                    curr.unlock();
                }
            } finally {
                pred.unlock();
            }
        }

        @Override
        public boolean contains(E x) {
            int key = x.hashCode();
            head.lock();
            Node<E> pred = head;
            try {
                Node<E> curr = pred.next;
                curr.lock();
                try {
                    while (curr.key < key) {
                        pred.unlock();
                        pred = curr;
                        curr = curr.next;
                        curr.lock();
                    }
                    return curr.key == key;
                } finally {
                    curr.unlock();
                }
            } finally {
                pred.unlock();
            }
        }
    }

    /**
     * 粗粒度同步技术
     */
    static class CoarseGrained<E> implements Set<E> {
        private final Node<E> head;
        private final Lock lock = new ReentrantLock();

        public CoarseGrained() {
            head = new Node<>(Integer.MIN_VALUE);
            head.next = new Node<>(Integer.MAX_VALUE);
        }

        @Override
        public boolean add(E x) {
            Node<E> pred, curr;
            int key = x.hashCode();
            lock.lock();
            try {
                pred = head;
                curr = pred.next;
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next;
                }
                if (key == curr.key) {
                    return false;
                } else {
                    Node<E> node = new Node<>(curr.item);
                    node.next = curr;
                    pred.next = node;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean remove(E x) {
            Node<E> pred, curr;
            int key = x.hashCode();
            lock.lock();
            try {
                pred = head;
                curr = pred.next;
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next;
                }
                if (key == curr.key) {
                    pred.next = curr.next;
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean contains(E x) {
            Node<E> curr;
            int key = x.hashCode();
            lock.unlock();
            try {
                curr = head.next;
                while (curr.key < key) {
                    curr = curr.next;
                }
                return key == curr.key;
            } finally {
                lock.unlock();
            }
        }
    }

    interface Set<T> {
        boolean add(T x);

        boolean remove(T x);

        boolean contains(T x);
    }
}
