package com.future.concurrent.history;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 7 章 自旋锁与争用
 * <p>
 * CLH 层次锁
 *
 * @author future
 */
@SuppressWarnings("all")
public class HCLHLock {

    static class QNode {
        private boolean tailWhenSpliced;
        private static final int TWS_MASK = 0x80000000;
        private boolean successorMustWait = false;
        private static final int SMW_MASK = 0x40000000;
        // private int clusterID;
        private static final int CLUSTER_MASK = 0x3fffffff;
        AtomicInteger state;

        QNode() {
            state = new AtomicInteger(0);
        }

        public void unlock() {
            int oldState = 0;
            int newState = ThreadID.getCluster();
            successorMustWait = true;
            newState |= SMW_MASK;
            tailWhenSpliced = false;
            newState &= (~TWS_MASK);
            do {
                oldState = state.get();
            } while (!state.compareAndSet(oldState, newState));
        }

        public int getClusterID() {
            return state.get() & CLUSTER_MASK;
        }

        public boolean waitForGrantOrClusterMaster() {
            return false;
        }

        public boolean isSuccessorMustWait() {
            return successorMustWait;
        }

        public void setSuccessorMustWait(boolean successorMustWait) {
            this.successorMustWait = successorMustWait;
        }

        public boolean isTailWhenSpliced() {
            return tailWhenSpliced;
        }

        public void setTailWhenSpliced(boolean tailWhenSpliced) {
            this.tailWhenSpliced = tailWhenSpliced;
        }
    }

    static final int MAX_CLUSTERS = 4;

    List<AtomicReference<QNode>> localQueues;
    AtomicReference<QNode> globalQueue;

    ThreadLocal<QNode> currNode = ThreadLocal.withInitial(QNode::new);
    ThreadLocal<QNode> predNode = ThreadLocal.withInitial(() -> null);

    public HCLHLock() {
        localQueues = new ArrayList<>(MAX_CLUSTERS);
        for (int i = 0; i < MAX_CLUSTERS; i++) {
            localQueues.add(new AtomicReference<>());
        }
        QNode head = new QNode();
        globalQueue = new AtomicReference<>(head);
    }

    public void lock() {
        QNode myNode = currNode.get();
        AtomicReference<QNode> localQueue = localQueues.get(ThreadID.getCluster());
        // splice my QNode into local queue
        QNode myPred = null;
        do {
            myPred = localQueue.get();
        } while (!localQueue.compareAndSet(myPred, myNode));
        if (myPred != null) {
            boolean iOwnLock = myPred.waitForGrantOrClusterMaster();
            if (iOwnLock) {
                predNode.set(myPred);
                return;
            }
        }

        // I am the cluster master: splice local queue into global queue.
        QNode localTail = null;
        do {
            myPred = globalQueue.get();
            localTail = localQueue.get();
        } while (!globalQueue.compareAndSet(myPred, localTail));
        // inform successor it is the new master
        localTail.setTailWhenSpliced(true);
        while (myPred.isSuccessorMustWait()) ;
        predNode.set(myPred);
        return;
    }

    public void unlock() {
        QNode myNode = currNode.get();
        myNode.setSuccessorMustWait(false);
        QNode node = predNode.get();
        node.unlock();
        currNode.set(node);
    }
}
