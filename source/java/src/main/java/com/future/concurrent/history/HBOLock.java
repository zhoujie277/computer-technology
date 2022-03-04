package com.future.concurrent.history;

import com.future.concurrent.history.backoff.BackOff;
import com.future.concurrent.history.backoff.ExponentialBackOff;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The art of multiprocessor programming
 * <p>
 * 第 7 章 层次退避锁
 *
 * 缺点之一：过度利用了局部性。有可能存在同一集群中的线程不断传递锁，而其他集群中的线程发送饥饿的现象。
 *
 * @author future
 */
public class HBOLock {
    private static final int LOCAL_MIN_DELAY = 1;
    private static final int LOCAL_MAX_DELAY = 2000;
    private static final int REMOTE_MIN_DELAY = 1;
    private static final int REMOTE_MAX_DELAY = 2000;

    private static final int FREE = -1;

    AtomicInteger state;

    public HBOLock() {
        state = new AtomicInteger(FREE);
    }

    public void lock() {
        int myCluster = ThreadID.getCluster();
        BackOff localBackoff = new ExponentialBackOff(LOCAL_MIN_DELAY, LOCAL_MAX_DELAY);
        BackOff remoteBackoff = new ExponentialBackOff(REMOTE_MIN_DELAY, REMOTE_MAX_DELAY);
        while (true) {
            if (state.compareAndSet(FREE, myCluster)) {
                return;
            }
            int lockState = state.get();
            if (lockState == myCluster) {
                localBackoff.backOff();
            } else {
                remoteBackoff.backOff();
            }
        }
    }

    public void unlock() {
        state.set(FREE);
    }
}
