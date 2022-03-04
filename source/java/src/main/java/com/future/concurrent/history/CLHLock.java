package com.future.concurrent.history;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * CLH锁是一种自旋锁，能确保无饥饿性，提供先来先服务的公平性。
 * 同MCS自旋锁一样，CLH 也是一种基于单向链表(隐式创建)的高性能、公平的自旋锁。
 * 申请加锁的线程只需要在<b>其前驱节点</b>的本地变量上自旋。
 * 从而极大地减少了不必要的处理器缓存同步的次数，降低了总线和内存的开销。
 * <p>
 * CLH队列锁的优点是空间复杂度低
 * （如果有n个线程，L个锁，每个线程每次只获取一个锁，
 * 那么需要的存储空间是 O（L + n），n 个线程有 n 个 myNode，L 个锁有 L 个 tail），
 * CLH 的一种变体被应用在了JAVA并发框架中。
 * CLH 在 SMP 系统结构下该法是非常有效的。
 * 但在 NUMA 系统结构下，每个线程有自己的内存，
 * 如果前趋结点的内存位置比较远，自旋判断前趋结点的 locked 域，性能将大打折扣。
 * <p>
 * CLH 锁是对自旋锁的一种改进，有效的解决了以上的两个缺点。
 * 首先它将线程组织成一个队列，保证先请求的线程先获得锁，避免了饥饿问题。
 * 其次锁状态去中心化，让每个线程在不同的状态变量中自旋，
 * 这样当一个线程释放它的锁时，只能使其后续线程的高速缓存失效，缩小了影响范围，
 * 从而减少了 CPU 的开销。
 * <p>
 * 此处的 CLH 的实现是不可重入的独占公平锁。
 */
@SuppressWarnings("StatementWithEmptyBody")
@Slf4j
public class CLHLock {
    /**
     * 隐式链表结构
     * <p>
     * CLH节点中并没有指向其后继节点的next属性。
     * 但是这并不代表 CLH 锁不依赖链表这种数据结构，
     * 毕竟作为一种公平的自旋锁，CLH还是需要仰仗链表的。
     * 只不过这个链表是隐式维护的，通过原子更新器的 getAndSet 方法在更新 tail 时，
     * 可以在 Set 的同时获取到原来的 tail 节点。
     * 这也从侧面反映了，为什么CLH锁是在前驱节点的active属性上自旋。
     * 每个节点只了解它直接前驱节点的状态，不需要显式地去维护一个完整的链表结构。
     */
    static class Node {
        volatile boolean locked;
    }

    private final ThreadLocal<Node> threadLocalNode = ThreadLocal.withInitial(Node::new);

    /**
     * 隐式链表的 tail 结点
     */
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    private void lock(Node cNode) {
        // 表示想要获取锁
        cNode.locked = true;
        // 获取前驱结点
        Node predecessor = tail.getAndSet(cNode);
        // 若前驱结点不为空，则说明前面已经有线程在占用锁，该线程需要在忙等待。
        if (predecessor != null) {
            // 等待条件为前驱结点的本地变量。
            while (predecessor.locked) ;
        }
        // 若前驱结点为空，或者 predecessor.locked = false; 则说明可以获得锁，进入临界区。
    }

    private void unlock(Node cNode) {
        // 唤醒下一个线程。
        cNode.locked = false;
    }

    public void lock() {
        Node node = threadLocalNode.get();
        lock(node);
    }

    public void unlock() {
        Node node = threadLocalNode.get();
        unlock(node);
        // 此处 remove 必不可少，否则 localMode 如果重用，将会出现资源同时占用的情况，或者可能产生死锁。
        // 比如，两个线程 T1，T2。T1 先获取锁，T2 等待 T1 释放锁。
        // T1 进入 lock 之后，由于 localNode 之前没有被 remove，所以获取到同一个 node。
        // 而此时，重用的 active 为 false，后面的线程 T3 再进来，则不会等待了，直接进入了临界区。
        // 此时就出现了一个本应该互斥的临界区被多个线程同时访问了，不安全的问题产生。
        // 如果，重用 node 将 active 设置为 true，则可能会出现另一种死锁情况。
        // T1 释放锁成功之后，又马上竞争重新尝试获取锁，此时 T1 等待 T2 释放锁。
        // 即 T2 还没来得及进入临界区，一直在等待 T1 的localNode 的 active 为 false。
        // 而此时 T1 等待 T2 的 active，T2 也在等待 T1 的 active。产生了死锁。
        //
        threadLocalNode.remove();
        // 也可以重用 prevNode. 参见 artOfMultiProcessorProgramming
        // 一开始使用 predNode (ThreadLocal) 保存
        // 然后在此处使用 threadLocalNode.set(preNode);
    }

    static class Tester {
        Runnable generateTask(final CLHLock lock, int taskId) {
            return () -> {
                lock.lock();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("Thread {} completed", taskId);
                lock.unlock();
            };
        }

        void test1() {
            final CLHLock lock = new CLHLock();
            for (int i = 1; i <= 20; i++) {
                Runnable runnable = generateTask(lock, i);
                new Thread(runnable).start();
            }
        }
    }

    public static void main(String[] args) {
        Tester tester = new Tester();
        tester.test1();
    }
}
