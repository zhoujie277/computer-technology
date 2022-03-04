package com.future.concurrent.history;

import sun.misc.Unsafe;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Field;

/**
 * 无锁队列
 * 1996 年 Michael-Scott 无阻塞插入算法。
 * 参照 Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue Algorithms
 * 译文在 computer_technology/docs/java 目录下
 *
 * @author future
 */
@ThreadSafe
@SuppressWarnings("unused")
class MichealScottQueue<E> {

    /**
     * 注意：Java 由于 GC 的存在，已出队的结点不会再马上回到队列中，由此不会出现 ABA 问题。
     * 此处之所以加上计数器，仅仅是为了演示算法的完整性。
     */
    private static class Node<E> {
        E item;
        volatile Node<E> next;
        int stamp;

        public Node(E item) {
            this.item = item;
        }

        @SuppressWarnings("SameParameterValue")
        boolean casNext(Node<E> cmp, Node<E> val) {
            return unsafe.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        private static final Unsafe unsafe;
        private static final long nextOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(Unsafe.class);
                nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile Node<E> head;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile Node<E> tail;

    public MichealScottQueue() {
        head = tail = new Node<>(null);
    }

    /**
     * 入队操作只原子更新 tail 结点。从而保证 head 结点无竞争。
     */
    public void enqueue(E item) {
        Node<E> node = new Node<>(item);
        for (; ; ) {
            Node<E> curTail = tail;
            Node<E> tailNext = curTail.next;
            if (curTail == tail) {
                // 再次检查 curTail == tail，如果成功，说明线程本地变量 curTail 和 tailNext 状态一致。
                if (tailNext != null) {
                    // tailNext != null, 说明队列处于中间状态 (tail 指向队列倒数第二个节点），则尝试推进尾结点
                    compareAndSetTail(curTail, tailNext);
                    // 此时不可以返回，不管成功与否，都需要在队列恢复稳定状态之后，继续将自己的 node 插入队列。
                } else {
                    // 如果 tailNext == null, 说明处于稳定状态 (tail 指向队列最后一个节点)，则先推进 next 节点（插入新结点）
                    if (curTail.casNext(null, node)) {
                        // 插入操作成功，尝试推进尾结点。
                        compareAndSetTail(curTail, node);
                        // 不管推进尾结点成功或失败，都可以返回
                        // 若成功，说明无竞争，执行良好。
                        // 若不成功，说明更新 tail 节点有竞争，且被另外线程更新成功了，无须再更新。
                        return;
                    }
                }
            }
        }
    }

    /**
     * 出队操作只更新 head 结点
     */
    public E dequeue() {
        for (; ; ) {
            Node<E> curHead = head;
            Node<E> curTail = tail;
            Node<E> headNext = curHead.next;
            if (curHead == head) {
                // 再次检查 curHead == head，说明线程本地变量 curHead 和 headNext 状态一致。
                if (curTail == curHead) {
                    if (headNext == null) {
                        // 说明队列为空，且处于稳定状态。
                        return null;
                    }
                    // 队列处于插入过程中的中间状态，尝试推进尾结点，不管成功与否，再次重试出队操作。
                    compareAndSetTail(curTail, headNext);
                } else {
                    // 队列不为空，只需关心 head 结点
                    E result = headNext.item;
                    if (compareAndSetHead(curHead, headNext)) {
                        // java 中由于 GC，无需手动释放结点。如果非 GC 的语言，则需要 free(curHead)
                        curHead.next = curHead;// help GC
                        curHead.item = null; // help GC
                        return result;
                    }
                    // casHead 失败，则重试
                }
            }
        }
    }

    private boolean compareAndSetHead(Node<E> cmp, Node<E> val) {
        return unsafe.compareAndSwapObject(this, headOffset, cmp, val);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean compareAndSetTail(Node<E> cmp, Node<E> val) {
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
            headOffset = unsafe.objectFieldOffset(MichealScottQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(MichealScottQueue.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }


}
