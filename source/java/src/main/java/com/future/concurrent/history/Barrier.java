package com.future.concurrent.history;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示常见的几种障碍实现技术
 *
 * @author future
 */
@SuppressWarnings("ALL")
@Slf4j
class Barrier {

    public static void main(String[] args) {
        int n = 15;
        TreeBarrier barrier = new TreeBarrier(n, 2);
//        StaticTreeBarrier staticTreeBarrier = new StaticTreeBarrier(n, 2);
    }

    interface IBarrier {
        void await();
    }

    /**
     * 终止检测障碍接口
     */
    interface TDBarrier {
        void setActive(boolean active);

        boolean isTerminated();
    }

    public class SimpleTDBarrier implements TDBarrier {
        AtomicInteger count;

        public SimpleTDBarrier(int n) {
            count = new AtomicInteger(n);
        }

        @Override
        public void setActive(boolean active) {
            if (active) {
                count.getAndIncrement();
            } else {
                count.getAndDecrement();
            }
        }

        @Override
        public boolean isTerminated() {
            return count.get() == 0;
        }
    }

    /**
     * 静态树障碍
     * <p>
     * 算法思想：每个线程一个结点。并且非叶子结点本地自旋计数器阻塞。
     * 每个线程执行完之后，竞争同一个父节点的计数。
     * 父节点计数为 0 之后，解除阻塞。同时向上传递计数。
     * 每个节点向上更新完计数之后，便自旋等待根节点通知。
     * 根节点通过修改 sense 通知所有结点已完成。
     * <p>
     * 代码出处：The art of multiprocessor programming
     */
    static class StaticTreeBarrier implements IBarrier {

        int radix;
        boolean sense;
        Node[] node;
        ThreadLocal<Boolean> threadSense;
        int nodes;

        public StaticTreeBarrier(int size, int myRadix) {
            radix = myRadix;
            nodes = 0;
            node = new Node[size];
            int depth = 0;
            while (size > 1) {
                depth++;
                size = size / radix;
            }
            build(null, depth);
            sense = false;
            threadSense = ThreadLocal.withInitial(() -> !sense);
            log.debug("depth: {}, node.len:{}, nodes: {}", depth, node.length, nodes);
        }

        // 构造完全 N 叉树
        void build(Node parent, int depth) {
            if (depth == 0) {
                node[nodes++] = new Node(parent, 0);
            } else {
                Node myNode = new Node(parent, radix);
                node[nodes++] = myNode;
                for (int i = 0; i < radix; i++) {
                    build(myNode, depth - 1);
                }
            }
        }

        @Override
        public void await() {
            node[ThreadID.get()].await();
        }

        class Node {
            int children;
            AtomicInteger childCount;
            Node parent;

            public Node(Node myParent, int count) {
                children = count;
                childCount = new AtomicInteger(count);
                parent = myParent;
            }

            public void await() {
                boolean mySense = threadSense.get();
                while (childCount.get() > 0) ;
                childCount.set(children);
                if (parent != null) {
                    parent.childDone();
                    while (sense != mySense) ;
                } else {
                    sense = !sense;
                }
                threadSense.set(!mySense);
            }

            public void childDone() {
                childCount.getAndDecrement();
            }
        }
    }

    /**
     * 组合树障碍
     * <p>
     * 此处留有疑问：build 完成之后 leaves != leaf.length.
     * 这个问题的结果是：leaf 数组后面有一部分 Node 是空指针。
     * 而现在的 barrier.await 算法可能会出现空指针错误。
     * <p>
     * 代码出处：The art of multiprocessor programming
     */
    static class TreeBarrier implements IBarrier {
        int radix;
        Node[] leaf;
        ThreadLocal<Boolean> threadSense;
        int leaves;

        public TreeBarrier(int n, int r) {
            radix = r;
            leaves = 0;
            leaf = new Node[n / r];
            int depth = 0;
            threadSense = ThreadLocal.withInitial(() -> true);
            while (n > 1) {
                depth++;
                n = n / r;
            }
            Node root = new Node();
            build(root, depth - 1);
            System.out.println("depth:" + depth + ", leaf.length=" + leaf.length + ", leaves=" + leaves);
        }

        void build(Node parent, int depth) {
            if (depth == 0) {
                leaf[leaves++] = parent;
            } else {
                for (int i = 0; i < radix; i++) {
                    Node node = new Node(parent);
                    build(node, depth - 1);
                }
            }
        }

        @Override
        public void await() {
            int me = ThreadID.get();
            Node myLeaf = leaf[me / radix];
            // 此处可能空指针
            myLeaf.await();
        }

        class Node {
            AtomicInteger count;
            Node parent;
            volatile boolean sense;

            public Node() {
                sense = false;
                parent = null;
                count = new AtomicInteger(radix);
            }

            public Node(Node parent) {
                this();
                this.parent = parent;
            }

            public void await() {
                boolean mySense = threadSense.get();
                int position = count.getAndDecrement();
                if (position == 1) {
                    if (parent != null) {
                        parent.await();
                    }
                    count.set(radix);
                    sense = mySense;
                } else {
                    while (sense != mySense) ;
                }
                threadSense.set(!mySense);
            }
        }
    }

    /**
     * 语义换向障碍
     */
    static class SenseBarrier implements IBarrier {

        private final int size;
        private final AtomicInteger count;
        private volatile boolean sense = false;

        private final ThreadLocal<Boolean> threadSense = ThreadLocal.withInitial(() -> !sense);

        public SenseBarrier(int n) {
            count = new AtomicInteger(n);
            size = n;
        }

        @Override
        public void await() {
            Boolean mySense = threadSense.get();
            int n = count.getAndDecrement();
            if (n == 1) {
                count.set(size);
                sense = mySense;
            } else {
                while (sense != mySense) ;
            }
            threadSense.set(!mySense);
        }
    }

    /**
     * 这种简单的栅栏实现不可使用，
     * 存在如下问题：
     * 1. 无法循环使用。如果循环使用，则会出现饥饿或死锁问题
     * 比如 A 和 B 两个线程，初始值为 2. A 线程先到达，等待 B 线程。
     * B 线程到达后，将 count 更新了，同时，B 线程又新启动了一个阶段。count 不为 0.
     * 此时，A 线程还在忙等待旧阶段的 count 减为 0，B 线程也在忙等待新阶段的 count 减为 0.
     */
    static class SimpleBarrier implements IBarrier {

        private final AtomicInteger count;
        private final int size;

        public SimpleBarrier(int n) {
            count = new AtomicInteger(n);
            size = n;
        }

        @Override
        public void await() {
            int n = count.getAndDecrement();
            if (n == 1) {
                count.set(size);
            } else {
                while (count.get() != 0) ;
            }
        }
    }

}
