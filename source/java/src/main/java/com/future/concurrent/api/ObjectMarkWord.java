package com.future.concurrent.api;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jol.info.ClassLayout;

import lombok.extern.slf4j.Slf4j;

/**
 * 对象头结构
 * 
 * jol-core 中 ClassLayout 可打印对象头结构
 * 程序刚启动时，默认是正常状态，无偏向锁（0），锁标志位为（01）
 * 经历了一定延迟后，默认是偏向锁状态（1）。锁标志位为（01）
 * 
 * @author future
 */
@Slf4j
@SuppressWarnings("unused")
class ObjectMarkWord {

    static class MarkWord {

    }

    /**
     * 查看默认情况下 biasable 状态
     */
    @SuppressWarnings("all")
    public static void biasalbe() throws InterruptedException {
        MarkWord word = new MarkWord();
        String printable = ClassLayout.parseInstance(word).toPrintable();
        log.debug("程序刚启动时，对象状态为无偏向状态（0），锁标志位为（01）");
        log.debug(printable);

        TimeUnit.SECONDS.sleep(4);

        ObjectMarkWord objectMarkWord = new ObjectMarkWord();
        // 调用 hashcode 会起到禁用偏向锁的作用。
        // objectMarkWord.hashCode();
        printable = ClassLayout.parseInstance(objectMarkWord).toPrintable();
        log.debug("经历启动延迟后，对象头结构：");
        log.debug(printable);

        synchronized (objectMarkWord) {
            // 如果禁用了偏向锁，此处会升级为轻量级锁
            log.debug("进入同步代码块后，分配了线程 ID，以及 Epoch");
            log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
        }
        // 如果禁用了偏向锁，退出同步代码块后，无竞争情况下，会还原为正常状态。
        log.debug("退出同步代码块后，对象头结构相较于同步块，不会发生变化，保留了线程 ID");
        log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
    }

    /**
     * 无竞争情况下，偏向锁升级为轻量级锁的过程
     * 
     * 当线程 1 进入同步代码块以及退出同步代码块时，对象仍然为偏向锁状态，
     * 当线程 2 进入同步代码块时，发现偏向锁状态的 线程 ID 不是当前线程（线程 2）的线程 ID，故升级为轻量级锁。
     * 当线程 2 退出同步代码块时，由于没有发生竞争，所以对象头状态去除轻量级锁，还原到初始无锁状态。
     * 
     * @throws InterruptedException
     */
    public static void lightWeight() throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        CountDownLatch latch = new CountDownLatch(1);
        MarkWord objectMarkWord = new MarkWord();

