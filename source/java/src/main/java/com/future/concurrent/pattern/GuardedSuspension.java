package com.future.concurrent.pattern;

/**
 * 保护性-暂停模式
 *
 * Guarded Suspension 的关注点在于临界值的条件是否满足，当达到设置的临界值时相关线程会被挂起。
 *
 * Guarded Suspension 通常是其他多线程设计模式的基础。
 *
 * @author future
 */
class GuardedSuspension {
    private Object response;

    public void offer(Object value) {
        synchronized (this) {
            response = value;
            this.notifyAll();
        }
    }

    public Object take() {
        synchronized (this) {
            while (response == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.notifyAll();;
            return response;
        }
    }

    public Object take(long timeout) {
        long begin = System.currentTimeMillis();
        long passedTime = 0, waitTime = 0;
        synchronized (this) {
            while (response == null) {
                waitTime = timeout - passedTime;
                if (waitTime <= 0) break;
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                passedTime = System.currentTimeMillis() - begin;
            }
            this.notifyAll();
            return response;
        }
    }
}
