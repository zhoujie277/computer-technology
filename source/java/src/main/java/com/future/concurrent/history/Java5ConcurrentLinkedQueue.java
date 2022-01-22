package com.future.concurrent.history;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 请注意，与大多数集合不同，大小方法不是一个恒定时间操作。
 * 由于这些队列的异步性，确定当前的元素数 元素的数量需要对元素进行遍历。
 */
@SuppressWarnings({"unused", "rawtypes", "all"})
public class Java5ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E> {

    private static class Node<E> {
        private volatile E item;
        private volatile Node<E> next;

        private static final AtomicReferenceFieldUpdater<Node, Node> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");
        private static final AtomicReferenceFieldUpdater<Node, Object> itemUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Object.class, "item");

        Node(E x) {
            item = x;
        }

        Node(E x, Node<E> n) {
            item = x;
            next = n;
        }

        E getItem() {
            return item;
        }

        boolean casItem(E cmp, E val) {
            return itemUpdater.compareAndSet(this, cmp, val);
        }

        void setItem(E val) {
            itemUpdater.set(this, val);
        }

        Node<E> getNext() {
            return next;
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return nextUpdater.compareAndSet(this, cmp, val);
        }

        public void setNext(Node<E> next) {
            nextUpdater.set(this, next);
        }
    }

    private static final AtomicReferenceFieldUpdater<Java5ConcurrentLinkedQueue, Node> tailUpdater = AtomicReferenceFieldUpdater.newUpdater(Java5ConcurrentLinkedQueue.class, Node.class, "tail");
    private static final AtomicReferenceFieldUpdater<Java5ConcurrentLinkedQueue, Node> headUpdater = AtomicReferenceFieldUpdater.newUpdater(Java5ConcurrentLinkedQueue.class, Node.class, "head");


    private boolean casTail(Node<E> cmp, Node<E> val) {
        return tailUpdater.compareAndSet(this, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return headUpdater.compareAndSet(this, cmp, val);
    }

    /**
     * Pointer to header node, initialized to a dummy node.
     * The first actual node is at head.getNext().
     */
    private transient volatile Node<E> head = new Node<>(null, null);

    /**
     * Pointer to last node on list
     **/
    private transient volatile Node<E> tail = head;

    public Java5ConcurrentLinkedQueue() {
    }

    public Java5ConcurrentLinkedQueue(Collection<? extends E> c) {
        for (Iterator<? extends E> it = c.iterator(); it.hasNext(); )
            add(it.next());
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> n = new Node<>(e, null);
        for (; ; ) {
            Node<E> t = tail;
            Node<E> s = t.getNext();
            if (t == tail) {
                if (s == null) {
                    if (t.casNext(null, n)) {
                        casTail(t, n);
                        return true;
                    }
                } else {
                    casTail(t, s);
                }
            }
        }
    }

    @Override
    public E poll() {
        for (; ; ) {
            Node<E> h = head;
            Node<E> t = tail;
            Node<E> first = h.next;
            if (h == head) {
                if (h == t) {
                    if (first == null)
                        return null;
                    casTail(t, first);
                } else if (casHead(h, first)) {
                    E item = first.item;
                    if (item != null) {
                        first.setItem(null);
                        return item;
                    }
                    // else skip over deleted item, continue loop,
                }
            }
        }
    }


    @Override
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = p.getNext()) {
            if (p.getItem() != null) {
                if (++count == Integer.MAX_VALUE)
                    break;
            }
        }
        return count;
    }

    @Override
    public E peek() {
        // same as poll except don't remove item
        for (; ; ) {
            Node<E> h = head;
            Node<E> t = tail;
            Node<E> first = h.getNext();
            if (h == head) {
                if (h == t) {
                    if (first == null)
                        return null;
                    else
                        casTail(t, first);
                } else {
                    E item = first.getItem();
                    if (item != null)
                        return item;
                    else
                        casHead(h, first);
                }
            }
        }
    }

    Node<E> first() {
        for (; ; ) {
            Node<E> h = head;
            Node<E> t = tail;
            Node<E> first = h.getNext();
            if (h == head) {
                if (h == t) {
                    if (first == null)
                        return null;
                } else {
                    if (first.getItem() != null)
                        return first;
                    else
                        casHead(h, first);
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return first() == null;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = p.getNext()) {
            E item = p.getItem();
            if (item != null && o.equals(item))
                return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = p.getNext()) {
            E item = p.getItem();
            if (item != null && o.equals(item) && p.casItem(item, null))
                return true;
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

}