        Thread t1 = new Thread("t1") {
            @Override
            public void run() {
                log.debug("进入 Thread 1，对象状态结构为：");
                log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
                synchronized (objectMarkWord) {
                    // 如果禁用了偏向锁，此处会升级为轻量级锁
                    log.debug("进入 Thread 1 同步代码块后，分配了线程 ID，以及 Epoch");
                    log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
                }
                log.debug("退出Thread 1 同步代码块后，对象头结构相较于同步块，不会发生变化，保留了线程 ID");
                log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());

                latch.countDown();
            };
        };
        t1.start();
        Thread t2 = new Thread("t2") {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }

                log.debug("进入 Thread 2，对象状态结构为：");
                log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
                synchronized (objectMarkWord) {
                    // 如果禁用了偏向锁，此处会升级为轻量级锁
                    log.debug("进入 Thread 2 同步代码块后，分配了线程 ID，以及 Epoch");
                    log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());
                }
                log.debug("退出Thread 2 同步代码块后，对象头结构相较于同步块，不会发生变化，保留了线程 ID");
                log.debug(ClassLayout.parseInstance(objectMarkWord).toPrintable());

            };
        };
        t2.start();

    }

    /**
     * 批量重偏向
     * 
     * 如下例所示，最初对象的偏向锁为 T1 线程所有，Thread ID 为 T1 线程 iD。
     * 当线程 T2 访问时，发现对象头偏向锁所属 Thread ID 不是当前线程，则执行偏向锁撤销，升级为 轻量级锁，
     * 当连续发生 20 次后，JVM 便会将该锁重新偏向为 T2 线程。
     * 20 是重偏向默认阈值。
     * 
     * @throws InterruptedException
     */
    public static void batchRebiasable() throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        CountDownLatch latch = new CountDownLatch(1);
        List<MarkWord> list = new Vector<>();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                MarkWord word = new MarkWord();
                list.add(word);
                synchronized (word) {
                    log.debug(ClassLayout.parseInstance(word).toPrintable());
                }
            }
            latch.countDown();
        }, "T1");
        t1.start();

        Thread t2 = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            log.debug("------->");
            for (int i = 0; i < 30; i++) {
                MarkWord word = list.get(i);
                synchronized (word) {
                    log.debug("进入Thread 2 代码块");
                    log.debug(ClassLayout.parseInstance(word).toPrintable());
                }
                log.debug("退出Thread 2 代码块");
                log.debug(ClassLayout.parseInstance(word).toPrintable());
            }
        }, "T2");
        t2.start();
    }

    /**
     * 批量撤销
     * 
     * 如下例所示，在 T1 线程中添加 39 个对象至集合中，每个对象的偏向锁状态均为偏向 T1 线程。
     * 当 T2 线程访问时，执行撤销偏向锁并升级为轻量级锁，出同步代码块时，置为无锁状态。
     * 当 T2 线程执行到 20 次时，对象的偏好均批量重定向为 T2 线程。
     * 当 T3 线程执行访问时，又撤销对象的 T2 偏向锁，并升级为轻量级锁。退出同步代码块时，置为无锁状态。
     * 当 T3 线程执行到 20 此时，偏向锁均批量重定向到 T3 线程。（前面 20个对象已被 T2 撤销为无锁状态，后 20 个偏向 T2 的对象被 T3 撤销）
     * 当撤销偏向锁累计达 40 次时，JVM 会执行批量撤销，仍为该类存在过度竞争，不适用于偏向锁。故新创建的对象均为无锁状态。
     * 40 是批量撤销的阈值。(将下例 loopCount 改为 38，生成的对象默认便还是有偏向锁状态)
     */
    public static void batchRevoke() throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        int loopCount = 39;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        List<MarkWord> list = new Vector<>();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < loopCount; i++) {
                MarkWord word = new MarkWord();
                list.add(word);
                synchronized (word) {
                    log.debug(ClassLayout.parseInstance(word).toPrintable());
                }
            }
            latch.countDown();
        }, "T1");
        t1.start();

        Thread t2 = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            log.debug("------->");
            for (int i = 0; i < loopCount; i++) {
                MarkWord word = list.get(i);
                log.debug("即将进入Thread 2 代码块: i=" + i);
                log.debug(ClassLayout.parseInstance(word).toPrintable());
                synchronized (word) {
                    log.debug(ClassLayout.parseInstance(word).toPrintable());
                }
                log.debug("退出Thread 2 代码块");
                log.debug(ClassLayout.parseInstance(word).toPrintable());
            }
            latch2.countDown();
        }, "T2");
        t2.start();

        Thread t3 = new Thread(() -> {
            try {
                latch2.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            log.debug("------->");
            for (int i = 0; i < loopCount; i++) {
                MarkWord word = list.get(i);
                log.debug("即将进入Thread 3 代码块: i=" + i);
                log.debug(ClassLayout.parseInstance(word).toPrintable());
                synchronized (word) {
                    log.debug(ClassLayout.parseInstance(word).toPrintable());
                }
                log.debug("退出Thread 3 代码块");
                log.debug(ClassLayout.parseInstance(word).toPrintable());
            }
        }, "T3");
        t3.start();

        t3.join();
        log.debug("批量撤销之后，JVM 认为该对象竞争太激烈了，则之后新创建的对象状态为无锁状态");
        log.debug(ClassLayout.parseInstance(new MarkWord()).toPrintable());
    }

    public static void main(String[] args) throws InterruptedException {
        // lightWeight();
        // batchRebiasable();
        batchRevoke();
    }

}
