package com.future.concurrent.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * MCS自旋锁是一种基于单向链表的高性能、公平的自旋锁。
 * 申请加锁的线程只需要在本地变量上自旋，直接前驱负责通知其结束自旋，
 * 从而极大地减少了不必要的处理器缓存同步的次数，降低了总线和内存的开销。
 * <p>
 * 申请线程只在本地变量上自旋，由直接前驱负责通知其结束自旋
 * （与 CLH 自旋锁不同的地方，不在轮询前驱的状态，而是由前驱主动通知），
 * 从而极大地减少了不必要的处理器缓存同步的次数，降低了总线和内存的开销。
 * 而 MCS 是在自己的结点的 locked 域上自旋等待。
 * 正因为如此，它解决了 CLH 在 NUMA 系统架构中获取 locked 域状态内存过远的问题。
 * <p>
 * 此处的 MCS 锁是一种不可重入的独占公平锁。
 *
 * @author future
 */
@SuppressWarnings("unused")
@Slf4j
public class MCSLock {

    /**
     * 单链表结构。
     * MCS 锁的节点对象需要有两个状态。
     * next 用来维护单向链表的结构，
     * blocked 用来表示节点的状态：true 表示处于自旋中，false 表示加锁成功。
     * MCS锁的节点状态blocked的改变是由其前驱节点触发改变的。
     * 加锁时会更新链表的末节点并完成链表结构的维护。
     * 释放锁的时候由于链表结构建立的时滞(getAndSet 原子方法和链表建立整体而言并非原子性)，
     * 可能存在多线程的干扰，需要使用忙等待保证链表结构就绪
     */
    static class Node {
        volatile Node next;
        volatile boolean blocked = true;
    }

    private final ThreadLocal<Node> currentThreadNode = new ThreadLocal<>();

    // 永远指向队列的尾部。
    private volatile Node mcsNode;

    private static final AtomicReferenceFieldUpdater<MCSLock, Node> mcsNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(MCSLock.class, Node.class, "mcsNode");

    /**
     * 每个线程都需要携带一个 Node 结构体。
     * 每个线程更新全局的 mcsNode;
     * 即 mcsNode 总是被 set 为最后一个进入 lock 方法的线程持有的。
     * 即 mcsNode 总是指向单链表的尾端。
     * 如果院子更新 mcsNode 的前驱节点不为空，则将前一个 Node 的 next 域指向新进来的线程持有的结点，形成一个单链表。
     */
    private void lock(Node currentThread) {
        // 原子更新 mcsNode，并返回更新之前的值。
        Node predecessor = mcsNodeUpdater.getAndSet(this, currentThread);
        if (predecessor != null) {
            // 形成单链表结构
            predecessor.next = currentThread;
            // 等待前驱结点主动通知，结束自旋等待。即将 blocked 设为 false。则该线程可获得锁。
            //noinspection StatementWithEmptyBody
            while (currentThread.blocked) ;
        } else {
            // 只有一个线程在使用它，没有出现争用。把自己标记为非阻塞。
            currentThread.blocked = false;
        }
    }

    private void unlock(Node currentThread) {
        // 无争用
        if (currentThread.next == null && !mcsNodeUpdater.compareAndSet(this, currentThread, null)) {
            // 如果此处更新失败，说明有多个线程在 unlock 在第一个 next == null 条件通过了，之后，在第二个条件出现了竞争。
            // 此处仍需要旋转等待，因为 predecessor.next 的更新和 mcsNodeUpdater.compareAndSet 的更新同属于一个原子操作。
            // noinspection StatementWithEmptyBody
            while (currentThread.next == null) ;
        }
        if (currentThread.next != null) {
            currentThread.next.blocked = false;
            currentThread.next = null; // for GC
        }
    }

    /**
     * 获取当前线程和对应的节点对象(不存在则初始化)
     */
    public void lock() {
        Node cNode = currentThreadNode.get();
        if (cNode == null) {
            cNode = new Node();
            currentThreadNode.set(cNode);
        }
        lock(cNode);
    }

    public void unlock() {
        Node cNode = currentThreadNode.get();
        if (cNode == null) {
            // 表明没有创建过，即没有 lock 过。
            return;
        }
        unlock(cNode);
        currentThreadNode.remove();
    }

    static class Tester {
        Runnable generateTask(final MCSLock lock, int taskId) {
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
            final MCSLock lock = new MCSLock();
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
