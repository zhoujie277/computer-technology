package com.future.concurrent.history.artof;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 10 章 并行队列和 ABA 问题之 10.7 双重数据结构
 *
 * @author future
 */
@SuppressWarnings("unused")
class TenParallelDualQueue {

    enum NodeType {
        ITEM, RESERVATION
    }

    static class Node<T> {
        volatile NodeType type;
        volatile AtomicReference<T> item;
        volatile AtomicReference<Node<T>> next;

        Node(T myItem, NodeType myType) {
            item = new AtomicReference<>(myItem);
            next = new AtomicReference<>(null);
            type = myType;
        }
    }

    static class SynchronousDualQueue<T> {
        AtomicReference<Node<T>> head, tail;

        SynchronousDualQueue() {
            Node<T> sentinel = new Node<>(null, NodeType.ITEM);
            head = new AtomicReference<>(sentinel);
            tail = new AtomicReference<>(sentinel);
        }

        public void enq(T e) {
            Node<T> offer = new Node<>(e, NodeType.ITEM);
            while (true) {
                Node<T> t = tail.get(), h = head.get();
                if (h == t || h.type == NodeType.ITEM) {
                    Node<T> n = t.next.get();
                    if (t == tail.get()) {
                        if (n != null) {
                            tail.compareAndSet(t, n);
                        } else if (t.next.compareAndSet(null, offer)) {
                            tail.compareAndSet(t, offer);
                            while (offer.item.get() == e) ;
                            h = head.get();
                            if (offer == h.next.get())
                                head.compareAndSet(h, offer);
                            return;
                        }
                    }
                } else {
                    // 配对消除
                    Node<T> n = h.next.get();
                    if (t != tail.get() || h != head.get() || n == null) {
                        continue;
                    }
                    boolean success = n.item.compareAndSet(null, e);
                    head.compareAndSet(h, n);
                    if (success) return;
                }
            }
        }
    }
}
