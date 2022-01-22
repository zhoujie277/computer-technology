package com.future.concurrent.history;

import sun.misc.Unsafe;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 1986 年 Treiber 提出的无锁栈结构。
 * 本示例采用 java unsafe 实现。
 *
 * @author future
 */
@ThreadSafe
@SuppressWarnings("all")
public class TreiberStack<E> {


    /**
     * 下面是 Java 并发编程实战一书中，对 Treiber Stack 的写法
     */
    private static class ConcurrentStack<E> {

        AtomicReference<Node<E>> top = new AtomicReference<>();

        public void push(E item) {
            Node<E> oldTop;
            Node<E> newNode = new Node<>(item);
            do {
                oldTop = top.get();
                newNode.next = oldTop;
            } while (!top.compareAndSet(oldTop, newNode));
        }

        private E pop() {
            Node<E> oldNode, newTop;
            do {
                oldNode = top.get();
                if (oldNode == null) return null;
                newTop = oldNode.next;
            } while (!top.compareAndSet(oldNode, newTop));
            return oldNode.item;
        }
    }

    private static class Node<E> {
        final E item;
        Node<E> next;

        public Node(E item) {
            this.item = item;
        }
    }

    private volatile Node<E> top = null;

    public void push(E item) {
        Node<E> oldTop;
        Node<E> newNode = new Node<>(item);
        do {
            oldTop = top;
            newNode.next = oldTop;
        } while (!casTop(oldTop, newNode));
    }

    private E pop() {
        Node<E> oldNode, newTop;
        do {
            oldNode = top;
            if (oldNode == null) return null;
            newTop = oldNode.next;
        } while (!casTop(oldNode, newTop));
        return oldNode.item;
    }

    private boolean casTop(Node<E> cmp, Node<E> update) {
        return unsafe.compareAndSwapObject(this, topOffset, cmp, update);
    }

    private static final Unsafe unsafe;
    private static final long topOffset;

    static {
        Field theUnsafe = null;
        try {
            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
            topOffset = unsafe.objectFieldOffset(TreiberStack.class.getDeclaredField("top"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

}
