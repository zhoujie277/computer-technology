# 深入理解 Java 虚拟机
> 《深入理解 Java 虚拟机》读书笔记

[TOC]

## 概述

### HotSpot VM 有哪几个主要组件？
VM 运行时（Runtime）、JIT 编译器（JIT Compiler）、内存管理器（Memory Manager）。

## 自动内存管理

### Java 内存区域和内存溢出异常

#### Java 虚拟机运行时数据区是如何划分的？
Java 虚拟机在执行 Java 程序的过程中会把它所管理的内存划分为若干个不同的数据区域。根据《Java 虚拟机规范》的规定，Java 虚拟机所管理的内存将会包括以下几个运行时数据区域。分别是：
+ 程序计数器。取指，译码，执行。
+ Java 虚拟机栈。线程私有，描述的是 Java 方法执行的线程内存模型：每个方法被执行的时候，Java 虚拟机都会同步创建一个栈帧。
  + 栈帧。存储局部变量表、操作数栈、动态链接、方法出口等信息。
+ 本地方法栈。为虚拟机使用到的本地方法服务。
+ Java 堆。
+ 方法区。各个线程共享的内存区域，它用于存储已被虚拟机加载的类型信息、常量、静态变量、即时编译器编译后的代码缓存等数据。在《Java 虚拟机规范》中，方法区被描述为堆的一个逻辑部分。但是它却有一个别名--“非堆”。以区别于 Java 堆。
  + 永久代。在 Java6 以及之前，Hotspot 虚拟机选择永久代来实现方法区。
  + 元空间。JDK8 以后，完全废弃了永久代，改用元空间来实现方法区。
  + 运行时常量池。方法区的一部分。
+ 直接内存。可以使用 Unsafe.allocateMemory/freeMemory 或者 NIO DirectByteBuffer 管理堆外内存。

#### 试描述 Java 中对象创建的过程。
```
  Object obj = new Object();

  // 上面代码对应的字节码
  0 new #2 <java/lang/Object>
  3 dup
  4 invokespecial #1 <java/lang/Object.<init>>
  7 astore_1
  8 return
```

从上面生成的字节码可以看到，当 JVM 执行 new Object() 这样的代码时，会发生如下步骤。
1. 首先会解释字节码 new 指令。它将去检查这个指令的参数是否能在常量池中定位到一个类的符号引用，并且检查这个符号引用代表的类是否已加载、解析和初始化。如果没有，将先执行相应的类加载过程。
2. 接下来 JVM 将为新生对象分配内存。对象所需内存大小在类加载完成后便可完全确定。
3. 决定内存分配的位置。可能是在 TLAB 中分配内存，如果虚拟机允许的话（比如 -XX:+UseTLAB) 。否则，将会进行一次 CAS + 重试的方式保证分配完内存更新指针操作的原子性。
4. 将分配到的内存空间都初始化为零。
5. 设置对象头的信息。包括这个对象是哪个类的实例、对象的 GC 分代年龄、是否启用偏向锁等信息。
6. 调用 Java 程序的构造函数 \<init>() 方法。即字节码指令中 invokespecial 的指令。

