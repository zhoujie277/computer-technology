package com.future.concurrent.history;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 该算法出自
 * Nonblocking Concurrent Data Structures with Condition Synchronization
 * 和 2009 年 Doug lea Scalable Synchronous Queues.
 * <p>
 * 此处仅仅演示算法的可行性。
 * 本处算法实现采用简单策略：无取消、无中断、无阻塞（简单自旋）
 * 且为了方便理解，不合并 push/pop、enqueue/dequeue 方法。
 * <p>
 * 同步队列的基本需求:
 * 1. 入队操作（生产者），分为两种情况
 * *  1. 如果队列为空或者队列是生产者队列，则排队等待。
 * *  2. 如果队列不为空且是消费者队列，则将数据交给队列中的某一个消费者，然后使其出队。
 * 2. 出队操作（消费者），分为两种情况
 * *  1. 如果队列为空或者队列是消费者队列，则排队等待。
 * *  2. 如果队列不为空且是生成者队列，则尝试取出一个生产者获取数据，然后返回。
 *
 * @author future
 */
@SuppressWarnings("unused")
public class SimpleSynchronousQueue {

    /**
     * 双栈结构
     * <p>
     * 所谓的双栈，是指该栈既可以表示请求栈，也可以表示数据栈。但它同时存在两种结点交叉。(除非匹配过程)
     * <p>
     * 为了表示代码的可重用，引入了 match 结点
     * 引入 match 结点，而不直接使用 item 作为匹配的原因是
     * 引入 match 结点之后，pop/push 的方法体几乎全部是一样的。
     * <p>
     * 下面 3 点是本代码的核心逻辑
     * <p>
     * 1. 当队列为空或者队列和操作线程的动作类型 (push<->data), (pop<->request) 一致时，进行入队操作，然后等待 match 结点匹配。
     * 2. 当队列类型 (根据头结点判断) 和操作线程的动作类型不一致时，且不为匹配状态，则进行尝试匹配动作。
     * 3. 当队列处于正在匹配状态中，无论哪个线程正在执行，都先帮忙推进匹配过程，再做自己的事情。
     */
    @SuppressWarnings("StatementWithEmptyBody")
    static class DualStack<E> {
        // 表示结点是请求结点（生产者线程）
        static final int REQUEST = 0;
        // 表示结点是数据节点（消费者线程）
        static final int DATA = 1;
        // 表示结点是正在被满足请求，但还未满足请求的中间状态。
        static final int FULFILLING = 2;

        /**
         * 表示栈中结点
         */
        private static class SNode<E> {
            // 引入 match 结点，而不直接使用 item 作为匹配的原因是
            // 引入 match 结点之后，pop/push 的方法体几乎全部是一样的。
            volatile SNode<E> match;
            SNode<E> next;
            E item;
            int mode;


            boolean casMatch(SNode<E> cmp, SNode<E> update) {
                return unsafe.compareAndSwapObject(this, matchOffset, cmp, update);
            }

            private static final Unsafe unsafe;
            private static final long matchOffset;


            static {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                    matchOffset = unsafe.objectFieldOffset(SNode.class.getDeclaredField("match"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new Error(e);
                }
            }

            public SNode(E item) {
                this.item = item;
            }

            public SNode(E item, int mode) {
                this.item = item;
                this.mode = mode;
            }

        }

        private volatile SNode<E> head;

