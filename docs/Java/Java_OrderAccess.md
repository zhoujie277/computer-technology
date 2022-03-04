# Java8 JVM 系列文档注释

[TOC]

## OrderAccess.hpp

### Memory Access Ordering Model
This interface is based on the JSR-133 Cookbook for Compiler Writers and on the IA64 memory model.  It is the dynamic equivalent of the C/C++ volatile specifier.  I.e., volatility restricts compile-time memory access reordering in a way similar to what we want to occur at runtime.

这个接口是基于 JSR-133 Cookbook for Compiler Writers 和 IA64 内存模型的。 它是 C/C++ 的 volatile 指定器的动态等价物。 也就是说，波动性限制了编译时的内存访问重排，其方式类似于我们希望在运行时发生的情况。

In the following, the terms 'previous', 'subsequent', 'before', 'after', 'preceding' and 'succeeding' refer to program order.  The terms 'down' and 'below' refer to forward load or store motion relative to program order, while 'up' and 'above' refer to backward motion. 

在下文中，术语 'previous', 'subsequent', 'before', 'after', 'preceding' and 'succeeding' 是指程序顺序。 术语 'down' and 'below' 指的是相对于程序顺序的向前加载或存储运动，而 'up' and 'above' 指的是向后运动。

We define four primitive memory barrier operations.

我们定义了四个原始的内存屏障操作。

```
    LoadLoad:   Load1(s); LoadLoad; Load2
```

Ensures that Load1 completes (obtains the value it loads from memory) before Load2 and any subsequent load operations.  Loads before Load1 may not float below Load2 and any subsequent load operations.

确保 Load1 在 Load2 和任何后续的加载操作之前完成（从内存中获得其加载的值）。 在 Load1 之前的加载操作不得浮动在 Load2 和任何后续加载操作之下。

```
    StoreStore: Store1(s); StoreStore; Store2
```

Ensures that Store1 completes (the effect on memory of Store1 is made visible to other processors) before Store2 and any subsequent store operations.  Stores before Store1 may not float below Store2 and any subsequent store operations.

确保 Store 1 在 Store 2 和任何后续存储操作之前完成（Store1 对内存的影响对其他处理器可见）。 Store1 之前的存储不得浮动在 Store2 和任何后续存储操作之下。

```
    LoadStore:  Load1(s); LoadStore; Store2
```

Ensures that Load1 completes before Store2 and any subsequent store operations.  Loads before Load1 may not float below Store2 and any subseqeuent store operations.

确保 Load1 在 Store2 和任何后续存储操作之前完成。 在 Load1 之前的加载不能浮动于 Store2 和任何后续的存储操作。

```
    StoreLoad:  Store1(s); StoreLoad; Load2
```

Ensures that Store1 completes before Load2 and any subsequent load operations.  Stores before Store1 may not float below Load2 and any subseqeuent load operations.

确保 Store1 在 Load2 和任何后续加载操作之前完成。 Store1 之前的存储不得漂浮在 Load2 和任何后续的加载操作之下。

We define two further operations, 'release' and 'acquire'.  They are mirror images of each other.

我们再定义两个操作，'release' and 'acquire'。 它们是彼此的镜像。

Execution by a processor of release makes the effect of all memory accesses issued by it previous to the release visible to all processors before the release completes.  The effect of subsequent memory accesses issued by it may be made visible before the release.  I.e., subsequent memory accesses may float above the release, but prior ones may not float below it.

一个处理器执行 release，会使它在 release 之前发出的所有内存访问的效果在 release 完成之前对所有处理器可见。它所发出的后续内存访问的效果可能在 release 前就已经可见。 也就是说，随后的内存访问可以浮动在 release 之上，但之前的访问不能浮动在 release 之下。

Execution by a processor of acquire makes the effect of all memory accesses issued by it subsequent to the acquire visible to all processors after the acquire completes.  The effect of prior memory accesses issued by it may be made visible after the acquire. I.e., prior memory accesses may float below the acquire, but subsequent ones may not float above it.

一个处理器执行 acquire，会使它在 acquire 之后发出的所有内存访问的效果在 acquire 完成后对所有处理器可见。 它所发出的先前的内存访问的效果可能会在 acquire 后变得可见。也就是说，先前的内存访问可以浮动在 acquire 的下方，但随后的访问不能浮动在 acquire 的上方。

Finally, we define a 'fence' operation, which conceptually is a release combined with an acquire.  In the real world these operations require one or more machine instructions which can float above and below the release or acquire, so we usually can't just issue the release-acquire back-to-back.  All machines we know of implement some sort of memory fence instruction.

最后，我们定义了一个 fence 操作，从概念上讲，它是一个 release 和一个 acquire 的结合。 在现实世界中，这些操作需要一个或多个机器指令，这些指令可以在 release 或 acquire 的上方和下方浮动，所以我们通常不能背对背地发出 release-acquire 指令。 我们所知的所有机器都实现了某种内存屏障指令。

