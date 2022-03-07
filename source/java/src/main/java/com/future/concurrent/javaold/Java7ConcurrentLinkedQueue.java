package com.future.concurrent.javaold;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("all")
public class Java7ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E> {

    private static class Node<E> {
        private volatile E item;
        private volatile Node<E> next;

        Node(E item) {
            // Piggyback on imminent casNext()
            lazySetItem(item);
        }

        E getItem() {
            return item;
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void setItem(E val) {
            item = val;
        }

        void lazySetItem(E val) {
            UNSAFE.putOrderedObject(this, itemOffset, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        Node<E> getNext() {
            return next;
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long nextOffset;
        private static final long itemOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                UNSAFE = (Unsafe) theUnsafe.get(null);
                nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
                itemOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("item"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 一个节点，从该节点出发，第一个活着的（非删除的）节点（如果有的话）。可以在 O(1) 时间内到达。
     */
    private transient volatile Node<E> head = new Node<>(null);

    /**
     * 在O(1) 时间内可以到达列表中最后一个节点（即 node.next == null 的唯一节点）的节点。
     */
    private transient volatile Node<E> tail = head;

    /**
     * 如果与 "真实"位置的链接少于 HOPS，我们就懒得更新头或尾指针。 我们假设 volatile 写比 volatile 读要昂贵得多。
     */
    private static final int HOPS = 1;

    public Java7ConcurrentLinkedQueue() {
    }

    public Java7ConcurrentLinkedQueue(Collection<? extends E> c) {
        for (E e : c)
            add(e);
    }

    public boolean add(E e) {
        return offer(e);
    }


    final void  updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p)) {
            h.lazySetNext(h);
        }
    }

    final Node<E> succ(Node<E> p) {
        Node<E> next = p.getNext();
        return (p == next) ? head : next;
    }

    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> n = new Node<>(e);
        retry:
        for (; ; ) {
            Node<E> t = tail;
            Node<E> p = t;
            for (int hops = 0; ; hops++) {
                Node<E> next = succ(p);
                if (next != null) {
                    if (hops > HOPS && t != tail)
                        continue retry;
                    p = next;
                } else if (p.casNext(null, n)) {
                    if (hops >= HOPS)
                        casTail(t, n);
                    return true;
                } else {
                    p = succ(p);
                }
            }
        }
    }

    public E poll() {
        Node<E> h = head;
        Node<E> p = h;
        for (int hops = 0; ; hops++) {
            E item = p.getItem();
            if (item != null && p.casItem(item, null)) {
                if (hops >= HOPS) {
                    Node<E> q = p.getNext();
                    updateHead(h, (q != null) ? q : p);
                }
                return item;
            }
            Node<E> next = succ(p);
            if (next == null) {
                updateHead(h, p);
                break;
            }
            p = next;
        }
        return null;
    }

    public E peek() {
        Node<E> h = head;
        Node<E> p = h;
        E item;
        for (; ; ) {
            item = p.getItem();
            if (item != null)
                break;
            Node<E> next = succ(p);
            if (next == null)
                break;
            p = next;
        }
        updateHead(h, p);
        return item;
    }

    Node<E> first() {
        Node<E> h = head;
        Node<E> p = h;
        Node<E> result;
        for (; ; ) {
            E item = p.getItem();
            if (item != null) {
                result = p;
                break;
            }
            Node<E> next = succ(p);
            if (next == null) {
                result = null;
                break;
            }
            p = next;
        }
        updateHead(h, p);
        return result;
    }

    public boolean isEmpty() {
        return first() == null;
    }

    @Override
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            if (p.getItem() != null) {
                if (++count == Integer.MAX_VALUE)
                    break;
            }
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.getItem();
            if (o.equals(item))
                return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        Node<E> pred = null;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.getItem();
            if (o.equals(item) && p.casItem(item, null)) {
                Node<E> next = succ(p);
                if (pred != null && next != null)
                    pred.casNext(p, next);
                return true;
            }
            pred = p;
        }
        return false;
    }

    public Object[] toArray() {
        // Use ArrayList to deal with resizing.
        ArrayList<E> al = new ArrayList<>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.getItem();
            if (item != null)
                al.add(item);
        }
        return al.toArray();
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /**
         * Next node to return item for.
         */
        private Node<E> nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Node of the last returned item, to support remove.
         */
        private Node<E> lastRet;

        Itr() {
            advance();
        }

        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         */
        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (; ; ) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.getItem();
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {
                    // skip over nulls
                    Node<E> next = succ(p);
                    if (pred != null && next != null)
                        pred.casNext(p, next);
                    p = next;
                }
            }
        }

        public boolean hasNext() {
            return nextNode != null;
        }

        public E next() {
            if (nextNode == null) throw new NoSuchElementException();
            return advance();
        }

        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            // rely on a future traversal to relink.
            l.setItem(null);
            lastRet = null;
        }
    }

    private static final long headOffset;
    private static final long tailOffset;

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private void lazySetHead(Node<E> val) {
        UNSAFE.putOrderedObject(this, headOffset, val);
    }

    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            headOffset = UNSAFE.objectFieldOffset(Java7ConcurrentLinkedQueue.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(Java7ConcurrentLinkedQueue.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
























