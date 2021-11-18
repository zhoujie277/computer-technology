package com.future.thread;

import java.util.concurrent.Semaphore;

/**
 * 读者-写者问题 读者优先的例子. 算法采用信号量实现。
 * 
 * @author zhoujie
 */
@SuppressWarnings("unused")
public class ReaderWriterSem {

    /**
     * 读者优先 <br/>
     * 即当读者进行读时，后续的写者必须等待，直到所有读者均离开后，写者才可进入。 <br/>
     * 允许任意多读进程同时读. 一次只允许一个写进程进行写操作.<br/>
     * 如果有一个写进程正在进行写操作，禁止任何读进程进行读操作。 <br/>
     * 若读者源源不断，可能会导致写者进程无限期地推迟。
     */
    private static class ReaderPriority {
        private static Semaphore dataRes = new Semaphore(1);
        private static Semaphore readMutex = new Semaphore(1);
        private static int readCount = 0;

        public static void read() {
            try {
                readMutex.acquire();
                if (readCount == 0) {
                    // 如果是滴一个读者,则占有数据资源
                    dataRes.acquire();
                }
                readCount++;
                readMutex.release();

                // read操作

                readMutex.acquire();
                readCount--;
                if (readCount == 0) {
                    // 如果是最后一个读者，则释放数据资源
                    dataRes.release();
                }
                readMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public static void write() {
            try {
                dataRes.acquire();
                // write 操作
                dataRes.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private static class Reader extends Thread {
            @Override
            public void run() {
                while (true) {
                    read();
                }
            }
        }

        private static class Writer extends Thread {
            @Override
            public void run() {
                while (true) {
                    write();
                }
            }
        }
    }

    /**
     * 写者优先 <br/>
     * 当一个写者到来时，只有那些已经获得授权允许读的进程才被允许完成它们的操作， 写者之后来的新读者将被推迟，直到写者完成。
     * 在该策略中，如果有一个不可中断的连续的写者，读者进程将会被无限期地推迟。
     */
    private static class WriterPriority {
        private static Semaphore dataRes = new Semaphore(1);
        private static Semaphore readSem = new Semaphore(1);
        private static Semaphore readMutex = new Semaphore(1);
        private static Semaphore writeMutex = new Semaphore(1);
        private static int readCount = 0;
        private static int writeCount = 0;

        public static void read() {
            try {
                // 读信号资源，只允许一个线程占有。如果读信号资源被写线程占有，则等待。
                // 如果读线程先进来，则占有，让写线程去等待。
                readSem.acquire();
                // 能到这里，说明读者线程已经进来了
                readMutex.acquire();
                if (readCount == 0) {
                    // 如果是第一个读者，就占有数据资源。不让写线程写数据。
                    dataRes.acquire();
                }
                readCount++;
                readMutex.release();
                // 释放读信号，如果写线程线占有，则释放写线程。
                // 请注意，当执行到这里时，读线程肯定已经获取了db信号，
                // 获得读信号的写线程虽然会往前运行，但会阻塞在写数据处。
                // 从而实现 已经在读的时候与写互斥。
                readSem.release();

                // read操作

                readMutex.acquire();
                readCount--;
                if (readCount == 0) {
                    dataRes.release();
                }
                readMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public static void write() {
            try {
                // 写者线程进来，优先处理写者对读者的互斥。
                writeMutex.acquire();
                if (writeCount == 0) {
                    // 第一个写者进来，占有读信号，不允许后面的线程读。
                    // 如果读线程先占有读信号，则写线程会阻塞在这里。
                    // 后面来的写线程则会阻塞在 writeMutex。
                    readSem.acquire();
                }
                writeCount++;
                writeMutex.release();

                // 真正的资源互斥处在这里，如果有读者，则等待读完。如果没有，则进去。
                dataRes.acquire();
                // write 操作
                dataRes.release();

                writeMutex.acquire();
                writeCount--;
                if (writeCount == 0) {
                    // 一直等到最后一个写者写完，从而实现写者优先。
                    // 最后一个写者写完，释放读信号，允许读。
                    readSem.release();
                }
                writeMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 公平策略 以上两种策略，读者或写者进程中一个对另一个有绝对的优先权.<br/>
     * Hoare提出了一种更公平的策略。由如下规则定义： <br/>
     * 1. 在一个读序列中，如果有写者在等待，那么就不允许新来的读者开始执行。 <br/>
     * 2. 在一个写操作结束时，所有等待的读者应该比下一个写者有更高的优先权。<br/>
     */
    private static class FairStrategy {
        private static Semaphore dataRes = new Semaphore(1);
        private static Semaphore readWriteMutex = new Semaphore(1);
        private static Semaphore readMutex = new Semaphore(1);
        private static int readCount = 0;

        public static void read() {
            try {
                // 读写互斥，先来先服务
                readWriteMutex.acquire();

                readMutex.acquire();
                if (readCount == 0) {
                    // 如果是第一个读者，就占有数据资源。不让写线程写数据。
                    dataRes.acquire();
                }
                readCount++;
                readMutex.release();

                readWriteMutex.release();

                // read操作，支持并发读

                readMutex.acquire();
                readCount--;
                if (readCount == 0) {
                    dataRes.release();
                }
                readMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public static void write() {
            try {
                // 读写互斥，先来先服务
                readWriteMutex.acquire();

                // 真正的资源互斥处在这里，如果有读者，则等待读完。如果没有，则进去。
                dataRes.acquire();
                // write 操作，只允许一个个写
                dataRes.release();

                readWriteMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

    }

}