The standalone implementations of release and acquire need an associated dummy volatile store or load respectively.  To avoid redundant operations, we can define the composite operators: 'release_store', 'store_fence' and 'load_acquire'.  Here's a summary of the machine instructions corresponding to each operation.

release 和 acquire 的独立实现分别需要一个相关的假的 volatile 存储或加载。 为了避免多余的操作，我们可以定义复合操作符。'release_store', 'store_fence' 和 'load_acquire'。 下面是每个操作所对应的机器指令的摘要。

```
               sparc RMO             ia64             x86
 ---------------------------------------------------------------------
 fence         membar #LoadStore |   mf               lock addl 0,(sp)
                      #StoreStore |
                      #LoadLoad |
                      #StoreLoad

 release       membar #LoadStore |   st.rel [sp]=r0   movl $0,<dummy>
                      #StoreStore
               st %g0,[]

 acquire       ld [%sp],%g0          ld.acq <r>=[sp]  movl (sp),<r>
               membar #LoadLoad |
                      #LoadStore

 release_store membar #LoadStore |   st.rel           <store>
                      #StoreStore
               st

 store_fence   st                    st               lock xchg
               fence                 mf

 load_acquire  ld                    ld.acq           <load>
               membar #LoadLoad |
                      #LoadStore
```

Using only release_store and load_acquire, we can implement the following ordered sequences.

只使用 release_store 和 load_acquire，我们就可以实现以下的有序序列。

```
    1. load, load   == load_acquire,  load
                    or load_acquire,  load_acquire
    2. load, store  == load,          release_store
                    or load_acquire,  store
                    or load_acquire,  release_store
    3. store, store == store,         release_store
                or release_store, release_store
```

These require no membar instructions for sparc-TSO and no extra instructions for ia64.

这些对于 sparc-TSO 来说不需要 membar 指令，对于 ia64 来说也不需要额外的指令。

Ordering a load relative to preceding stores requires a store_fence, which implies a membar #StoreLoad between the store and load under sparc-TSO.  A fence is required by ia64.  On x86, we use locked xchg.

在 sparc-TSO 下，相对于前面的存储，对负载的排序需要一个 store_fence，这意味着存储和负载之间有一个 membar #StoreLoad。 ia64 也需要一个栅栏。 在 x86 上，我们使用锁定的 xchg。

```
    4. store, load  == store_fence, load
```

Use store_fence to make sure all stores done in an 'interesting' region are made visible prior to both subsequent loads and stores.

使用 store_fence 来确保在一个 "有趣 "的区域所做的所有存储在随后的加载和存储之前都是可见的。

Conventional usage is to issue a load_acquire for ordered loads.  Use release_store for ordered stores when you care only that prior stores are visible before the release_store, but don't care exactly when the store associated with the release_store becomes visible.  Use release_store_fence to update values like the thread state, where we don't want the current thread to continue until all our prior memory accesses (including the new thread state) are visible to other threads.

传统的用法是为有序的加载发出 load_acquire。 当你只关心先前的存储在 release_store 之前是否可见，但并不关心与 release_store 相关的存储何时可见时，可以使用release_store 进行有序存储。 使用 release_store_fence 来更新像线程状态这样的值，在这种情况下，我们不希望当前线程继续下去，直到我们所有先前的内存访问（包括新的线程状态）对其他线程可见。

### C++ Volatility
C++ guarantees ordering at operations termed 'sequence points' (defined to be volatile accesses and calls to library I/O functions).  'Side effects' (defined as volatile accesses, calls to library I/O functions and object modification) previous to a sequence point must be visible at that sequence point.  See the C++ standard, section 1.9, titled "Program Execution".  This means that all barrier implementations, including standalone loadload, storestore, loadstore, storeload, acquire and release must include a sequence point, usually via a volatile memory access.  Other ways to guarantee a sequence point are, e.g., use of indirect calls and linux's \__asm__ volatile. 

C++ 保证在被称为 "序列点"（定义为 volatile 访问和对库 I/O 函数的调用）的操作中进行排序。 在序列点之前的 "副作用"（定义为 volatile 访问、调用库 I/O 函数和对象修改）必须在该序列点上可见。 见 C++ 标准第 1.9 节，标题为 "程序执行"。 这意味着所有屏障的实现，包括独立的 loadload、storestore、storeload、acquire 和 release 都必须包括一个序列点，通常是通过一个 volatile 内存访问。 其他保证序列点的方法是，例如，使用间接调用和 linux 的 \__asm__ volatile。

Note: as of 6973570, we have replaced the originally static "dummy" field (see above) by a volatile store to the stack. All of the versions of the compilers that we currently use (SunStudio, gcc and VC++) respect the semantics of volatile here. If you build HotSpot using other compilers, you may need to verify that no compiler reordering occurs across the sequence point respresented by the volatile access.

