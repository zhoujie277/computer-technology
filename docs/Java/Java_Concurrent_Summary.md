# Java_Concurrent_Summary
Java 并发库总结

## 1. AbstractQueuedSynchronizer
所谓同步器是指对线程进行某个临界区的访问建立一整套规则。以实现一个线程或者多个线程对该临界区的同步与互斥。临界区通常是包含共享资源的代码区。通常，只允许一个线程访问临界区，叫独占访问；而同时多个线程同时访问临界区，则称为共享访问。

AbstractQueuedSynchronizer 的设计总共从 MESA 管程、CLH 无阻塞队列、阻塞与非阻塞、公平性、独占锁和共享锁、可超时与可打断、提供性能追踪接口等几个方面来展开实现的。

### 1.1 MESA 管程

### 1.2 CLH 无阻塞队列

### 1.3 阻塞与非阻塞

### 1.4 公平性

### 1.5 独占锁和共享锁

### 1.6 可超时与可打断

### 1.7 性能追踪接口

## FutureTask
FutureTask 异步任务的设计主要是从执行任务、等待任务、取消任务、异常处理以及查询任务状态等几个方面来展开实现的，在 Java 中其典型实现有 Java5 版本和 Java8 的版本。Java5 中采用的是 AbstractQueuedSynchronizer 的同步队列来实现等待队列。而 Java8 中采用 TreiberStack 来实现的可取消等待队列。

## ThreadPoolExecutor