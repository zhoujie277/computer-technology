## The JSR-133 Cookbook for Compiler Writers
> by Doug Lea, with help from members of the JMM mailing list.

Preface: This document is now of historical interest only. Some of the accounts here of processor support for ordering and atomics are obsolete and should not be relied on. For updated accounts, see, among other sources, the guide to JDK9+ memory order modes. For current processor support, see C++ mappings.

前言: 这份文件现在只具有历史意义。这里关于处理器对排序和原子的支持的一些描述已经过时了，不应该被依赖。关于最新的描述，请参见 JDK9+ 内存排序模式指南等资料。关于当前的处理器支持，请参见 C++ 映射。

This is an unofficial guide to implementing the new Java Memory Model (JMM) specified by JSR-133 . It provides at most brief backgrounds about why various rules exist, instead concentrating on their consequences for compilers and JVMs with respect to instruction reorderings, multiprocessor barrier instructions, and atomic operations. It includes a set of recommended recipes for complying to JSR-133. This guide is "unofficial" because it includes interpretations of particular processor properties and specifications. We cannot guarantee that the intepretations are correct. Also, processor specifications and implementations may change over time.

这是一份关于实现 JSR-133 规定的新的Java内存模型（JMM）的非官方指南。它最多只提供了关于各种规则存在原因的简要背景，而集中于它们对编译器和 JVM 在指令重排、多处理器障碍指令和原子操作方面的影响。它包括一套推荐的配方，以符合 JSR-133 的要求。本指南是 "非官方的"，因为它包括对特定处理器属性和规范的解释。我们不能保证这些解释是正确的。另外，处理器的规格和实现可能会随着时间的推移而改变。

[TOC]

### Reorderings
For a compiler writer, the JMM mainly consists of rules disallowing reorderings of certain instructions that access fields (where "fields" include array elements) as well as monitors (locks).

对于编译器编写者来说，JMM 主要包括不允许对某些访问字段（"字段 "包括数组元素）的指令进行重新排序的规则，以及监控器（锁）。

#### Volatiles and Monitors
The main JMM rules for volatiles and monitors can be viewed as a matrix with cells indicating that you cannot reorder instructions associated with particular sequences of bytecodes. This table is not itself the JMM specification; it is just a useful way of viewing its main consequences for compilers and runtime systems.

JMM 关于 volatiles 和监视器的主要规则可以被看作是一个表格，其中的单元格表示你不能重新排列与特定字节码序列相关的指令。这个表格本身并不是 JMM 规范；它只是一种有用的方式，可以查看它对编译器和运行时系统的主要后果。

1st operation \ 2nd oepration | Normal Load Normal Store | Volatile Load MonitorEnter | Volatile Store MonitorExit 
------------- | -------------- | ------------ | --------------
Normal Load Normal Store   | -  | -  | No
Volatile Load MonitorEnter | No | No | No
Volatile Store MonitorExit | -  | No | No 

Where: 
+ Normal Loads are getfield, getstatic, array load of non-volatile fields.
+ Normal Stores are putfield, putstatic, array store of non-volatile fields
+ Volatile Loads are getfield, getstatic of volatile fields that are acceessible by multiple threads
+ Volatile Stores are putfield, putstatic of volatile fields that are accessible by multiple threads
+ MonitorEnters (including entry to synchronized methods) are for lock objects accessible by multiple threads.
+ MonitorExits (including exit from synchronized methods) are for lock objects accessible by multiple threads.

The cells for Normal Loads are the same as for Normal Stores, those for Volatile Loads are the same as MonitorEnter, and those for Volatile Stores are same as MonitorExit, so they are collapsed together here (but are expanded out as needed in subsequent tables). We consider here only variables that are readable and writable as an atomic unit -- that is, no bit fields, unaligned accesses, or accesses larger than word sizes available on a platform.

正常加载的单元与正常存储的单元相同，volatile load 的单元与 MonitorEnter 相同，volatile stores 的单元与 MonitorExit 相同，所以它们在这里被折叠在一起（但在随后的表格中会根据需要扩展出来）。我们在这里只考虑可作为一个原子单元的可读和可写的变量--也就是说，没有位字段、无符号访问或大于平台上可用字数的访问。

Any number of other operations might be present between the indicated 1st and 2nd operations in the table. So, for example, the "No" in cell [Normal Store, Volatile Store] says that a non-volatile store cannot be reordered with ANY subsequent volatile store; at least any that can make a difference in multithreaded program semantics.

在表格中指定的第 1 和第 2 个操作之间可能存在任何数量的其他操作。因此，例如，单元格 [Normal Store，Volatile Store] 中的 "No" 表示非 volatile store 不能与任何后续的 volatile store 重新排序；至少是任何能在多线程程序语义中产生差异的操作。

The JSR-133 specification is worded such that the rules for both volatiles and monitors apply only to those that may be accessed by multiple threads. If a compiler can somehow (usually only with great effort) prove that a lock is only accessible from a single thread, it may be eliminated. Similarly, a volatile field provably accessible from only a single thread acts as a normal field. More fine-grained analyses and optimizations are also possible, for example, those relying on provable inaccessibility from multiple threads only during certain intervals.