        /**
         * 生产者线程，向栈中推送数据
         */
        void push(E item) {
            // 三种情况：
            // 1. 当栈为空或者栈是数据栈，则更新栈顶，继续等待。
            // 2. 当栈不为空且是请求栈，则传送数据给头结点并唤醒消费者线程。
            // 3. 当栈不为空且是请求栈，但正在有 push 线程传送数据，表示此时发生争用，则需要帮助推进传送数据操作再继续自己的操作。
            // 判断栈类型的方法是检查头结点。
            // 所以，只要是有结点入栈，都得生成自己状态的结点并更新 head 结点。表示整个栈的状态。
            SNode<E> s = new SNode<>(item);
            for (; ; ) {
                SNode<E> h = head;
                if (h == null || h.mode == DATA) {
                    s.mode = DATA;
                    s.next = h;
                    if (!casHead(h, s)) continue;
                    // 忙等待
                    while (s.match == null) ;
                    h = head;
                    if (h != null && s == h.next) {
                        casHead(h, s.next);  // 仅仅是一个优化，帮忙推进 fulfill 过程更新 head 结点。
                    }
                    return;
                } else if (h.mode == REQUEST) {
                    s.mode = FULFILLING | DATA;
                    s.next = h;
                    if (!casHead(h, s)) continue;
                    // 配对成功，更新 item 并出栈配对成功的两个结点
                    h = s.next;
                    SNode<E> hn = h.next;
                    h.casMatch(null, s);
                    casHead(s, hn);
                    return;
                } else {
                    // is fulfilling 正在被满足的状态，不更新 head 节点，
                    // 推进完成 fulfill 过程，然后再开始自己的动作。
                    SNode<E> n = h.next;
                    SNode<E> nn = n.next;
                    n.casMatch(null, h);
                    casHead(h, nn);
                }
            }
        }

        E pop() {
            // 三种情况：
            // 1. 当栈为空或者栈是请求栈，则更新栈顶，继续等待。
            // 2. 当栈不为空且是数据栈，则获取数据并唤醒生产者线程。
            // 3. 当栈不为空且是数据栈，但正在有 push 线程传送数据，表示此时发生争用，则需要帮助推进传送数据操作再继续自己的操作。
            // 判断栈类型的方法是检查头结点。
            // 所以，只要是有结点入栈，都得生成自己状态的结点并更新 head 结点。表示整个栈的状态。
            SNode<E> s = new SNode<>(null);
            for (; ; ) {
                SNode<E> h = head;
                if (h == null || h.mode == REQUEST) {
                    s.mode = REQUEST;
                    s.next = h;
                    if (!casHead(h, s)) continue;
                    // 忙等待
                    while (s.match == null) ;
                    h = head;
                    if (h != null && s == h.next) {
                        casHead(s, h.next); // 仅仅是一个优化，帮忙推进 fulfill 过程更新 head 结点。
                    }
                    return s.match.item;
                } else if (h.mode == DATA) {
                    s.mode = REQUEST | FULFILLING;
                    s.next = h;
                    if (!casHead(h, s)) continue;
                    // 配对成功，更新 item 并出栈配对成功的两个结点
                    h = s.next;
                    SNode<E> hn = h.next;
                    h.casMatch(null, s);
                    casHead(s, hn);
                    return h.item;
                } else {
                    // is fulfilling 正在被满足的状态，不更新 head 节点，
                    // 推进完成 fulfill 过程，然后再开始自己的动作。
                    SNode<E> n = h.next;
                    SNode<E> nn = n.next;
                    n.casMatch(null, h);
                    casHead(h, nn);
                }
            }
        }

        boolean casHead(SNode<E> h, SNode<E> nh) {
            return unsafe.compareAndSwapObject(this, headOffset, h, nh);
        }