以下是 bytecodeInterpreter.cpp 关于 new 关键字执行的代码片段。
```
  CASE(_new): {
      u2 index = Bytes::get_Java_u2(pc+1);
      ConstantPool* constants = istate->method()->constants();
      if (!constants->tag_at(index).is_unresolved_klass()) {
        // Make sure klass is initialized and doesn't have a finalizer
        Klass* entry = constants->slot_at(index).get_klass();
        assert(entry->is_klass(), "Should be resolved klass");
        Klass* k_entry = (Klass*) entry;
        assert(k_entry->oop_is_instance(), "Should be InstanceKlass");
        InstanceKlass* ik = (InstanceKlass*) k_entry;
        if ( ik->is_initialized() && ik->can_be_fastpath_allocated() ) {
          size_t obj_size = ik->size_helper();
          oop result = NULL;
          // If the TLAB isn't pre-zeroed then we'll have to do it
          bool need_zero = !ZeroTLAB;
          if (UseTLAB) {
            result = (oop) THREAD->tlab().allocate(obj_size);
          }
          if (result == NULL) {
            need_zero = true;
            // Try allocate in shared eden
      retry:
            HeapWord* compare_to = *Universe::heap()->top_addr();
            HeapWord* new_top = compare_to + obj_size;
            if (new_top <= *Universe::heap()->end_addr()) {
              if (Atomic::cmpxchg_ptr(new_top, Universe::heap()->top_addr(), compare_to) != compare_to) {
                goto retry;
              }
              result = (oop) compare_to;
            }
          }
          if (result != NULL) {
            // Initialize object (if nonzero size and need) and then the header
            if (need_zero ) {
              HeapWord* to_zero = (HeapWord*) result + sizeof(oopDesc) / oopSize;
              obj_size -= sizeof(oopDesc) / oopSize;
              if (obj_size > 0 ) {
                memset(to_zero, 0, obj_size * HeapWordSize);
              }
            }
            if (UseBiasedLocking) {
              result->set_mark(ik->prototype_header());
            } else {
              result->set_mark(markOopDesc::prototype());
            }
            result->set_klass_gap(0);
            result->set_klass(k_entry);
            SET_STACK_OBJECT(result, 0);
            UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);
          }
        }
      }
      // Slow case allocation
      CALL_VM(InterpreterRuntime::_new(THREAD, METHOD->constants(), index),
              handle_exception);
      SET_STACK_OBJECT(THREAD->vm_result(), 0);
      THREAD->set_vm_result(NULL);
      UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);
    }
```

#### 试描述 Java 中对象的内存布局。
在 HotSpot 虚拟机里，对象在堆内存中的存储布局可以划分为三个部分：对象头(Header)、实例数据（Instance Data）和对齐填充（Padding）。
+ 对象头。HopSpot 虚拟机对象的对象头部分包括两类信息。
  + 一部分是用于存储对象自身的运行时数据。如哈希码、GC 分代年龄、锁状态标志、线程持有的锁、偏向线程 ID、偏向时间戳等。
  + 另一部分是类型指针。即对象指向它的类型元数据的指针，Java 虚拟机通过这个指针来确定该对象是哪个类的实例。

#### 试举出几种不同的内存错误
+ 普通的 Java 堆内存异常。当 Java 堆空闲容量无法承载新对象需要的分配内存请求时，会抛出 OutOfMemoryError。
+ 虚拟机栈的内存错误。
  + 如果虚拟机的栈内存允许动态扩展。虚拟机栈扩容时，却无法申请到足够的内存时，将抛出 OutOfMemoryError。
  + 当无限制的分配线程时，由于每个线程都会占用一定的虚拟机栈内存，最终因申请不到足够的内存而创建失败时，会抛出 OutOfMemoryError。
  + 当线程请求的栈深度大于虚拟机所允许的最大深度，将抛出 StackOverflowError。
  + 当栈帧太大或者虚拟机栈容量太小，以至于新的栈帧无法分配的时候，将抛出 StackOverflowError。
+ 如果 JVM 花费了 98% 的时间进行垃圾回收，而只得到 2% 可用的内存，频繁的进行内存回收(最起码已经进行了 5 次连续的垃圾回收)，JVM 就会曝出 java.lang.OutOfMemoryError: GC overhead limit exceeded 错误。
  > Exception in thread thread_name: java.lang.OutOfMemoryError: GC Overhead limit exceeded Cause: The detail message "GC overhead limit exceeded" indicates that the garbage collector is running all the time and Java program is making very slow progress. After a garbage collection, if the Java process is spending more than approximately 98% of its time doing garbage collection and if it is recovering less than 2% of the heap and has been doing so far the last 5 (compile time constant) consecutive garbage collections, then a java.lang.OutOfMemoryError is thrown. This exception is typically thrown because the amount of live data barely fits into the Java heap having little free space for new allocations.
  > Action: Increase the heap size. The java.lang.OutOfMemoryError exception for GC Overhead limit exceeded can be turned off with the command line flag -XX:-UseGCOverheadLimit.
+ 在 JDK8 中，若往字符串常量池不断的添加字符串，也会导致堆内存溢出。
  ```
    void metaspaceMemory() {
        List<String> list = new ArrayList<>();
        int i = 0;
        try {
            for (int j = 0; j < 2600000; j++) {
                list.add(String.valueOf(j).intern());
                i++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println(i);
        }
    }

    java.lang.OutOfMemoryError: Java heap space
      at java.lang.Integer.toString(Integer.java:403)
      at java.lang.String.valueOf(String.java:3099)
  ```