JSR-133 规范的措辞是这样的：挥发物和监视器的规则只适用于那些可能被多个线程访问的锁。如果编译器能够以某种方式（通常需要付出很大的努力）证明一个锁只能由一个线程访问，那么它就可以被消除了。同样，一个可以证明只有一个线程可以访问的 volatile 字段就像一个普通字段一样。更精细的分析和优化也是可能的，例如，那些依赖于可证明的仅在特定时间段内从多个线程不可访问的分析。

Blank cells in the table mean that the reordering is allowed if the accesses aren't otherwise dependent with respect to basic Java semantics (as specified in the JLS). For example even though the table doesn't say so, you can't reorder a load with a subsequent store to the same location. But you can reorder a load and store to two distinct locations, and may wish to do so in the course of various compiler transformations and optimizations. This includes cases that aren't usually thought of as reorderings; for example reusing a computed value based on a loaded field rather than reloading and recomputing the value acts as a reordering. However, the JMM spec permits transformations that eliminate avoidable dependencies, and in turn allow reorderings.

表中空白的单元格意味着，如果访问不依赖于基本的 Java 语义（如 JLS 中规定的），则允许重新排序。例如，即使表格中没有这样说，你也不能将一个加载和一个后续的存储重新排序到同一位置。但是你可以将一个加载和存储重新排序到两个不同的位置，并且在各种编译器转换和优化的过程中可能希望这样做。这包括通常不被认为是重新排序的情况；例如，重新使用一个基于加载字段的计算值，而不是重新加载和重新计算值，这就是重新排序。然而，JMM 规范允许消除可避免的依赖关系的转换，并反过来允许重新排序。

In all cases, permitted reorderings must maintain minimal Java safety properties even when accesses are incorrectly synchronized by programmers: All observed field values must be either the default zero/null "pre-construction" values, or those written by some thread. This usually entails zeroing all heap memory holding objects before it is used in constructors and never reordering other loads with the zeroing stores. A good way to do this is to zero out reclaimed memory within the garbage collector. See the JSR-133 spec for rules dealing with other corner cases surrounding safety guarantees.

在所有情况下，即使程序员不正确地同步访问，允许的重新排序也必须保持最小的 Java 安全属性。所有观察到的字段值必须是默认的零/空的 "构建前 "值，或者是由某个线程编写的值。这通常需要在构造函数中使用之前将所有持有对象的堆内存清零，并且永远不要用清零的存储来重新排序其他的加载。一个好的方法是在垃圾收集器内将回收的内存清零。请参阅 JSR-133 规范，了解处理围绕安全保证的其他角落情况的规则。

The rules and properties described here are for accesses to Java-level fields. In practice, these will additionally interact with accesses to internal bookkeeping fields and data, for example object headers, GC tables, and dynamically generated code.

这里描述的规则和属性是针对对 Java 级别字段的访问。在实践中，这些将额外地与对内部记账字段和数据的访问相互作用，例如对象头、GC 表和动态生成的代码。

#### Final Fields
Loads and Stores of final fields act as "normal" accesses with respect to locks and volatiles, but impose two additional reordering rules:

在 lock 和 volatile 方面，final 字段的加载和存储与 "正常 "访问一样，但施加了两个额外的重排序规则。

1. A store of a final field (inside a constructor) and, if the field is a reference, any store that this final can reference, cannot be reordered with a subsequent store (outside that constructor) of the reference to the object holding that field into a variable accessible to other threads. 

一个 final 字段的存储（在一个构造函数内），以及如果该字段是一个引用，这个 final 字段可以引用的任何存储，不能与随后的存储（在该构造函数外）重新排序，将持有该字段的对象的引用存储到一个其他线程可以访问的变量中。比如，你不能重排序
```
    x.finalField = v; ...; sharedRef = x;
```

This comes into play for example when inlining constructors, where "..." spans the logical end of the constructor. You cannot move stores of finals within constructors down below a store outside of the constructor that might make the object visible to other threads. (As seen below, this may also require issuing a barrier). Similarly, you cannot reorder either of the first two with the third assignment in: v.afield = 1; x.finalField = v; ... ; sharedRef = x;

