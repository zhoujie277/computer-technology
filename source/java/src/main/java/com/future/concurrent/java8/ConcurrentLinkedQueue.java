package com.future.concurrent.java8;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 *
 */
@SuppressWarnings("unused")
class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E> {

    /**
     * 请注意，与此包中的大多数非阻塞算法一样，此
     * 实现依赖于这样一个事实：在垃圾收集系统中，由于回收的节点，不存在ABA问题的可能性，
     * 因此不需要使用“计数指针”或在非 GC 设置中使用的版本中看到的相关技术。
     * <p>
     * 只有一个（最后一个）节点的 next 引用为空，这是排队时的情况。
     * 最后一个节点可以在O（1）时间内从尾部到达，但尾部仅仅是一个优化——它也可以在O（N）时间内从头部到达。
     * <p>
     * 队列中包含的元素是节点中可从 head 访问的非空项。
     * 将节点的项引用封装为 null 会自动将其从队列中移除。
     * 即使在导致 head 前进的同时修改的情况下，head 中所有元素的可达性也必须保持真实。
     * 由于创建了迭代器或仅仅是丢失了时间片的 poll（），已退出队列的节点可能会无限期地继续使用。
     * <p>
     * 上面可能暗示所有节点都可以从前一个已出队的节点进行 GC 访问。
     * 这会造成两个问题：
     * 1. 允许恶意迭代器导致无限内存保留
     * 2. 如果某个节点在使用期间是长期存在的，则会导致旧节点与新节点的跨代链接，这一点各代 GCs 很难处理，从而导致重复的主要收集。
     * <p>
     * 然而，只有未删除的节点才需要能够从出列的节点访问，并且访问性不一定必须是 GC 所理解的那种。
     * 我们使用的技巧是将刚出列的节点链接到自身。这样的自我链接隐含着向头部前进的意思。
     * <p>
     * 头部和尾部都允许滞后。事实上，不能每次都更新它们是一个显著的优化（更少的情况）。
     * 与LinkedTransferQueue（参见该类的内部文档）一样，我们使用两个松弛阈值；
     * 也就是说，当当前指针距离第一个/最后一个节点两步或更多步时，我们更新 head/tail。
     * <p>
     * 由于 head 和 tail 是同时独立更新的，tail 有可能落后于 head（为什么不）？
     * <p>
     * 将节点的项引用封装为null会自动从队列中删除元素。迭代器跳过包含空项的节点。
     * 该类以前的实现在 poll（）和remove（Object）之间存在竞争，相同的元素似乎可以通过两个并发操作成功删除。
     * 方法 remove（Object）也会延迟地取消已删除节点的链接，但这只是一种优化。
     * <p>
     * 在构造节点时（在排队之前），我们通过使用 Unsafe.putObject 的方法替代普通写入 避免为 volatile 的写入项付费。
     * 这使得排队的成本为“one-and-a-half”个案例。
     * <p>
     * 头部和尾部都可能指向或不指向具有非空项的节点。
     * 如果队列为空，则所有项目当然必须为空。
     * 创建时，head和tail都引用带有null项的虚拟节点。
     * 头部和尾部都只使用CAS进行更新，所以它们永远不会退化，尽管这只是一个优化。
     */
    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        // 构造一个新节点。使用轻松写入，因为只有在通过casNext发布后才能看到该项。
        Node(E item) {
            unsafe.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return unsafe.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return unsafe.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            unsafe.putOrderedObject(this, nextOffset, val);
        }

        private static final Unsafe unsafe;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                itemOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("item"));
                nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }

    }

    private volatile Node<E> head;

    private volatile Node<E> tail;

    public ConcurrentLinkedQueue() {
        head = tail = new Node<>(null);
    }

    public ConcurrentLinkedQueue(Collection<? extends E> collection) {
        Node<E> h = null, t = null;
        for (E e : collection) {
            Node<E> newNode = new Node<>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null)
            h = t = new Node<>(null);
        head = h;
        tail = t;
    }

    public boolean add(E e) {
        return offer(e);
    }

    // 尝试将 head CAS to p。如果成功，请将 old head 重新指向自身，作为下面 succ 的标记。
    final void updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p))
            h.lazySetNext(h);
    }

    /**
     * 返回 p 的后继节点，如果 p.next 已链接到自身，则返回 head 节点，
     * 仅当使用现在不在列表中的过时指针进行遍历时，才会返回 true。
     */
    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return (p == next) ? head : next;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    /**
     * 在此队列末尾插入指定的元素。由于队列是无界的，因此此方法永远不会返回 false。
     */
    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<>(e);
        for (Node<E> t = tail, p = t; ; ) {
            Node<E> q = p.next;
            if (q == null) {
                // p is last node
                if (p.casNext(null, newNode)) {
                    if (p != t)
                        casTail(t, newNode);
                    return true;
                }
                // Lost CAS race to another thread; re-read next
            } else if (p == q) {
                // middle state. p is second of last node
                p = (t != (t = tail)) ? t : head;
            } else {
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }

    @Override
    public E poll() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                E item = p.item;

                if (item != null && p.casItem(item, null)) {
                    if (p != h)
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    return item;
                } else if ((q = p.next) == null) {
                    updateHead(h, p);
                    return null;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    @Override
    public E peek() {
        return null;
    }

    private static void checkNotNull(Object v) {
        if (v == null) throw new NullPointerException();
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return unsafe.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return unsafe.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private static final Unsafe unsafe;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
            headOffset = unsafe.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) {
        Node<String> node = new Node<>("hello world");
        System.out.println(node.item);
    }
}
