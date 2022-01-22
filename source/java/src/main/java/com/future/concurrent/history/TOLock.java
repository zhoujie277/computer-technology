package com.future.concurrent.history;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TimeOutLock
 * 该示例出自 artOfMultiProcessorProgramming
 * 原理发表自 Michael L.Scott 2002 年发表的 CLH-NB try lock.
 *
 * @author future
 */
@SuppressWarnings("unused")
class TOLock {
    static class QNode {
        // pred 三个状态，
        // 1. null。表示正在临界区
        // 2. AVAILABLE。表示已出临界区。
        // 3. 指向前驱结点。表示自己已超时。
        public QNode pred = null;
    }

    static QNode AVAILABLE = new QNode();
    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myNode;

    public TOLock() {
        tail = new AtomicReference<>(null);
        myNode = ThreadLocal.withInitial(QNode::new);
    }

    public boolean tryLock(long time, TimeUnit unit) {
        long startTime = System.currentTimeMillis();
        long patience = TimeUnit.MILLISECONDS.convert(time, unit);
        QNode qnode = new QNode();
        myNode.set(qnode);
        qnode.pred = null;
        QNode myPred = tail.getAndSet(qnode);
        if (myPred == null || myPred.pred == AVAILABLE) {
            return true;
        }

        while (System.currentTimeMillis() - startTime < patience) {
            QNode predPred = myPred.pred;
            if (predPred == AVAILABLE) {
                return true;
            } else if (predPred != null) {
                myPred = predPred;
            }
        }

        // 如果超时的结点是 tail 结点，则尝试更新 tail 为前驱结点
        // 如果超时的结点不是 tail 结点，则更改代表自己线程的结点的 pred 指针，指向前驱结点。
        // 表示自己已超时。也就是告诉后继节点（如果有），请尝试在有效的前驱结点上旋转。
        // 超时的线程最终返回 false
        if (!tail.compareAndSet(qnode, myPred))
            qnode.pred = myPred;
        return false;
    }

    public void unlock() {
        QNode qnode = myNode.get();
        if (!tail.compareAndSet(qnode, null))
            qnode.pred = AVAILABLE;
    }
}