例如，当内联构造函数时，"... "横跨构造函数的逻辑末端，这一点就会发挥作用。你不能把构造函数中的参数存储空间移到构造函数外的存储空间下面，因为这可能会使该对象对其他线程可见。(正如下面所见，这可能还需要发布一个屏障）。同样，你不能用第三项任务来重新排列前两项中的任何一项。
```
    v.afield = 1; x.finalField = v; ...; sharedRef = x;
```

2. The initial load (i.e., the very first encounter by a thread) of a final field cannot be reordered with the initial load of the reference to the object containing the final field. This comes into play in: x = sharedRef; ... ; i = x.finalField;
A compiler would never reorder these since they are dependent, but there can be consequences of this rule on some processors.

一个 final 字段的初始加载（即线程第一次遇到）不能与包含 final 字段的对象的引用的初始加载重新排序。这在以下情况下会发生作用。
```
    x = sharedRef; ... ; i = x.finalField;
```
编译器永远不会对这些东西进行重新排序，因为它们是相互依赖的，但在某些处理器上可能会出现这一规则的后果。

These rules imply that reliable use of final fields by Java programmers requires that the load of a shared reference to an object with a final field itself be synchronized, volatile, or final, or derived from such a load, thus ultimately ordering the initializing stores in constructors with subsequent uses outside constructors.

这些规则意味着 Java 程序员对 final 字段的可靠使用要求对具有 final 字段的对象的共享引用的加载本身是同步的、易失的或最终的，或者是从这样的加载中派生出来的，从而最终在构造函数中的初始化存储与构造函数之外的后续使用排序。

### Memory Barriers
Compilers and processors must both obey reordering rules. No particular effort is required to ensure that uniprocessors maintain proper ordering, since they all guarantee "as-if-sequential" consistency. But on multiprocessors, guaranteeing conformance often requires emitting barrier instructions. Even if a compiler optimizes away a field access (for example because a loaded value is not used), barriers must still be generated as if the access were still present. (Although see below about independently optimizing away barriers.)

编译器和处理器都必须遵守重排规则。不需要特别努力来确保单处理器保持正确的排序，因为它们都保证了 "如果顺序 "的一致性。但在多处理器上，保证一致性往往需要发出屏障指令。即使编译器优化了一个字段访问（例如因为没有使用一个加载的值），屏障仍然必须被生成，就像访问仍然存在一样。(不过请看下面关于独立优化障碍的内容)。

Memory barriers are only indirectly related to higher-level notions described in memory models such as "acquire" and "release". And memory barriers are not themselves "synchronization barriers". And memory barriers are unrelated to the kinds of "write barriers" used in some garbage collectors. Memory barrier instructions directly control only the interaction of a CPU with its cache, with its write-buffer that holds stores waiting to be flushed to memory, and/or its buffer of waiting loads or speculatively executed instructions. These effects may lead to further interaction among caches, main memory and other processors. But there is nothing in the JMM that mandates any particular form of communication across processors so long as stores eventually become globally performed; i.e., visible across all processors, and that loads retrieve them when they are visible.

内存屏障只是与内存模型中描述的更高层次的概念间接相关，如 "acquire"和 "release"。而且内存屏障本身并不是 "同步屏障"。而且，内存屏障与某些垃圾收集器中使用的 "写屏障 "类型没有关系。内存障碍指令只直接控制 CPU 与其缓存的交互，与其持有等待被刷入内存的存储的写缓冲区，和/或其等待加载或推测执行的指令的缓冲区。这些影响可能会导致缓存、主内存和其他处理器之间的进一步互动。但是在 JMM 中并没有规定任何特定形式的跨处理器的通信，只要存储最终成为全局执行的，即在所有处理器中都是可见的，并且当它们可见时，loads 操作可以检索它们。

#### Categories
Nearly all processors support at least a coarse-grained barrier instruction, often just called a Fence, that guarantees that all loads and stores initiated before the fence will be strictly ordered before any load or store initiated after the fence. This is usually among the most time-consuming instructions on any given processor (often nearly as, or even more expensive than atomic instructions). Most processors additionally support more fine-grained barriers.

几乎所有的处理器都支持至少一个粗粒度的屏障指令，通常被称为 Fence，它保证在 fence 之前启动的所有加载和存储将在 fence 之后启动的任何加载或存储之前被严格排序。这通常是任何给定的处理器上最耗时的指令之一（通常与原子指令差不多，甚至比原子指令更昂贵）。大多数处理器还支持更细粒度的屏障。

A property of memory barriers that takes some getting used to is that they apply BETWEEN memory accesses. Despite the names given for barrier instructions on some processors, the right/best barrier to use depends on the kinds of accesses it separates. Here's a common categorization of barrier types that maps pretty well to specific instructions (sometimes no-ops) on existing processors:

内存屏障的一个需要适应的特性是，它们在内存访问之间适用。尽管在一些处理器上给屏障指令起了名字，但正确/最好的屏障取决于它所分离的访问类型。下面是一个常见的屏障类型的分类，它与现有处理器上的特定指令（有时是 no-ops）有很好的映射。

##### LoadLoad Barriers
> The sequence: Load1; LoadLoad; Load2

ensures that Load1's data are loaded before data accessed by Load2 and all subsequent load instructions are loaded. In general, explicit LoadLoad barriers are needed on processors that perform speculative loads and/or out-of-order processing in which waiting load instructions can bypass waiting stores. On processors that guarantee to always preserve load ordering, the barriers amount to no-ops.

确保 Load1 的数据在 Load2 和所有后续 Load 指令访问的数据被加载之前被加载。一般来说，在执行投机性加载和/或乱序处理的处理器上需要明确的 LoadLoad 障碍，在这种情况下，等待的加载指令可以绕过等待的存储。在保证始终保持加载顺序的处理器上，这些屏障相当于无操作。

##### StoreStore Barriers
> The sequence: Store1; StoreStore; Store2

ensures that Store1's data are visible to other processors (i.e., flushed to memory) before the data associated with Store2 and all subsequent store instructions. In general, StoreStore barriers are needed on processors that do not otherwise guarantee strict ordering of flushes from write buffers and/or caches to other processors or main memory.

确保 Store1 的数据在与 Store2 和所有后续存储指令相关的数据之前对其他处理器可见（即刷新到内存）。一般来说，StoreStore 屏障在处理器上是需要的，因为它不能保证从写缓冲区和/或缓存到其他处理器或主内存的刷新的严格顺序。

##### LoadStore Barriers
> The sequence: Load1; LoadStore; Store2

ensures that Load1's data are loaded before all data associated with Store2 and subsequent store instructions are flushed. LoadStore barriers are needed only on those out-of-order procesors in which waiting store instructions can bypass loads.

确保 Load1 的数据在所有与 Store2 相关的数据和随后的存储指令被刷新之前被加载。LoadStore 障碍只有在那些乱序执行的处理器上才需要，在这些处理器中，等待存储指令可以绕过加载。

##### StoreLoad Barriers
> The sequence: Store1; StoreLoad; Load2

ensures that Store1's data are made visible to other processors (i.e., flushed to main memory) before data accessed by Load2 and all subsequent load instructions are loaded. StoreLoad barriers protect against a subsequent load incorrectly using Store1's data value rather than that from a more recent store to the same location performed by a different processor. Because of this, on the processors discussed below, a StoreLoad is strictly necessary only for separating stores from subsequent loads of the same location(s) as were stored before the barrier. StoreLoad barriers are needed on nearly all recent multiprocessors, and are usually the most expensive kind. Part of the reason they are expensive is that they must disable mechanisms that ordinarily bypass cache to satisfy loads from write-buffers. This might be implemented by letting the buffer fully flush, among other possible stalls.

确保 Store1 的数据在 Load2 和所有后续加载指令访问的数据被加载之前，对其他处理器是可见的（即刷新到主内存）。StoreLoad 屏障可以防止随后的加载错误地使用 Store1 的数据值，而不是由不同的处理器执行的最近的存储到同一位置的数据。正因为如此，在下面讨论的处理器中，StoreLoad 严格来说只需要将存储与后续加载的相同位置分开，因为在屏障之前存储的是相同的位置。几乎所有最新的多核处理器都需要存储加载屏障，而且通常是最昂贵的一种。它们昂贵的部分原因是，它们必须禁用通常绕过缓冲区的机制，以满足来自写缓冲区的负载。这可能是通过让缓冲区完全刷新来实现的，还有其他可能的停顿。

On all processors discussed below, it turns out that instructions that perform StoreLoad also obtain the other three barrier effects, so StoreLoad can serve as a general-purpose (but usually expensive) Fence. (This is an empirical fact, not a necessity.) The opposite doesn't hold though. It is NOT usually the case that issuing any combination of other barriers gives the equivalent of a StoreLoad.

在下面讨论的所有处理器上，事实证明执行 StoreLoad 的指令也能获得其他三个障碍效应，所以 StoreLoad 可以作为一个通用的（但通常是昂贵的）Fence。(这是一个经验性的事实，不是必须的。)但相反的情况并不成立。通常情况下，发出任何其他障碍的组合都不会得到相当于 StoreLoad 的效果。

The following table shows how these barriers correspond to JSR-133 ordering rules.

##### Required barriers

1st Operation \ 2 nd operation | Normal Load | Normal Store | Volatile Load MonitorEnter | Volatile Store MonitorExit
-------------------- | ---- | ---- | ----- | ---
Normal Load  | - | - | - | LoadStore
Normal Store | - | - | - | StoreStore
Volatile Load MonitorEnter | LoadLoad | LoadStore | LoadLoad | LoadStore
Volatile Store MonitorExit | - | - | StoreLoad | StoreStore

Plus the special final-field rule requiring a StoreStore barrier in
```
    x.finalField = v; StoreStore; sharedRef = x;
```

Here's an example showing placements.

```
Java                    | Instructions
----------------------- | ------------
class X {               | 
    int a, b;           |  
    volatile int v, u;  |
    void f () {         |
        int i, j;       |
                        |
        i = a;          | load a
        j = b;          | load b
        i = v;          | load v
                        |     LoadLoad
        j = u;          | load u
                        |     LoadStore
        a = i;          | store a
        b = j;          | store b
                        |     StoreStore
        v = i;          | store v
                        |     StoreStore
        u = j;          | store u
                        |     StoreLoad
        i = u;          | load u
                        |     LoadLoad
                        |     LoadStore
        j = b;          | load b
        a = i;          | store a
    }                   |
}                       |
```

#### Data Dependency and Barriers
The need for LoadLoad and LoadStore barriers on some processors interacts with their ordering guarantees for dependent instructions. On some (most) processors, a load or store that is dependent on the value of a previous load are ordered by the processor without need for an explicit barrier. This commonly arises in two kinds of cases, indirection:

在一些处理器上对 LoadLoad 和 LoadStore 屏障的需要与它们对依赖指令的排序保证相互影响。在一些（大多数）处理器上，依赖于前一个加载值的加载或存储被处理器排序，而不需要明确的屏障。这通常出现在两种情况下，

indirection:
```
    Load x; Load x.field
```
and control 
```
    Load x; if (predicate(x)) Load or Store y;
```

Processors that do NOT respect indirection ordering in particular require barriers for final field access for references initially obtained through shared references:
```
    x = sharedRef; ...; LoadLoad; i = x.finalField;
```

Conversely, as discussed below, processors that DO respect data dependencies provide several opportunities to optimize away LoadLoad and LoadStore barrier instructions that would otherwise need to be issued. (However, dependency does NOT automatically remove the need for StoreLoad barriers on any processor.)

相反，正如下面所讨论的，那些尊重数据依赖性的处理器提供了一些机会来优化 LoadLoad 和 LoadStore 障碍指令，否则这些指令就需要被发布。(然而，依赖性并不能自动消除任何处理器上对 StoreLoad 障碍的需求）。

#### Interactions with Atomic Instructions
The kinds of barriers needed on different processors further interact with implementation of MonitorEnter and MonitorExit. Locking and/or unlocking usually entail the use of atomic conditional update operations CompareAndSwap (CAS) or LoadLinked/StoreConditional (LL/SC) that have the semantics of performing a volatile load followed by a volatile store. While CAS or LL/SC minimally suffice, some processors also support other atomic instructions (for example, an unconditional exchange) that can sometimes be used instead of or in conjunction with atomic conditional updates.

不同处理器上所需的屏障种类与 MonitorEnter 和 MonitorExit 的实现进一步互动。锁定和/或解锁通常需要使用原子条件更新操作 CompareAndSwap（CAS）或 LoadLinked/StoreConditional（LL/SC），其语义是执行 volatile 加载和 volatile 存储。虽然 CAS 或 LL/SC 已经足够了，但一些处理器也支持其他原子指令（例如，无条件交换），有时可以代替原子条件更新或与原子条件更新一起使用。

On all processors, atomic operations protect against read-after-write problems for the locations being read/updated. (Otherwise standard loop-until-success constructions wouldn't work in the desired way.) But processors differ in whether atomic instructions provide more general barrier properties than the implicit StoreLoad for their target locations. On some processors these instructions also intrinsically perform barriers that would otherwise be needed for MonitorEnter/Exit; on others some or all of these barriers must be specifically issued.

在所有的处理器上，原子操作可以防止被读取/更新的位置出现读后写的问题。(否则，标准的循环--直到成功的结构就不能以理想的方式工作）。但是处理器在原子指令是否为其目标位置提供比隐含的 StoreLoad 更普遍的屏障属性方面有所不同。在一些处理器上，这些指令也内在地执行了 MonitorEnter/Exit 所需要的障碍；在其他处理器上，这些障碍的一部分或全部必须被特别发出。

Volatiles and Monitors have to be separated to disentangle these effects, giving:

operation | NormalLoad | Normal Store | Volatile Load | Volatile Store | MonitorEnter | MonitorExit 
---------- | -------- | --------- | ---------- | --------- | --------- | ---------
Normal Load    | - | - | - | LoadStore  | - | LoadStore
Normal Store   | - | - | - | StoreStore | - | StoreExit
Volatile Load  | LoadLoad | LoadStore | LoadLoad | LoadStore | LoadEnter | LoadExit
Volatile Store | - | - | StoreLoad | StoreStore | StoreEnter | StoreExit
MonitorEnter   | EnterLoad | EnterStore | EnterLoad | EnterStore | EnterEnter | EnterExit
MonitorExit    | - | - | ExitLoad | ExitStore | ExitEnter | ExitExit

Plus the special final-field rule requiring a StoreStore barrier in:
```
    x.finalField = v; StoreStore; sharedRef = x;
```

In this table, "Enter" is the same as "Load" and "Exit" is the same as "Store", unless overridden by the use and nature of atomic instructions. In particular:

在这个表中，"Enter"与 "Load "相同，"Exit"与 "Store"相同，除非被原子指令的使用和性质所取代。特别是。
+ EnterLoad is needed on entry to any synchronized block/method that performs a load. It is the same as LoadLoad unless an atomic instruction is used in MonitorEnter and itself provides a barrier with at least the properties of LoadLoad, in which case it is a no-op. 
+ StoreExit is needed on exit of any synchronized block/method that performs a store. It is the same as StoreStore unless an atomic instruction is used in MonitorExit and itself provides a barrier with at least the properties of StoreStore, in which case it is a no-op. 
+ ExitEnter is the same as StoreLoad unless atomic instructions are used in MonitorExit and/or MonitorEnter and at least one of these provide a barrier with at least the properties of StoreLoad, in which case it is a no-op.

+ EnterLoad 在进入任何执行加载的同步块/方法时都需要。它与 LoadLoad 相同，除非在 MonitorEnter 中使用原子指令，并且本身提供了一个至少具有 LoadLoad 属性的屏障，在这种情况下，它是一个无用的。
+ StoreExit 在任何执行存储的同步块/方法的退出时都需要。它与 StoreStore 相同，除非在 MonitorExit 中使用原子指令，并且本身提供了一个至少具有 StoreStore 属性的屏障，在这种情况下，它是一个无用的。
+ ExitEnter 与 StoreLoad 相同，除非在 MonitorExit 和/或 MonitorEnter 中使用了原子指令，并且其中至少有一条提供了一个至少具有 StoreLoad 属性的障碍，在这种情况下，它是一个 no-op。

The other types are specializations that are unlikely to play a role in compilation (see below) and/or reduce to no-ops on current processors. For example, EnterEnter is needed to separate nested MonitorEnters when there are no intervening loads or stores. Here's an example showing placements of most types:

其他类型是不太可能在编译中发挥作用的特殊化（见下文）和/或在当前处理器上减少到无操作。例如， EnterEnter 需要用来分隔嵌套的 MonitorEnter，当中间没有加载或存储时。下面是一个例子，显示了大多数类型的放置。

```
    Java                         |         Instruction
-------------------------------  | ---------------------
class X {                        | 
    int a;                       |
    volatile int v;              |
    void f() {                   |
        int i;                   |
        synchronized (this) {    |
            i = a;               | enter
            a = i;               |      EnterLoad
        }                        |      EnterStore
                                 | load a
                                 | store a
                                 |      LoadExit
                                 |      StoreExit
        synchronized(this) {     | exit
            synchronized(this) { |      ExitEnter
            }                    | enter
        }                        |      EnterEnter
                                 | enter
                                 |      EnterExit
                                 | exit
                                 |      ExitExit
                                 | exit
                                 |      ExitEnter
        i = v;                   |      ExitLoad
        synchronized(this) {     | load v
        }                        |      loadEnter
                                 | enter
                                 |      EnterExit
                                 | exit
                                 |      ExitEnter
        v = i;                   |      ExitStore
        synchronized(this) {     | store v
        }                        |      StoreEnter
    }                            | enter
}                                |      EnterExit
                                 | exit
```

Java-level access to atomic conditional update operations will be available in JDK1.5 via JSR-166 (concurrency utilities) so compilers will need to issue associated code, using a variant of the above table that collapses MonitorEnter and MonitorExit -- semantically, and sometimes in practice, these Java-level atomic updates act as if they are surrounded by locks.

在 JDK1.5 中，将通过 JSR-166（并发工具）提供对原子条件更新操作的 Java 级访问，因此编译器将需要发布相关的代码，使用上表的一个变体来折叠 MonitorEnter 和 MonitorExit -- 在语义上，有时在实践中，这些 Java 级的原子更新就像它们被锁包围一样。

### Multiprocessors
Here's a listing of processors that are commonly used in MPs, along with links to documents providing information about them. (Some require some clicking around from the linked site and/or free registration to access manuals). This isn't an exhaustive list, but it includes processors used in all current and near-future multiprocessor Java implementations I know of. The list and the properties of processors decribed below are not definitive. In some cases I'm just reporting what I read, and could have misread. Several reference manuals are not very clear about some properties relevant to the JMM. Please help make it definitive.

这里列出了通常用于 MP 的处理器，以及提供有关这些处理器信息的文件链接。(有些需要从链接的网站上点击一下和/或免费注册以访问手册）。这并不是一个详尽的列表，但它包括了我所知道的所有当前和未来的多处理器Java 实现中使用的处理器。下面描述的列表和处理器的属性并不是确定的。在某些情况下，我只是报告了我读到的内容，而且可能读错了。一些参考手册对与 JMM 相关的一些属性也不是很清楚。请帮助使其成为明确的。

Good sources of hardware-specific information about barriers and related properties of machines not listed here are Hans Boehm's atomic_ops library, the Linux Kernel Source, and Linux Scalability Effort. Barriers needed in the linux kernel correspond in straightforward ways to those discussed here, and have been ported to most processors. For descriptions of the underlying models supported on different processors, see Sarita Adve et al, Recent Advances in Memory Consistency Models for Hardware Shared-Memory Systems and Sarita Adve and Kourosh Gharachorloo, Shared Memory Consistency Models: A Tutorial.

这里没有列出的关于 Barriers 和机器的相关属性的硬件特定信息的良好来源是 Hans Boehm's atomic_ops library、Linux Kernel Source 和 Linux Scalability Effort。linux 内核中需要的屏障与这里讨论的屏障有直接的对应关系，并且已经被移植到大多数处理器上。关于不同处理器支持的底层模型的描述，见 Sarita Adve et al, Recent Advances in Memory Consistency Models for Hardware Shared-Memory Systems 和 Sarita Adve and Kourosh Gharachorloo, Shared Memory Consistency Models: A Tutorial.


### Recipes

#### Uniprocessors
If you are generating code that is guaranteed to only run on a uniprocessor, then you can probably skip the rest of this section. Because uniprocessors preserve apparent sequential consistency, you never need to issue barriers unless object memory is somehow shared with asynchrononously accessible IO memory. This might occur with specially mapped java.nio buffers, but probably only in ways that affect internal JVM support code, not Java code. Also, it is conceivable that some special barriers would be needed if context switching doesn't entail sufficient synchronization.

如果你生成的代码只保证在单处理器上运行，那么你可能可以跳过本节的其余部分。因为单处理器保留了明显的顺序一致性，你永远不需要发出障碍，除非对象内存以某种方式与异步访问的 IO 内存共享。这可能发生在特殊映射的 java.nio 缓冲区，但可能只影响内部 JVM 支持代码，而不是 Java 代码。另外，可以想象，如果上下文切换不需要足够的同步，就需要一些特殊的障碍。

#### Inserting Barriers
Barrier instructions apply between different kinds of accesses as they occur during execution of a program. Finding an "optimal" placement that minimizes the total number of executed barriers is all but impossible. Compilers often cannot tell if a given load or store will be preceded or followed by another that requires a barrier; for example, when a volatile store is followed by a return. The easiest conservative strategy is to assume that the kind of access requiring the "heaviest" kind of barrier will occur when generating code for any given load, store, lock, or unlock:

屏障指令适用于程序执行过程中出现的不同种类的访问之间。找到一个 "最佳"位置，使执行的障碍总数最小化，这几乎是不可能的。编译器通常无法判断一个给定的加载或存储之前或之后是否会有另一个需要屏障的指令；例如，当一个 volatile 存储之后是一个返回。最简单的保守策略是，在为任何给定的加载、存储、锁定或解锁生成代码时，假定需要 "最重 "类型的障碍的访问会发生。

1. Issue a StoreStore barrier before each volatile store. (On ia64 you must instead fold this and most barriers into corresponding load or store instructions.)

在每个易失性存储之前，发出一个 StoreStore 屏障。(在ia64上，你必须把这个和大多数障碍折叠到相应的加载或存储指令中)。

2. Issue a StoreStore barrier after all stores but before return from any constructor for any class with a final field.

在所有存储之后，但在任何具有 final 字段的类的任何构造函数返回之前，发出 StoreStore 屏障。

3. Issue a StoreLoad barrier after each volatile store. Note that you could instead issue one before each volatile load, but this would be slower for typical programs using volatiles in which reads greatly outnumber writes. Alternatively, if available, you can implement volatile store as an atomic instruction (for example XCHG on x86) and omit the barrier. This may be more efficient if atomic instructions are cheaper than StoreLoad barriers.

在每个 volatile 存储之后发出一个 StoreLoad 屏障。请注意，你可以在每次 volatile 加载之前发出一个，但是对于使用 volatile 的典型程序来说，这将会比较慢，因为在这些程序中，读的次数大大超过写的次数。另外，如果可以的话，你可以把 volatile 存储作为一个原子指令来实现（例如X86上的XCHG），并省略障碍。如果原子指令比 StoreLoad 屏障更便宜的话，这可能会更有效。

4. Issue LoadLoad and LoadStore barriers after each volatile load. On processors that preserve data dependent ordering, you need not issue a barrier if the next access instruction is dependent on the value of the load. In particular, you do not need a barrier after a load of a volatile reference if the subsequent instruction is a null-check or load of a field of that reference.

在每次 volatile 加载后都要发布 LoadLoad 和 LoadStore 障碍。在保留数据依赖性排序的处理器上，如果下一条访问指令依赖于加载的值，你就不需要发出屏障。特别是，如果随后的指令是一个空检查或加载该引用的一个字段，那么在加载一个易失性引用后就不需要设置障碍。

5. Issue an ExitEnter barrier either before each MonitorEnter or after each MonitorExit. (As discussed above, ExitEnter is a no-op if either MonitorExit or MonitorEnter uses an atomic instruction that supplies the equivalent of a StoreLoad barrier. Similarly for others involving Enter and Exit in the remaining steps.)

在每个 MonitorEnter 之前或 MonitorExit 之后发出一个 ExitEnter 障碍。(如上所述，如果MonitorExit 或 MonitorEnter 使用一个原子指令，提供相当于 StoreLoad 屏障的指令，ExitEnter 是一个无用的指令。在其余的步骤中，涉及 Enter 和 Exit 的其他指令也是如此）。

6. Issue EnterLoad and EnterStore barriers after each MonitorEnter.
7. Issue StoreExit and LoadExit barriers before each MonitorExit.
8. If on a processor that does not intrinsically provide ordering on indirect loads, issue a LoadLoad barrier before each load of a final field. (Some alternative strategies are discussed in this JMM list posting, and this description of linux data dependent barriers.)

如果在一个不提供间接负载排序的处理器上，在每次加载最后一个字段之前发出一个 LoadLoad 障碍。(一些替代的策略在这个 JMM 列表中讨论过，还有这个关于 linux 数据依赖障碍的描述)。

Many of these barriers usually reduce to no-ops. In fact, most of them reduce to no-ops, but in different ways under different processors and locking schemes. For the simplest examples, basic conformance to JSR-133 on x86 or sparc-TSO using CAS for locking amounts only to placing a StoreLoad barrier after volatile stores.

许多这样的障碍通常会减少到 no-ops。事实上，它们中的大多数都减少为无操作，但在不同的处理器和锁定方案下有不同的方式。对于最简单的例子，在 x86 或 sparc-TSO上使用 CAS 进行锁定，基本符合 JSR-133 的规定，只需要在 volatile 存储后放置一个 StoreLoad 障碍。

#### Remove Barriers
The conservative strategy above is likely to perform acceptably for many programs. The main performance issues surrounding volatiles occur for the StoreLoad barriers associated with stores. These ought to be relatively rare -- the main reason for using volatiles in concurrent programs is to avoid the need to use locks around reads, which is only an issue when reads greatly overwhelm writes. But this strategy can be improved in at least the following ways:

上述保守的策略对许多程序来说可能表现得可以接受。围绕 volatiles 的主要性能问题发生在与存储相关的StoreLoad 障碍上。这应该是比较少见的--在并发程序中使用 volatiles 的主要原因是为了避免在读取时使用锁，这只有在读取大大超过写入时才是一个问题。但是这个策略至少可以通过以下方式进行改进。

+ Removing redundant barriers. Similar eliminations can be used for interactions with locks, but depend on how locks are implemented. Doing all this in the presence of loops, calls, and branches is left as an exercise for the reader. :-)
+ Rearranging code (within the allowed constraints) to further enable removing LoadLoad and LoadStore barriers that are not needed because of data dependencies on processors that preserve such orderings. (类似的消除方法也可用于与锁的交互，但取决于锁的实现方式。在有循环、调用和分支的情况下做这一切，是留给读者的一个练习。)
+ Moving the point in the instruction stream that the barriers are issued, to improve scheduling, so long as they still occur somewhere in the interval they are required. (移动指令流中发出障碍物的时间点，以改善调度，只要它们仍然发生在它们所需的间隔的某个地方。)
+ Removing barriers that aren't needed because there is no possibility that multiple threads could rely on them; for example volatiles that are provably visible only from a single thread. Also, removing some barriers when it can be proven that threads can only store or only load certain fields. All this usually requires a fair amount of analysis. (删除那些不需要的障碍，因为不可能有多个线程依赖它们；例如，可以证明只有一个线程可见的挥发物。另外，当可以证明线程只能存储或只能加载某些字段时，也要删除一些障碍。所有这些通常都需要进行相当多的分析。)

#### Miscellany
JSR-133 also addresses a few other issues that may entail barriers in more specialized cases:

JSR-133 还解决了一些其他问题，这些问题在更专门的情况下可能会带来障碍。

+ Thread.start() requires barriers ensuring that the started thread sees all stores visible to the caller at the call point. Conversely, Thread.join() requires barriers ensuring that the caller sees all stores by the terminating thread. These are normally generated by the synchronization entailed in implementations of these constructs. (Thread.start()需要确保被启动的线程在调用点看到调用者可见的所有存储。反之，Thread.join()需要确保调用者看到终止线程的所有存储。这些通常是由这些构造的实现中所涉及的同步产生的。
)
+ Static final initialization requires StoreStore barriers that are normally entailed in mechanics needed to obey Java class loading and initialization rules. (静态的最终初始化需要 StoreStore 障碍，这些障碍通常包含在遵守 Java 类加载和初始化规则所需的机制中。)
+ Ensuring default zero/null initial field values normally entails barriers, synchronization, and/or low-level cache control within garbage collectors. (确保默认的零/空初始字段值通常需要障碍、同步和/或垃圾收集器内的低级缓存控制。)
+ JVM-private routines that "magically" set System.in, System.out, and System.err outside of constructors or static initializers need special attention since they are special legacy exceptions to JMM rules for final fields. (在构造函数或静态初始化器之外 "神奇地 "设置System.in、System.out和System.err的JVM私有例程需要特别注意，因为它们是JMM规则的最终字段的特殊遗留例外。)
+ Similarly, internal JVM deserialization code that sets final fields normally requires a StoreStore barrier. (同样地，设置 final 字段的JVM内部反序列化代码通常需要一个StoreStore屏障。)
+ Finalization support may require barriers (within garbage collectors) to ensure that Object.finalize code sees all stores to all fields prior to the objects becoming unreferenced. This is usually ensured via the synchronization used to add and remove references in reference queues. (最终化支持可能需要屏障（在垃圾收集器内），以确保Object.finalize代码在对象变成未引用前看到所有字段的所有存储。这通常是通过用于在引用队列中添加和删除引用的同步来确保的。)
+ Calls to and returns from JNI routines may require barriers, although this seems to be a quality of implementation issue. (对JNI例程的调用和返回可能需要障碍，尽管这似乎是一个实施质量问题。)
+ Most processors have other synchronizing instructions designed primarily for use with IO and OS actions. These don't impact JMM issues directly, but may be involved in IO, class loading, and dynamic code generation. (大多数处理器都有其他同步指令，主要设计用于IO和操作系统的操作。这些并不直接影响JMM问题，但可能涉及到IO、类加载和动态代码生成。)