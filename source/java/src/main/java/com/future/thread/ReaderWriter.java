package com.future.thread;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * 读者-写者问题 读者优先的例子. <br/>
 * 算法采用管程实现。
 * 
 * @author zhoujie
 */
@SuppressWarnings("unused")
public class ReaderWriter {

    /**
     * 读者优先 <br/>
     * 即当读者进行读时，后续的写者必须等待，直到所有读者均离开后，写者才可进入。 <br/>
     * 允许任意多读进程同时读. 一次只允许一个写进程进行写操作.<br/>
     * 如果有一个写进程正在进行写操作，禁止任何读进程进行读操作。 <br/>
     * 若读者源源不断，可能会导致写者进程无限期地推迟。
     */
    private static class ReaderPriority {
        private ReentrantLock readlock = new ReentrantLock();
        private ReentrantLock writelock = new ReentrantLock();
        private int readCount = 0;

        public void read() {
            readlock.lock();
            if (readCount == 0) {
                // 如果是滴一个读者,则占有数据资源
                writelock.lock();
            }
            readCount++;
            readlock.unlock();

            // read操作

            readlock.lock();
            readCount--;
            if (readCount == 0) {
                // 如果是最后一个读者，则释放数据资源
                writelock.unlock();
            }
            readlock.unlock();
        }

        public void write() {
            writelock.lock();
            // write 操作
            writelock.unlock();
        }
    }

    /**
     * 写者优先 当一个写者到来时，只有那些已经获得授权允许读的进程才被允许完成它们的操作， 写者之后来的新读者将被推迟，直到写者完成。
     * 在该策略中，如果有一个不可中断的连续的写者，读者进程将会被无限期地推迟。
     */
    private static class WriterPriority {
        private ReentrantLock readlock = new ReentrantLock();
        private ReentrantLock writelock = new ReentrantLock();
        private ReentrantLock readMutex = new ReentrantLock();
        private ReentrantLock writeMutex = new ReentrantLock();

        private int readCount = 0;
        private int writeCount = 0;

        public void read() {
            // 读锁，只允许一个线程占有。如果读信号资源被写线程占有，则等待。
            // 如果读线程先进来，则占有，让写线程去等待。
            readlock.lock();
            // 能到这里，说明读者线程已经进来了
            readMutex.lock();

            if (readCount == 0) {
                // 如果是第一个读者，就占有数据资源。不让写线程写数据。
                writelock.lock();
            }
            readCount++;
            // 释放读信号，如果写线程线占有，则释放写线程。
            // 请注意，当执行到这里时，读线程肯定已经获取了db信号，
            // 获得读信号的写线程虽然会往前运行，但会阻塞在写数据处。
            // 从而实现 已经在读的时候与写互斥。
            readMutex.unlock();
            
            readlock.unlock();

            // read操作

            readMutex.lock();
            readCount--;
            if (readCount == 0) {
                writelock.unlock();
            }
            readMutex.unlock();
        }

        public void write() {
            // 写者线程进来，优先处理写者对读者的互斥。
            writeMutex.lock();
            if (writeCount == 0) {
                // 第一个写者进来，占有读信号，不允许后面的线程读。
                // 如果读线程先占有读信号，则写线程会阻塞在这里。
                // 后面来的写线程则会阻塞在 writeMutex。
                readlock.lock();
            }
            writeCount++;
            writeMutex.unlock();

            // 真正的资源互斥处在这里，如果有读者，则等待读完。如果没有，则进去。
            writelock.lock();
            // write 操作
            writelock.unlock();

            writeMutex.lock();
            writeCount--;
            if (writeCount == 0) {
                // 一直等到最后一个写者写完，从而实现写者优先。
                // 最后一个写者写完，释放读信号，允许读。
                readlock.unlock();
            }
            writeMutex.unlock();
        }
    }

    /**
     * 公平策略 以上两种策略，读者或写者进程中一个对另一个有绝对的优先权.<br/>
     * Hoare提出了一种更公平的策略。由如下规则定义： <br/>
     * 1. 在一个读序列中，如果有写者在等待，那么就不允许新来的读者开始执行。 <br/>
     * 2. 在一个写操作结束时，所有等待的读者应该比下一个写者有更高的优先权。<br/>
     */
    private static class FairStrategy {
        private ReentrantLock readlock = new ReentrantLock(true);
        private ReentrantLock writelock = new ReentrantLock(true);
        private ReentrantLock readWritelock = new ReentrantLock(true);
        private int readCount = 0;

        public void read() {
            // 读写互斥，先来先服务
            readWritelock.lock();

            readlock.lock();
            if (readCount == 0) {
                // 如果是第一个读者，就占有数据资源。不让写线程写数据。
                writelock.lock();
            }
            readCount++;
            readlock.unlock();

            readWritelock.unlock();

            // read操作，支持并发读

            readlock.lock();
            readCount--;
            if (readCount == 0) {
                writelock.unlock();
            }
            readlock.unlock();
        }

        public void write() {
            // 读写互斥，先来先服务
            readWritelock.lock();

            // 真正的资源互斥处在这里，如果有读者，则等待读完。如果没有，则进去。
            writelock.lock();
            // write 操作，只允许一个个写
            writelock.unlock();

            readWritelock.unlock();
        }
    }

}
