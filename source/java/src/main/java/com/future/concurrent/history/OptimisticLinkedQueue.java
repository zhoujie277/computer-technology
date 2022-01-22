package com.future.concurrent.history;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 该算法出自论文
 * An optimistic approach to lock-free fifo queues
 * by Edya Ladan-Mozes, Nir Shavit
 */
@SuppressWarnings("unused")
public class OptimisticLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {

    private static class Node<E> {
        private volatile E item;
        private volatile Node<E> next;
        private volatile Node<E> prev;

        Node(E x) {
            item = x;
            next = null;
            prev = null;
        }

        Node(E x, Node<E> n) {
            item = x;
            next = n;
            prev = null;
        }

        public E getItem() {
            return item;
        }

        public void setItem(E item) {
            this.item = item;
        }

        public void setNext(Node<E> next) {
            this.next = next;
        }

        public Node<E> getNext() {
            return next;
        }

        public void setPrev(Node<E> prev) {
            this.prev = prev;
        }

        public Node<E> getPrev() {
            return prev;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<OptimisticLinkedQueue, Node> tailUpdater = AtomicReferenceFieldUpdater.newUpdater(OptimisticLinkedQueue.class, Node.class, "tail");

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<OptimisticLinkedQueue, Node> headUpdater = AtomicReferenceFieldUpdater.newUpdater(OptimisticLinkedQueue.class, Node.class, "head");

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return tailUpdater.compareAndSet(this, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return headUpdater.compareAndSet(this, cmp, val);
    }

    /**
     * Pointer to the head node, initialized to a dummy node.
     * The first actual node is at head.getPrev().
     */
    private transient volatile Node<E> head = new Node<>(null, null);

    /**
     * Pointer to last node on list
     */
    private transient volatile Node<E> tail = head;

    /**
     * Creates a <tt> ConcurrentLinkedQueue</tt> that is initially empty.
     */
    public OptimisticLinkedQueue() {
    }


    /**
     * Enqueues the specified element at the tail of the queue.
     */
    @Override
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> n = new Node<>(e, null);
        for (; ; ) {
            Node<E> t = tail;
            n.setNext(t);
            if (casTail(t, n)) {
                t.setPrev(n);
                return true;
            }
        }
    }

    /**
     * Dequeues an element from the queue. After a successful casHead,
     * the prev and next pointers of the dequeued node are set to null
     * to allow garbage collection.
     */
    @Override
    public E poll() {
        for (; ; ) {
            Node<E> h = head;
            Node<E> t = tail;
            Node<E> first = h.getPrev();
            if (h == head) {
                if (h != t) {
                    if (first == null) {
                        continue;
                    }
                    E item = first.getItem();
                    if (casHead(h, first)) {
                        h.setNext(null);
                        h.setPrev(null);
                        return item;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Fixing the backwords pointers when needed
     */
    private void fixList(Node<E> t, Node<E> h) {
        Node<E> curNodeNext;
        Node<E> curNode = t;
        while (h == this.head && curNode != h) {
            curNodeNext = curNode.getNext();
            curNodeNext.setPrev(curNode);
            curNode = curNodeNext.getNext();
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
    public E peek() {
        return null;
    }
}