注意：从 6973570 开始，我们已经用一个 volatile 存储到堆栈的方法取代了原来的静态 "哑"字段（见上文）。我们目前使用的所有编译器版本（SunStudio、gcc 和 VC++）都尊重这里的 volatile 语义。如果你使用其他编译器构建 HotSpot，你可能需要验证编译器是否在 volatile 访问所代表的序列点上发生重排。

### os::is_MP Considered Redundant
Callers of this interface do not need to test os::is_MP() before issuing an operation. The test is taken care of by the implementation of the interface (depending on the vm version and platform, the test may or may not be actually done by the implementation).

这个接口的调用者不需要在发出操作前测试 os::is_MP()。该测试由接口的实现负责（取决于 vm 的版本和平台，测试可能由实现完成，也可能不由实现完成）。

### A Note on Memory Ordering and Cache Coherency
Cache coherency and memory ordering are orthogonal concepts, though they interact.  E.g., all existing itanium machines are cache-coherent, but the hardware can freely reorder loads wrt other loads unless it sees a load-acquire instruction.  All existing sparc machines are cache-coherent and, unlike itanium, TSO guarantees that the hardware orders loads wrt loads and stores, and stores wrt to each other.

缓存一致性和内存排序是正交的概念，尽管它们相互影响。 例如，所有现有的 Itanium 机器都是高速缓存一致性的，但是硬件可以自由地将负载与其他负载重新排序，除非它看到一个 load-acquire 指令。 所有现有的 Sparc 机器都是高速缓存一致的，而且与 Itanium 不同的是，TSO 保证硬件对负载和存储的排序，以及对存储的排序。

Consider the implementation of loadload.  If your platform isn't cache-coherent, then loadload must not only prevent hardware load instruction reordering, but it must also ensure that subsequent loads from addresses that could be written by other processors (i.e., that are broadcast by other processors) go all the way to the first level of memory shared by those processors and the one issuing the loadload.

考虑一下 loadload 的实现。 如果你的平台不是高速缓存一致性的，那么 loadload 不仅要防止硬件加载指令的重新排序，还必须确保随后从可能被其他处理器写入的地址（即被其他处理器广播的地址）加载，一直到这些处理器和发出 loadload 的处理器共享的第一层内存。

So if we have a MP that has, say, a per-processor D$ that doesn't see writes by other processors, and has a shared E$ that does, the loadload barrier would have to make sure that either
1. cache lines in the issuing processor's D$ that contained data from addresses that could be written by other processors are invalidated, so subsequent loads from those addresses go to the E$, (it could do this by tagging such cache lines as 'shared', though how to tell the hardware to do the tagging is an interesting problem), or 
2. there never are such cache lines in the issuing processor's D\$, which means all references to shared data (however identified: see above) bypass the D\$ (i.e., are satisfied from the E$).

因此，如果我们有一个 MP，比如说，每个处理器的 D$ 不会看到其他处理器的写入，而有一个共享的 E$ 会看到，loadload 屏障必须确保
1. 发出处理器的 D$ 中包含其他处理器可以写入的地址的数据的缓存行被无效，所以随后从这些地址加载的数据会进入 E$，（它可以通过将这些缓存行标记为 ‘shared' 来做到这一点，尽管如何告诉硬件进行标记是一个有趣的问题），或 
2. 发行处理器的 D$ 中从来没有这样的缓存行，这意味着对共享数据的所有引用（无论如何识别：见上文）都会绕过 D$ (即从 E$ 中满足）。

If your machine doesn't have an E\$, substitute 'main memory' for 'E$'.

如果你的机器没有 E$ ，用 "主存储器" 代替 "E$"。

Either of these alternatives is a pain, so no current machine we know of has incoherent caches.

这两种方法都很麻烦，所以目前我们知道的机器都没有不一致的缓存。

If loadload didn't have these properties, the store-release sequence for publishing a shared data structure wouldn't work, because a processor trying to read data newly published by another processor might go to its own incoherent caches to satisfy the read instead of to the newly written shared memory.

如果 loadload 没有这些属性，发布共享数据结构的 store-release 序列就无法工作，因为试图读取另一个处理器新发布的数据的处理器可能会去自己的不一致的缓存来满足读取，而不是去新写入的共享内存。

### NOTE WELL!!
#### A Note on MutexLocker and Friends
See mutexLocker.hpp.  We assume throughout the VM that MutexLocker's and friends' constructors do a fence, a lock and an acquire in that order.  And that their destructors do a release and unlock, in that order.  If their implementations change such that these assumptions are violated, a whole lot of code will break.

参见 mutexLocker.hpp。 在整个虚拟机中，我们假设 MutexLocker 和它的朋友们的构造函数按照这个顺序做了一个栅栏、一个锁和一个获取。 他们的析构器也按这个顺序进行释放和解锁。 如果它们的实现发生了变化，从而违反了这些假设，那么大量的代码将被破坏。