+ 直接内存导致的内存溢出。
  ```
    List<ByteBuffer> buf = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      buf.add(ByteBuffer.allocateDirect(_2MB));
    }

    Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
      at java.nio.Bits.reserveMemory(Bits.java:695)
      at java.nio.DirectByteBuffer.<init>(DirectByteBuffer.java:123)
      at java.nio.ByteBuffer.allocateDirect(ByteBuffer.java:311)
  ```

### 垃圾收集器与内存分配策略

#### 在自动内存管理系统中，如何知道哪些对象是需要被回收的？
若要确定哪些对象能被回收，需要先确定该自动内存管理系统中采用的是什么垃圾标识算法。通常垃圾标识算法有如下两种。
+ 引用计数法。采用该算法识别对象是否存亡的标志取决于该对象的引用计数是否为 0，如果该对象引用计数为 0，则该对象被视为垃圾，是需要被回收的。
+ 可达性分析算法。该算法通过一系列称为“GC Roots”的根对象作为起始结点集，从这些结点开始根据引用向下搜索，搜索过程走过的路径称为引用链。如果某个对象到 GC Roots 间没有引用链相连，则说明此对象是不可能再被使用的。

#### finalize() 方法有什么用？
当对象被标记为垃圾，即将被回收之前，JVM 会将重写了 finalize() 且没有被调用过的对象送入 Finalizer 的引用队列，由 Finalizer 线程从该队列获取该对象，并调用该对象的 finalize()，如果这次 finalize() 方法执行过后，该对象依然被判定为垃圾，则该对象会被真正回收。

#### 方法区中的对象会被回收么？
方法区的垃圾收集主要回收两部分内容：废弃的常量和不再使用的类型。

#### JVM 如何判定一个类型是否属于”不再被使用的类”
需要满足下面三个条件。
1. 该类所有的实例都已经被回收，也就是 Java 堆中不存在该类及其任何派生子类的实例。
2. 加载该类的类加载器已经被回收。
3. 该类对应的 java.lang.Class 对象没有在任何地方被引用，无法在任何地方通过反射访问该类的方法。


#### 垃圾收集算法

##### 自动内存管理有哪些隐患
1. 对资源的占用。垃圾收集器通常要占用线程以及堆内存等资源。它和引用程序一起执行时，很可能会降低应用程序的吞吐量。
2. 延迟。不同的垃圾收集器都会有一定程度的 stop-the-world 现象。可能是在标记对象可达性分析时，也有可能是在内存压缩整理时。其会暂停应用程序的堆内存访问。
3. 内存碎片。根据采取垃圾收集算法的不同，可能会产生一定程度的内存碎片，或者需要消耗额外的资源去清理碎片。

##### 分代收集理论有哪些？
1. 弱分代假说。绝大多数对象都是朝生夕死的。
2. 强分代假说。熬过越多次垃圾收集过程的对象就越难以消亡。
3. 跨代引用假说。跨代引用相对于同代引用来说仅占极少数。

##### 试简单描述分代假说
分代假说是指一种内存管理技术。它将内存分为多个代，其中第一个代称为初始代，每当产生一个新对象时，总是在初始代分配，然后每轮垃圾收集后，存活的对象就被移到下一个分代，直至移到最后一个分代为止。（注意，此处的“一轮”可以是一次，也可以是多次收集为一轮）

而在现有的现实情况中，分代收集通常将堆内存空间划分为被称作新生代和老年代的两个区域。一般情况下，一个对象首先会在新生代区域内分配，然后随着每一次垃圾收集的动作发生，存在于新生代区域的对象会被逐步晋升到老年代中去。另外，通常情况下，每一次垃圾收集，存在于新生代的大部分对象基本都会被回收。只有少部分或没有对象会逐渐晋升到老年代中。即弱分代假说，这个假说很重要，且这个假说成立的程度决定了分代收集器的性能。


