package com.future.concurrent.api;

import com.future.concurrent.fakelib.LinkedBlockingQueue;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SuppressWarnings("unused")
class BlockingQueueAPI {

    @ToString
    private static class Msg {
        static AtomicInteger counter = new AtomicInteger(1);
        private final int id;

        public Msg() {
            id = counter.getAndIncrement();
        }
    }

    static Msg createMsg() {
        return new Msg();
    }

    static class Producer implements Runnable {
        private final BlockingQueue<Msg> queue;
        private final Msg msg = createMsg();

        public Producer(BlockingQueue<Msg> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                log.debug("producer is running... {}", msg);
                queue.put(msg);
                log.debug("producer put success..");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer implements Runnable {
        private final BlockingQueue<Msg> queue;

        public Consumer(BlockingQueue<Msg> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                log.debug("consumer is running...");
                Msg msg = queue.take();
//                Msg msg = queue.poll();
                log.debug("consume msg {}", msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 以下测试主要针对 SynchronousQueue，双线程版本的两个基本用例。
     * 1. 先存后取；即先在容器中进行 put 操作生成 Data 结点，然后 take 操作生成 Request 结点以取得匹配的 Data 结点数据。
     * 2. 先取后存；即先在容器中进行 take 操作生成 Request 结点，然后 put 操作生成 Data 结点以唤醒匹配的 take 操作，并将 Data 结点数据交给它。
     */
    void testHistoryForTwoThread(final BlockingQueue<Msg> queue) throws InterruptedException {
        Thread producer = new Thread(new Producer(queue), "producer");
        Thread consumer = new Thread(new Consumer(queue), "consumer");
        // 先存，后取的示例。
        producer.start();
        Thread.sleep(1000);
        consumer.start();

        // 先取后存的示例
//        consumer.start();
//        Thread.sleep(1000);
//        producer.start();
    }

    /**
     * precondition:
     * A calls dequeue
     * B calls dequeue
     * C enqueues a msgId 1
     * D enqueues a msgId 2
     *
     * fair expect History:
     * A's call returns the msgOd 1.
     * B's call returns the msgId 2.
     *
     * No expect:
     * B's call returns the msgId 1.
     * A's call returns the msgId 2.
     */
    void testHistory(final BlockingQueue<Msg> queue) throws InterruptedException {
        Thread A = new Thread(new Consumer(queue), "A");
        Thread B = new Thread(new Consumer(queue), "B");
        Thread C = new Thread(new Producer(queue), "C"); // msg 1
        Thread D = new Thread(new Producer(queue), "D"); // msg 2

        A.start();
        Thread.sleep(500);
        B.start();
        Thread.sleep(500);
        C.start();
        C.join();
        D.start();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void testLinkedBlockingQueue() {
        // test Linked Node's next == ? after remove(Object) method
        LinkedBlockingQueue<Msg> queue = new LinkedBlockingQueue<>();
        queue.add(createMsg());
        Msg msg = createMsg();
        queue.add(msg);
        queue.add(createMsg());
        // to debug
        queue.remove(msg);
    }

    public static void main(String[] args) throws InterruptedException {
        BlockingQueueAPI queueAPI = new BlockingQueueAPI();
//        queueAPI.testHistory(new SynchronousQueue<>(true));
        queueAPI.testHistory(new SynchronousQueue<>(false));
//        queueAPI.testHistory(new ArrayBlockingQueue<>(10));
//        queueAPI.testLinkedBlockingQueue();
    }
}