        static final Unsafe unsafe;
        static final long headOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                headOffset = unsafe.objectFieldOffset(DualStack.class.getDeclaredField("head"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 此处引入虚结点，参照 MichaelScottQueue 算法
     * <p>
     * 双队列不要和栈一样，使用 match 结点，因为引入 match 结点必然导致多余的插入操作。
     * 不论是头插还是尾插，都会增加并发的复杂度。
     * 所以双队列只会有两种情况。
     * 1. 该队列是请求队列（消费者队列）
     * 2. 该队列是数据队列（生产者队列）
     * <p>
     * 判定队列为空的方法: head == tail
     * 判定队列是否生产者队列: tail.isData();
     */
    @SuppressWarnings("StatementWithEmptyBody")
    static class DualQueue<E> {
        private static final int REQUEST = 0;
        private static final int DATA = 1;

        private static class QNode<E> {
            volatile QNode<E> next;
            volatile E item;
            final int mode;

            public QNode(E item, int mode) {
                this.item = item;
                this.mode = mode;
            }

            boolean casNext(QNode<E> cmp, QNode<E> update) {
                return unsafe.compareAndSwapObject(this, nextOffset, cmp, update);
            }

            boolean casItem(E cmp, E update) {
                return unsafe.compareAndSwapObject(this, itemOffset, cmp, update);
            }

            static final Unsafe unsafe;
            static final long nextOffset;
            static final long itemOffset;

            static {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                    nextOffset = unsafe.objectFieldOffset(QNode.class.getDeclaredField("next"));
                    itemOffset = unsafe.objectFieldOffset(QNode.class.getDeclaredField("item"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }

        private volatile QNode<E> head;
        private volatile QNode<E> tail;

        public DualQueue() {
            // 引入虚结点，为了减小消费者线程和生产者线程的竞争，参照 MichaelScottQueue
            head = tail = new QNode<>(null, -1);
        }

        void enqueue(E item) {
            QNode<E> q = new QNode<>(item, DATA);
            for (; ; ) {
                QNode<E> h = head;
                QNode<E> t = tail;
                if (t == h || t.mode == DATA) {
                    // 如果空，或者是生产者队列
                    QNode<E> n = t.next;
                    if (t == tail) {
                        // 说明本地变量 h 和 n 状态一致
                        if (n == null) {
                            // 说明队列处于稳定状态
                            if (!t.casNext(null, q)) continue;
                            casTail(t, q);
                            // 忙等待
                            while (q.item == item) ;
                            h = head;
                            if (q == h.next) {
                                // 帮忙推进 head
                                casHead(h, q);
                            }
                            return;
                        } else {
                            // 说明队列处于中间状态，则推进中间状态，然后重试
                            casTail(t, n);
                        }
                    }
                } else {
                    // 如果是消费者队列
                    QNode<E> n = h.next;
                    // 读取 h.next 变量到本地后，此处需要防止不一致需要注意以下几点：
                    // 1. 论文此处有 t != tail; 原因不明；笔者认为此处无取消算法加上可能会限制并发。
                    // 2. h != head，必须加上，防止后面操作本地变量 h 和 n 不是有效的状态。
                    // 3. 如果 n 为空，表示 head 可能已更新成功，作为优化。
                    if (h != head || n == null) continue;
                    boolean success = n.casItem(null, item);
                    casHead(h, n);
                    if (success) {
                        // 没有成功则重试。
                        return;
                    }
                }
            }
        }

        E dequeue() {
            // 尝试在队列中增加一个 request 结点
            QNode<E> q = new QNode<>(null, REQUEST);
            for (; ; ) {
                QNode<E> h = head;
                QNode<E> t = tail;
                if (h == t || t.mode == REQUEST) {
                    // 如果空，或者是消费者队列
                    QNode<E> n = t.next;
                    if (t == tail) {
                        // 说明本地变量 t 和 n 状态一致。
                        if (n == null) {
                            // 说明队列处于稳定状态
                            if (!t.casNext(null, q)) continue;
                            casTail(t, q);
                            // 忙等待
                            while (q.item == null) ;
                            h = head;
                            if (q == h.next) {
                                // 帮忙推进 head
                                casHead(h, q);
                            }
                            return q.item;
                        } else {
                            // 说明队列处于中间状态，则推进中间状态，然后重试
                            casTail(t, n);
                        }
                    }
                } else {
                    // 如果是生产者队列
                    QNode<E> n = h.next;
                    // 要求状态一致。
                    if (t != tail || h != head || n == null) continue;
                    E item = n.item;
                    boolean success = n.casItem(item, null);
                    casHead(h, n);
                    if (success) {
                        return item;
                    }
                }
            }
        }

        boolean casTail(QNode<E> cmp, QNode<E> update) {
            return unsafe.compareAndSwapObject(this, tailOffset, cmp, update);
        }

        boolean casHead(QNode<E> cmp, QNode<E> update) {
            return unsafe.compareAndSwapObject(this, headOffset, cmp, update);
        }

        static final Unsafe unsafe;
        static final long headOffset;
        static final long tailOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                headOffset = unsafe.objectFieldOffset(DualQueue.class.getDeclaredField("head"));
                tailOffset = unsafe.objectFieldOffset(DualQueue.class.getDeclaredField("tail"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }
}
