##### 试描述标记-清除算法
算法分为标记-清除两个阶段。首先标记出所有需要回收的对象，在标记完成后，统一回收掉所有被标记的对象。其使用起来的注意点有两个。
1. 执行效率不稳定。如果 Java 堆中包含大量对象，而且其中大部分是需要被回收的，这时须进行大量标记和清除的动作，导致标记和清除两个过程的执行效率都随对象数量增长而降低；
2. 内存空间的碎片化问题。标记、清除之后会产生大量不连续的内存碎片，这样的结果是可能在后续需要分配较大对象时，找不到足够的连续内存而不得不提前触发另一次垃圾收集动作。

##### 试描述标记-复制算法
（此处忽略半区复制算法）在 HotSpot 虚拟机中，将新生代分为一块较大的 Eden 空间和两块较小的 Survivor 空间，每次分配内存只使用 Eden 和其中一块 Survivor，发生垃圾收集时，将 Eden 和 Survivor 中仍然存活的对象一次性复制到另外一块 Survivor 空间上，然后直接清理掉 Eden 和 已用过的一块 Survivor 空间。其使用起来的注意点有两个。
1. Eden 区太小，则导致过多的短命对象被晋升到了老年代。Eden 区太大，则可能导致新生代垃圾收集收集速度效率不高。
2. 需要有其他内存区域作为分配担保机制。即当仅有的一块 Survivor 空间不足以容纳一次 Minor GC 之后存活的对象时，就需要分配到其他区域。所以通常，老年代不能使用复制算法。

##### 试描述标记-整理算法
该算法分为清除-整理阶段。首先标记出所有需要回收的对象，在标记完成后，让所有存活的对象都向内存空间一端移动，然后直接清理掉边界以外的内存。其优点是不会产生内存碎片。但是要注意的是，由于要移动对象的缘故，而且该算法一般应用在老年代中，需要移动的对象通常都会比较多。所以导致应用程序暂停的时间会比标记-清除要长，可能要长的多。

##### 垃圾收集过程中，哪些阶段需要停顿以及如何实现用户线程停顿的？
+ 无论哪种收集器，在 GCRoot 枚举时一定会停顿，只是根据实现的方式，停顿时间可以比较高效。
+ 在非并发收集器中，在清理垃圾阶段都会有停顿。
+ 在 CMS 收集器中，初始标记和重新标记阶段会暂停用户线程，并发标记和并发清理不需要，但由于发生并发模式失败导致的 Full GC 也会暂停用户线程。
+ 暂停用户线程一般是在被称为安全点或者安全区域内实现的。即用户线程只有到达了安全点或者处于安全区域才会停止，所以线程停止后，此时垃圾收集器可以开始工作。

##### 什么是跨代引用？试举一个代码实例。
+ 是指在部分区域垃圾收集方式中，典型的如老年代和新生代，老年代的存活对象含有对新生代的对象存在着引用，导致在新生代的对象无法确定它到底能不能被收集，如果老年代有引用，则它不能被垃圾收集，又或者，老年代已经可以被回收了，新生代对象也可以被收集。总之，新生代对象只有被老年代对象引用，就必须要检查引用它的老年代对象，从而确定垃圾收集不会发生错误。
+ 典型的可能会发生跨代引用的现象是，不恰当的实现一个先进先出的链表，比如，一个链表的头部很久没出队了，然后一直有结点在队尾入队。新入队的结点会在新生代分配，头部的结点已经被晋升到老年代中了。突然在某一个时刻，头部的元素一直不间断出队，并且出队的结点没有断开 next 的引用，此时就会出现跨代引用的现象。

##### 记忆集与卡表是什么？
+ 记忆集是一种用于记录非收集区域指向收集区域的指针集合。卡表，是记忆集的一种实现方式。一张卡（卡页）代表一块内存区域。以卡为单位来记录该区域内是否含有跨代指针。一个卡页通常包含不止一个对象，只要卡页内至少有一个对象的字段存在着跨代引用，那就将对应卡表的数组元素的值标识为 1，称为这个卡变脏。没有则为 0.

##### 什么是三色标记？
三色标记其实是标识对象的三个状态。
+ 白色。表示对象尚未被垃圾收集器访问过。显然在可达性分析刚刚开始的阶段，所有的对象都是白色的，若在分析结束的阶段，仍然是白色的对象，即代表不可达。
+ 黑色。表示对象已经被垃圾收集器访问过，且这个对象的所有引用都已经扫描过。黑色对象表示该对象是存活的，不能被垃圾回收。
+ 灰色。表示对象已经被垃圾收集器访问过，但这个对象上至少存在一个引用还没有被扫描过。

