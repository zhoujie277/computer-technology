
### 故障诊断调优相关
+ jps: 查看当前系统中有哪些 java 进程及对应的 pid。
+ jstack: 给定一个 pid，命令行输出某个 java 进程中的线程栈信息。包括 locked，waiting to lock xxx，线程状态以及对应的源代码行数等信息。
+ jmap: 命令行工具。查看某一时刻堆内存占用情况。
  ```
      jmap -heap pid
  ```
+ jconsole：图形界面，多功能的监测工具，连接一个 java 进程，可连续的监测内存使用情况，以及监测死锁。
+ jvisualvm: 图形界面。连接一个 java 进程，也可连续监测内存及线程使用情况，不过它比 jconsole 多了一个堆转储的功能。可抓取内存快照以及搜索对象较大的对象。

#### 在 linux 下，已知某机器中有一个线程 CPU 占用过高，用什么方法可以诊断出来是哪个线程?
+ 先使用 top 命令，查看当前占用 CPU 最高的进程 pid。
+ 再使用 ps H -eo pid,tid,%cpu | grep pid 查看该进程下的每个线程使用的 CPU 情况。
+ 再用 jstack pid 查看该进程下每个线程当前的状态。输出信息中有 tid 线程当前状态、源代码行数等信息，可以根据第二步找到有问题的 tid 找出 jstack 中相匹配的信息对应的源代码。

#### 如何查看 GCRoot？
+ 先堆转储文件。
+ 用 MAT 打开文件，可查看 GCRoot。