##### 为什么需要三色标记？
标记主要是为了区分对象的状态。因此，为了能标记垃圾对象，至少需要两种状态。即标记 1 来标识可达，而 0 标识不可达。用此算法标记，将不得不将全部用户线程全部挂起，因为无法识别

##### 三色标记在并发收集中会遇到哪些问题？怎么解决？
+ 一个问题是在标记的过程中，已经被标记为黑色（即可达），但随即用户不再需要了，也就是原本消亡的对象被错误的标记为存活。这种处理方式一般是容忍，把它当作逃过这次垃圾收集的浮动垃圾。等待下一次被回收。
+ 原本是白色可被回收的对象，又因为用户操作，使得与被扫描过的黑色对象新链接了起来，这个很可能导致被错误回收，从而引起程序崩溃。

Wilson 于 1994 年在理论上证明了，当且仅当以下两个条件同时满足时，会产生”对象消失”的问题，即原本应该是黑色的对象被误标为白色。
+ 赋值器插入了一条与多条从黑色对象到白色对象的新引用。
+ 赋值器删除了全部从灰色对象到该白色对象的直接或间接引用。

因此，只需要破坏这两个条件的任意一个即可。由此分别产生了两种解决方案：增量更新和原始快照。
+ 增量更新。当黑色对象插入新的指向白色对象的引用关系时，就将这个新插入的引用记录下来，等并发扫描结束之后，再将这些记录过的引用关系中的黑色对象为根，重新扫描一次。
+ 原始快照。当灰色对象将要删除指向白色对象的引用关系时，就将这个将要删除的引用记录下来，在并发扫描结束之后，再将这些记录过的引用关系中的灰色对象为根，重新扫描一次。

##### 试描述垃圾收集器的历史


### 虚拟机性能监控、故障处理工具


## 虚拟机执行子系统

### 虚拟机类加载机制

#### 简要描述下类在加载阶段所做的工作
1. 通过一个类的全限定名来获取定义此类的二进制字节流。
2. 将这个字节流所代表的静态存储结构转化为方法区的运行时数据结构。
3. 在内存中生成一个代表这个类的 java.lang.Class 对象，作为方法区这个类的各种数据的访问入口。

#### 哪些情况下，必须对类进行初始化操作？
有且只有下列六种情况中，将会触发类的初始化
1. 虚拟机启动时，会先初始化这个含有 main 方法的主类
2. 遇到 new、getstatic、putstatic、invokestatic 的字节码指令时。
3. 当初始化类的时候，发现其父类还没有进行过初始化，则需要先触发器父类的初始化。
4. 使用 java.lang.reflect 包的方法对类型进行反射调用的时候，需要先触发其初始化。
5. 当一个接口定义了 JDK8 新加入的默认方法（default 修饰），如果有这个接口的实现类发生了初始化，该接口要在其之前被初始化。
6. 当使用 JDK7 加入的动态语言支持时，如果一个 java.lang.invoke.MethodHandler 实例最后的解析结果为 REF_getStatic 等的方法句柄，并且这个方法句柄对应的类还没有被初始化，则需要先触发其初始化。

#### 试描述类加载过程的双亲委派模型
+ 如果一个类加载器收到了类加载的请求，它首先会将这个请求委托给父加载器去完成，每一个层级的类加载器都是如此，因此所有的加载请求最终都应该传送到最顶层的启动类加载器中，只有当父加载器反馈自己无法完成这个家在请求时，子加载器才会尝试自己去完成加载。

#### 有哪些方法可以破坏双亲委派模型？破坏的目的是什么？
1. 自定义类加载器去加载，并重写 loadClass 方法。可定制自己想要的类加载规则。（如果实现 findClass 方法，则不会破坏双清委派模型）
2. 使用 Thread 类的 contextClassLoader。可通过设置 setContextClassLoader/getContextClassLoader 来定制自己想要的类加载规则。这种情况通常可用于父加载器获取子加载器将子类加载至虚拟机中。

## 程序编译与代码优化

