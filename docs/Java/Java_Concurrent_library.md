# Java 并发库相关数据结构

[TOC]

## ConcurrentLinkedQueue
An unbounded thread-safe queue based on linked nodes. This queue orders elements FIFO (first-in-first-out). The head of the queue is that element that has been on the queue the longest time. The tail of the queue is that element that has been on the queue the shortest time. New elements are inserted at the tail of the queue, and the queue retrieval operations obtain elements at the head of the queue. A ConcurrentLinkedQueue is an appropriate choice when many threads will share access to a common collection. Like most other concurrent collection implementations, this class does not permit the use of null elements.

一个基于链接节点的无界线程安全队列。这个队列以先进先出（FIFO）方式排列元素。队列的头是在队列中时间最长的元素。队列的尾部是在队列中时间最短的元素。新元素在队尾插入，队列检索操作在队首获得元素。当许多线程将共享对一个共同集合的访问时，ConcurrentLinkedQueue 是一个合适的选择。像大多数其他并发集合的实现一样，这个类不允许使用空元素。

This implementation employs an efficient non-blocking algorithm based on one described in Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue Algorithms  by Maged M. Michael and Michael L. Scott.

这个实现采用了一种高效的非阻塞算法，该算法基于 Maged M. Michael 和 Michael L. Scott 在《Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue Algorithms》中描述的算法。

Iterators are weakly consistent, returning elements reflecting the state of the queue at some point at or since the creation of the iterator. They do not throw java.util.ConcurrentModificationException, and may proceed concurrently with other operations. Elements contained in the queue since the creation of the iterator will be returned exactly once.

迭代器是弱一致性的，返回的元素反映了队列在创建迭代器时或创建后的某个点的状态。它们不会抛出 java.util.ConcurrentModificationException，并且可以与其他操作同时进行。自迭代器创建以来包含在队列中的元素将被准确地返回一次。

Beware that, unlike in most collections, the size method is NOT a constant-time operation. Because of the asynchronous nature of these queues, determining the current number of elements requires a traversal of the elements, and so may report inaccurate results if this collection is modified during traversal. Additionally, the bulk operations addAll, removeAll, retainAll, containsAll, equals, and toArray are not guaranteed to be performed atomically. For example, an iterator operating concurrently with an addAll operation might view only some of the added elements.

请注意，与大多数集合不同，大小方法不是一个恒定时间操作。由于这些队列的异步性，确定当前元素的数量需要对元素进行遍历，因此如果在遍历过程中修改了这个集合，可能会报告不准确的结果。此外，批量操作addAll、removeAll、retainAll、containsAll、equals 和 toArray 都不能保证以原子方式执行。例如，一个与 addAll 操作同时进行的迭代器可能只查看一些添加的元素。

This class and its iterator implement all of the optional methods of the Queue and Iterator interfaces.

这个类和它的迭代器实现了队列和迭代器接口的所有可选方法。

Memory consistency effects: As with other concurrent collections, actions in a thread prior to placing an object into a ConcurrentLinkedQueue happen-before actions subsequent to the access or removal of that element from the ConcurrentLinkedQueue in another thread.

内存一致性效应。与其他并发集合一样，在一个线程中把一个对象放入 ConcurrentLinkedQueue 之前的操作，会发生在另一个线程中访问或从 ConcurrentLinkedQueue 中移除该元素的后续操作之前。


### Overview
This is a modification of the Michael & Scott algorithm, adapted for a garbage-collected environment, with support for interior node deletion (to support remove(Object)).  For explanation, read the paper.

这是对 Michael & Scott 算法的修改，适用于垃圾收集的环境，支持内部节点删除（支持 remove(Object)）。 如需解释，请阅读该论文。

Note that like most non-blocking algorithms in this package, this implementation relies on the fact that in garbage collected systems, there is no possibility of ABA problems due to recycled nodes, so there is no need to use "counted pointers" or related techniques seen in versions used in non-GC'ed settings.

请注意，像这个包中的大多数非阻塞算法一样，这个实现依赖于这样一个事实，即在垃圾收集系统中，不可能出现由于回收节点而导致的 ABA 问题，所以不需要使用 "计数指针"或在非 GC ed 环境下使用的版本中看到的相关技术。

The fundamental invariants are: 
- There is exactly one (last) Node with a null next reference, which is CASed when enqueueing. This last Node can be reached in O(1) time from tail, but tail is merely an optimization - it can always be reached in O(N) time from head as well.
- The elements contained in the queue are the non-null items in Nodes that are reachable from head.  CASing the item reference of a Node to null atomically removes it from the queue.  Reachability of all elements from head must remain true even in the case of concurrent modifications that cause head to advance.  A dequeued Node may remain in use indefinitely due to creation of an Iterator or simply a poll() that has lost its time slice.

基本的不变量是。
- 有一个（最后一个）节点的下一个引用为空，在入队时被 CAS。这个最后的节点可以在 O(1) 时间内从尾部到达，但尾部只是一个优化 - 它总是可以在 O(N) 时间内从头部到达。
- 队列中包含的元素是 Node 中可以从 head 到达的非空项。 将一个 Node 的项目引用 CAS 为 null 时，会将其从队列中移除。 即使在并发修改导致头部前进的情况下，所有元素从头部的可及性必须保持真实。 由于创建了一个迭代器或者仅仅是一个失去了时间片的 poll()，一个出队的 Node 可能会被无限期地使用。

The above might appear to imply that all Nodes are GC-reachable from a predecessor dequeued Node.  That would cause two problems: 
- allow a rogue Iterator to cause unbounded memory retention
- cause cross-generational linking of old Nodes to new Nodes if a Node was tenured while live, which generational GCs have a hard time dealing with, causing repeated major collections. 

以上可能意味着所有的节点都可以从先前已出队的节点中获得 GC 可达性。 这将导致两个问题。
- 允许一个流氓迭代器导致无限制的内存保留
- 如果一个节点在存活期间被租用，就会导致旧节点与新节点的跨代链接，而跨代 GC 很难处理这种情况，从而导致重复的大集合。

However, only non-deleted Nodes need to be reachable from dequeued Nodes, and reachability does not necessarily have to be of the kind understood by the GC.  We use the trick of linking a Node that has just been dequeued to itself.  Such a self-link implicitly means to advance to head.

然而，只有非删除的节点才需要可以从去排队的节点中到达，而且到达性不一定是 GC 所理解的那种。 我们使用的技巧是将一个刚刚被删除的节点链接到它自己。 这样的自我链接隐含着前进到头部的意思。

Both head and tail are permitted to lag.  In fact, failing to update them every time one could is a significant optimization (fewer CASes). As with LinkedTransferQueue (see the internal documentation for that class), we use a slack threshold of two; that is, we update head/tail when the current pointer appears to be two or more steps away from the first/last node.

头和尾都允许滞后。 事实上，每次都不更新它们是一个重要的优化（减少 CAS）。和LinkedTransferQueue 一样（见该类的内部文档），我们使用了一个 2 的 slack 阈值；也就是说，当当前指针看起来离第一个/最后一个节点有两步或更多的距离时，我们会更新头部/尾部。

Since head and tail are updated concurrently and independently, it is possible for tail to lag behind head (why not)?

由于头部和尾部是同时独立更新的，所以尾部有可能落后于头部（为什么不可能）？

CASing a Node's item reference to null atomically removes the element from the queue.  Iterators skip over Nodes with null items.  Prior implementations of this class had a race between poll() and remove(Object) where the same element would appear to be successfully removed by two concurrent operations.  The method remove(Object) also lazily unlinks deleted Nodes, but this is merely an optimization.

将 CAS 一个 Node 的 item 引用为 null 时，会将该元素从队列中移除。 迭代器跳过 item 为空的 Node。 这个类之前的实现在 poll() 和 remove(Object) 之间存在竞争，同一个元素会被两个并发的操作成功删除。 方法 remove(Object) 也会懒惰地解除被删除的 Nodes 的链接，但这仅仅是一种优化。

When constructing a Node (before enqueuing it) we avoid paying for a volatile write to item by using Unsafe.putObject instead of a normal write.  This allows the cost of enqueue to be "one-and-a-half" CASes.

当构造一个 Node 时（在排队之前），我们通过使用 Unsafe.putObject 而不是普通的写来避免支付对 item 的 volatile 写。 这使得 enqueue 的成本为 "一个半" CAS 操作。

Both head and tail may or may not point to a Node with a non-null item.  If the queue is empty, all items must of course be null.  Upon creation, both head and tail refer to a dummy Node with null item.  Both head and tail are only updated using CAS, so they never regress, although again this is merely an optimization.

头和尾都可能指向一个具有非空项的Node，也可能不指向。 如果队列是空的，所有项目当然必须是空的。 创建时，头部和尾部都指向一个具有空项的虚拟节点。 head 和 tail 都只使用 CAS 进行更新，所以它们永远不会回退，尽管这只是一种优化。

## ConcurrentHashMap
A hash table supporting full concurrency of retrievals and high expected concurrency for updates. This class obeys the same functional specification as Hashtable, and includes versions of methods corresponding to each method of Hashtable. However, even though all operations are thread-safe, retrieval operations do not entail locking, and there is not any support for locking the entire table in a way that prevents all access. This class is fully interoperable with Hashtable in programs that rely on its thread safety but not on its synchronization details.

一个支持全并发检索和高预期并发更新的哈希表。这个类遵守与 Hashtable 相同的功能规范，并且包括与 Hashtable 的每个方法相对应的方法版本。然而，尽管所有操作都是线程安全的，但检索操作不需要锁定，也不支持以阻止所有访问的方式锁定整个表。在依赖其线程安全但不依赖其同步细节的程序中，该类与 Hashtable 是完全可以互通的。

Retrieval operations (including get) generally do not block, so may overlap with update operations (including put and remove). Retrievals reflect the results of the most recently completed update operations holding upon their onset. (More formally, an update operation for a given key bears a happens-before relation with any (non-null) retrieval for that key reporting the updated value.) For aggregate operations such as putAll and clear, concurrent retrievals may reflect insertion or removal of only some entries. Similarly, Iterators, Spliterators and Enumerations return elements reflecting the state of the hash table at some point at or since the creation of the iterator/enumeration. They do not throw ConcurrentModificationException. However, iterators are designed to be used by only one thread at a time. Bear in mind that the results of aggregate status methods including size, isEmpty, and containsValue are typically useful only when a map is not undergoing concurrent updates in other threads. Otherwise the results of these methods reflect transient states that may be adequate for monitoring or estimation purposes, but not for program control.

检索操作（包括获取）一般不会阻塞，所以可能与更新操作（包括投放和删除）重叠。检索反映了最近完成的更新操作的结果，并在其开始时保持。(更正式地说，一个给定的键的更新操作与该键的任何（非空的）检索有一个发生之前的关系，报告更新的值）。对于像 putAll 和 clear 这样的聚合操作，并发的检索可以反映出只插入或删除一些条目。同样，Iterators、Spliterators 和 Enumerations 返回反映哈希表在迭代器/枚举创建时或创建后某一时刻的状态的元素。它们不会抛出 ConcurrentModificationException。然而，迭代器被设计为一次只能由一个线程使用。请记住，包括 size、isEmpty 和 containsValue 在内的聚合状态方法的结果通常只在 map 没有在其他线程中进行并发更新时有用。否则，这些方法的结果反映的是瞬时状态，可能足以用于监控或估计的目的，但不能用于程序控制。

The table is dynamically expanded when there are too many collisions (i.e., keys that have distinct hash codes but fall into the same slot modulo the table size), with the expected average effect of maintaining roughly two bins per mapping (corresponding to a 0.75 load factor threshold for resizing). There may be much variance around this average as mappings are added and removed, but overall, this maintains a commonly accepted time/space tradeoff for hash tables. However, resizing this or any other kind of hash table may be a relatively slow operation. When possible, it is a good idea to provide a size estimate as an optional initialCapacity constructor argument. An additional optional loadFactor constructor argument provides a further means of customizing initial table capacity by specifying the table density to be used in calculating the amount of space to allocate for the given number of elements. Also, for compatibility with previous versions of this class, constructors may optionally specify an expected concurrencyLevel as an additional hint for internal sizing. Note that using many keys with exactly the same hashCode() is a sure way to slow down performance of any hash table. To ameliorate impact, when keys are Comparable, this class may use comparison order among keys to help break ties.

当有太多的碰撞（即键有不同的哈希代码，但落入同一槽的表大小）时，表被动态扩展，预期的平均效果是每个映射保持大约两个仓（对应于调整大小的 0.75 负载因子阈值）。随着映射的添加和删除，这个平均值可能会有很大的变化，但总的来说，这保持了一个普遍接受的哈希表的时间/空间权衡。然而，调整这个或任何其他类型的哈希表的大小可能是一个相对缓慢的操作。在可能的情况下，提供一个大小估计作为一个可选的 initialCapacity 构造参数是一个好主意。一个额外的可选 loadFactor 构造参数提供了进一步定制初始表容量的方法，它指定了在计算为给定的元素数量分配的空间时要使用的表密度。另外，为了与该类以前的版本兼容，构造函数可以选择指定一个预期的并发级别（concurrencyLevel）作为内部大小的额外提示。请注意，使用许多具有完全相同的 hashCode()的键，肯定会降低任何哈希表的性能。为了减轻影响，当键是可比较的时候，这个类可以使用键之间的比较顺序来帮助打破联系。

A Set projection of a ConcurrentHashMap may be created (using newKeySet() or newKeySet(int)), or viewed (using keySet(Object) when only keys are of interest, and the mapped values are (perhaps transiently) not used or all take the same mapping value.

ConcurrentHashMap 的 Set 投影可以被创建（使用 newKeySet() 或 newKeySet(int)），或者被查看（使用 keySet(Object)），当只有键感兴趣，而映射的值（也许是短暂的）不被使用，或者都采取相同的映射值。

A ConcurrentHashMap can be used as scalable frequency map (a form of histogram or multiset) by using java.util.concurrent.atomic.LongAdder values and initializing via computeIfAbsent. For example, to add a count to a ConcurrentHashMap<String,LongAdder> freqs, you can use freqs.computeIfAbsent(k -> new LongAdder()).increment();

通过使用 java.util.concurrent.atomic.LongAdder 值并通过 computeIfAbsent 初始化，ConcurrentHashMap 可以作为可扩展的频率图（直方图或多数据集的一种形式）。例如，为了给 ConcurrentHashMap<String,LongAdder> freqs 增加一个计数，你可以使用 freqs.computeIfAbsent(k -> new LongAdder()).increment()。

This class and its views and iterators implement all of the optional methods of the Map and Iterator interfaces.

这个类和它的视图和迭代器实现了Map和Iterator接口的所有可选方法。

Like Hashtable but unlike HashMap, this class does not allow null to be used as a key or value.

与 Hashtable 一样，但与 HashMap 不同，该类不允许将 null 作为键或值使用。

ConcurrentHashMaps support a set of sequential and parallel bulk operations that, unlike most Stream methods, are designed to be safely, and often sensibly, applied even with maps that are being concurrently updated by other threads; for example, when computing a snapshot summary of the values in a shared registry. There are three kinds of operation, each with four forms, accepting functions with Keys, Values, Entries, and (Key, Value) arguments and/or return values. Because the elements of a ConcurrentHashMap are not ordered in any particular way, and may be processed in different orders in different parallel executions, the correctness of supplied functions should not depend on any ordering, or on any other objects or values that may transiently change while computation is in progress; and except for forEach actions, should ideally be side-effect-free. Bulk operations on Map.Entry objects do not support method setValue.

ConcurrentHashMaps 支持一组顺序和并行的批量操作，与大多数 Stream 方法不同的是，这些操作被设计成可以安全地，而且通常是合理地应用于正在被其他线程并发更新的地图；例如，在计算共享注册表中的值的快照摘要时。有三种操作，每种都有四种形式，接受带有键、值、条目和（键、值）参数和/或返回值的函数。因为 ConcurrentHashMap 的元素没有以任何特定的方式排序，并且可能在不同的并行执行中以不同的顺序处理，所以提供的函数的正确性不应该依赖于任何排序，或者依赖于任何其他对象或计算过程中可能瞬时改变的值；除了 forEach 操作，最好是没有副作用。对 Map.Entry 对象的批量操作不支持 setValue 方法。

forEach: Perform a given action on each element. A variant form applies a given transformation on each element before performing the action.
search: Return the first available non-null result of applying a given function on each element; skipping further search when a result is found.

forEach。对每个元素执行一个给定的动作。一个变体形式在执行动作前对每个元素应用一个给定的转换。

reduce: Accumulate each element. The supplied reduction function cannot rely on ordering (more formally, it should be both associative and commutative). There are five variants:
+ Plain reductions. (There is not a form of this method for (key, value) function arguments since there is no corresponding return type.)
+ Mapped reductions that accumulate the results of a given function applied to each element.
+ Reductions to scalar doubles, longs, and ints, using a given basis value.

减少。累积每个元素。提供的还原函数不能依赖排序（更正式地说，它应该既是关联的又是换元的）。有五种变体。
+ 纯粹的还原。(对于(key, value)函数参数没有此方法的形式，因为没有相应的返回类型)。
+ 映射式还原，累积应用于每个元素的特定函数的结果。
+ 使用一个给定的基础值，对标量的双数、长数和英寸进行还原。

These bulk operations accept a parallelismThreshold argument. Methods proceed sequentially if the current map size is estimated to be less than the given threshold. Using a value of Long.MAX_VALUE suppresses all parallelism. Using a value of 1 results in maximal parallelism by partitioning into enough subtasks to fully utilize the ForkJoinPool.commonPool() that is used for all parallel computations. Normally, you would initially choose one of these extreme values, and then measure performance of using in-between values that trade off overhead versus throughput.

这些批量操作接受一个 parallelismThreshold 参数。如果估计当前 map 的大小小于给定的阈值，方法将顺序进行。使用 Long.MAX_VALUE 的值可以抑制所有的并行性。使用 1 的值会产生最大的并行性，通过分割成足够多的子任务来充分利用用于所有并行计算的 ForkJoinPool.commonPool()。通常情况下，你最初会选择这些极端值中的一个，然后测量使用介于两者之间的值来权衡开销和吞吐量的性能。

The concurrency properties of bulk operations follow from those of ConcurrentHashMap: Any non-null result returned from get(key) and related access methods bears a happens-before relation with the associated insertion or update. The result of any bulk operation reflects the composition of these per-element relations (but is not necessarily atomic with respect to the map as a whole unless it is somehow known to be quiescent). Conversely, because keys and values in the map are never null, null serves as a reliable atomic indicator of the current lack of any result. To maintain this property, null serves as an implicit basis for all non-scalar reduction operations. For the double, long, and int versions, the basis should be one that, when combined with any other value, returns that other value (more formally, it should be the identity element for the reduction). Most common reductions have these properties; for example, computing a sum with basis 0 or a minimum with basis MAX_VALUE.

批量操作的并发属性与 ConcurrentHashMap 的属性相同。从 get(key) 和相关访问方法返回的任何非空的结果都与相关的插入或更新有一个 happens-before 的关系。任何批量操作的结果都反映了这些每个元素关系的组成（但是对于整个 map 来说不一定是原子的，除非它在某种程度上被知道是静止的）。反过来说，由于 map 中的键和值永远不会是空的，空作为一个可靠的原子指标，表明当前没有任何结果。为了保持这个属性，null 作为所有非标量还原操作的隐含基础。对于 double、long 和 int 版本，基础应该是一个当与任何其他值结合时，返回其他值的元素（更正式地说，它应该是还原的身份元素）。大多数常见的还原操作都有这些属性；例如，用基数 0 计算和，或用基数 MAX_VALUE 计算最小值。

Search and transformation functions provided as arguments should similarly return null to indicate the lack of any result (in which case it is not used). In the case of mapped reductions, this also enables transformations to serve as filters, returning null (or, in the case of primitive specializations, the identity basis) if the element should not be combined. You can create compound transformations and filterings by composing them yourself under this "null means there is nothing there now" rule before using them in search or reduce operations.
Methods accepting and/or returning Entry arguments maintain key-value associations. They may be useful for example when finding the key for the greatest value. Note that "plain" Entry arguments can be supplied using new AbstractMap.SimpleEntry(k,v).
Bulk operations may complete abruptly, throwing an exception encountered in the application of a supplied function. Bear in mind when handling such exceptions that other concurrently executing functions could also have thrown exceptions, or would have done so if the first exception had not occurred.

作为参数提供的搜索和转换函数同样应该返回 null，以表示没有任何结果（在这种情况下，它不会被使用）。在映射还原的情况下，这也使得变换能够作为过滤器，如果元素不应该被组合，则返回 null（或者，在原始特化的情况下，返回身份基础）。你可以在搜索或还原操作中使用复合变换和过滤之前，根据这个"null 表示现在没有任何东西" 的规则，自己组合它们来创建复合变换和过滤。

Speedups for parallel compared to sequential forms are common but not guaranteed. Parallel operations involving brief functions on small maps may execute more slowly than sequential forms if the underlying work to parallelize the computation is more expensive than the computation itself. Similarly, parallelization may not lead to much actual parallelism if all processors are busy performing unrelated tasks.

与顺序形式相比，并行形式的加速很常见，但不能保证。如果将计算并行化的基础工作比计算本身更昂贵，那么涉及小 map 上简短函数的并行操作可能比顺序形式执行得更慢。同样，如果所有的处理器都在忙着执行不相关的任务，那么并行化可能不会导致很多实际的并行。

### Overview
The primary design goal of this hash table is to maintain concurrent readability (typically method get(), but also iterators and related methods) while minimizing update contention. Secondary goals are to keep space consumption about the same or better than java.util.HashMap, and to support high initial insertion rates on an empty table by many threads.

这个哈希表的主要设计目标是保持并发的可读性（通常是方法 get()，但也包括迭代器和相关方法），同时最大限度地减少更新时的争用。次要目标是保持空间消耗与 java.util.HashMap 差不多或更好，并支持许多线程对空表的高初始插入率。

This map usually acts as a binned (bucketed) hash table. Each key-value mapping is held in a Node. Most nodes are instances of the basic Node class with hash, key, value, and next fields. However, various subclasses exist: TreeNodes are arranged in balanced trees, not lists.  TreeBins hold the roots of sets of TreeNodes. ForwardingNodes are placed at the heads of bins during resizing. ReservationNodes are used as placeholders while establishing values in computeIfAbsent and related methods.  The types TreeBin, ForwardingNode, and ReservationNode do not hold normal user keys, values, or hashes, and are readily distinguishable during search etc because they have negative hash fields and null key and value fields. (These special nodes are either uncommon or transient, so the impact of carrying around some unused fields is insignificant.)

这个映射通常作为一个分档（bucketed）的哈希表。每个键值映射被保存在一个 Node 中。大多数节点是基本的 Node 类的实例，有 hash、key、value 和 next 字段。然而，存在各种子类。TreeNodes 被排列成平衡的树，而不是列表。 TreeBins 保存着 TreeNodes 集合的根部。ForwardingNodes 在调整大小时被放置在 bins 的头部。当在 computeIfAbsent和相关方法中建立值时，ReservationNodes 被用作占位符。TreeBin、ForwardingNode 和 ReservationNode 这些类型并不持有普通的用户键、值或哈希值，并且在搜索过程中很容易被区分开来，因为它们有负的哈希字段和空的键和值字段。(这些特殊的节点要么不常见，要么是短暂的，所以携带一些未使用的字段的影响是不明显的)。

The table is lazily initialized to a power-of-two size upon the first insertion.  Each bin in the table normally contains a list of Nodes (most often, the list has only zero or one Node). Table accesses require volatile/atomic reads, writes, and CASes. Because there is no other way to arrange this without adding further indirections, we use intrinsics (sun.misc.Unsafe) operations.

该表在第一次插入时被懒散地初始化为 2 的幂级大小。 表中的每个 bin 通常包含一个节点列表（最常见的是，该列表只有零或一个节点）。表的访问需要 volatile/原子性的读、写和 CASes。因为没有其他方法可以在不增加间接性的情况下进行安排，所以我们使用本征（sun.misc.unsafe）操作。

We use the top (sign) bit of Node hash fields for control purposes -- it is available anyway because of addressing constraints.  Nodes with negative hash fields are specially handled or ignored in map methods. Insertion (via put or its variants) of the first node in an empty bin is performed by just CASing it to the bin.  This is by far the most common case for put operations under most key/hash distributions.  Other update operations (insert, delete, and replace) require locks.  We do not want to waste the space required to associate a distinct lock object with each bin, so instead use the first node of a bin list itself as a lock. Locking support for these locks relies on builtin "synchronized" monitors.

我们使用节点哈希字段的顶部（符号）位来控制 -- 由于寻址限制，它无论如何都是可用的。 具有负哈希字段的节点在映射方法中被特别处理或忽略。在一个空的 bin 中插入（通过 put 或其变体）第一个节点时，只需将其 CAS 到 bin 中。 这是迄今为止在大多数键/哈希分布下最常见的插入操作的情况。其他更新操作（插入、删除和替换）需要锁。 我们不想浪费空间将一个不同的锁对象与每个 bin 联系起来，所以使用 bin 列表的第一个节点本身作为一个锁。对这些锁的支持依赖于内置的 "同步" 监视器。

Using the first node of a list as a lock does not by itself suffice though: When a node is locked, any update must first validate that it is still the first node after locking it, and retry if not. Because new nodes are always appended to lists, once a node is first in a bin, it remains first until deleted or the bin becomes invalidated (upon resizing).

但使用列表中的第一个节点作为锁本身并不足够。当一个节点被锁定时，任何更新必须首先验证它是否仍然是锁定后的第一个节点，如果不是，则重试。因为新的节点总是被追加到列表中，一旦一个节点在一个 bin 中是第一个，它就一直是第一个，直到被删除或 bin 变得无效（在调整大小时）。

The main disadvantage of per-bin locks is that other update operations on other nodes in a bin list protected by the same lock can stall, for example when user equals() or mapping functions take a long time.  However, statistically, under random hash codes, this is not a common problem.  Ideally, the frequency of nodes in bins follows a Poisson distribution (http://en.wikipedia.org/wiki/Poisson_distribution) with a parameter of about 0.5 on average, given the resizing threshold of 0.75, although with a large variance because of resizing granularity. Ignoring variance, the expected occurrences of list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The first values are:

每个 bin 锁的主要缺点是，在 bin 列表中受同一锁保护的其他节点上的其他更新操作可能会停滞，例如当用户 equals() 或映射函数需要很长的时间。 然而，据统计，在随机哈希代码下，这不是一个常见的问题。 理想情况下，考虑到 0.75 的调整阈值，bin 中的节点频率遵循泊松分布（http://en.wikipedia.org/wiki/Poisson_distribution），参数平均约为0.5，尽管由于调整颗粒度的原因，有很大的方差。忽略方差，列表大小 k 的预期发生率是（exp(-0.5) * pow(0.5, k) / factorial(k)）。第一个值是。

```
    0:    0.60653066
    1:    0.30326533
    2:    0.07581633
    3:    0.01263606
    4:    0.00157952
    5:    0.00015795
    6:    0.00001316
    7:    0.00000094
    8:    0.00000006
    more: less than 1 in ten million
```

Lock contention probability for two threads accessing distinct elements is roughly 1 / (8 * #elements) under random hashes.

在随机散列下，两个线程访问不同元素的锁争夺概率大约为 1/(8 * #elements)。

Actual hash code distributions encountered in practice sometimes deviate significantly from uniform randomness. This includes the case when N > (1<<30), so some keys MUST collide. Similarly for dumb or hostile usages in which multiple keys are designed to have identical hash codes or ones that differs only in masked-out high bits. So we use a secondary strategy that applies when the number of nodes in a bin exceeds a threshold. These TreeBins use a balanced tree to hold nodes (a specialized form of red-black trees), bounding search time to O(log N). Each search step in a TreeBin is at least twice as slow as in a regular list, but given that N cannot exceed (1<<64) (before running out of addresses) this bounds search steps, lock hold times, etc, to reasonable constants (roughly 100 nodes inspected per operation worst case) so long as keys are Comparable (which is very common -- String, Long, etc). TreeBin nodes (TreeNodes) also maintain the same "next" traversal pointers as regular nodes, so can be traversed in iterators in the same way.

在实践中遇到的实际哈希码分布有时会大大偏离均匀随机性。这包括 N > (1 << 30) 时的情况，所以有些钥匙一定会发生碰撞。同样，在哑巴或敌意的使用中，多个钥匙被设计成具有相同的哈希码，或者只在被屏蔽的高位上有差异。因此，我们使用一个次要的策略，当一个 bin 中的节点数量超过一个阈值时，就会适用。这些 TreeBins 使用平衡树来保存节点（红黑树的一种特殊形式），将搜索时间限制在 O(log N)。在 TreeBin 中的每个搜索步骤至少是普通列表的两倍，但是考虑到 N 不能超过（1 << 64）（在耗尽地址之前），只要键是可比较的（这是非常常见的--字符串、长等），这就将搜索步骤、锁保持时间等限定为合理的常数（最坏的情况是每次操作大约检查 100 个节点）。TreeBin 节点（TreeNodes）也保持着与普通节点相同的 "下一个 "遍历指针，因此可以用同样的方式在迭代器中遍历。

The table is resized when occupancy exceeds a percentage threshold (nominally, 0.75, but see below).  Any thread noticing an overfull bin may assist in resizing after the initiating thread allocates and sets up the replacement array. However, rather than stalling, these other threads may proceed with insertions etc.  The use of TreeBins shields us from the  worst case effects of overfilling while resizes are in  progress.  Resizing proceeds by transferring bins, one by one,  from the table to the next table. However, threads claim small blocks of indices to transfer (via field transferIndex) before doing so, reducing contention.  A generation stamp in field sizeCtl ensures that resizings do not overlap. Because we are using power-of-two expansion, the elements from each bin must either stay at same index, or move with a power of two offset. We eliminate unnecessary node creation by catching cases where old nodes can be reused because their next fields won't change.  On average, only about one-sixth of them need cloning when a table doubles. The nodes they replace will be garbage collectable as soon as they are no longer referenced by any reader thread that may be in the midst of concurrently traversing table.  Upon transfer, the old table bin contains only a special forwarding node (with hash field "MOVED") that contains the next table as its key. On encountering a forwarding node, access and update operations restart, using the new table.

当占用率超过一个百分比阈值（名义上是 0.75，但见下文），该表就会被调整大小。 在启动线程分配和设置替换数组后，任何注意到过满仓的线程都可以协助调整大小。然而，这些其他线程并没有停滞不前，而是可以继续进行插入等工作。 使用 TreeBins 可以使我们避免在调整大小的过程中出现过满的情况。 调整大小的过程是一个接一个地从表转移到下一个表。然而，线程在这样做之前会要求转移小块的索引（通过字段 transferIndex），以减少争论。字段 sizeCtl 中的生成戳记确保调整大小不会重叠。因为我们使用的是 2 次方扩展，每个 bin 的元素必须保持在相同的索引，或者以 2 次方的偏移量移动。我们通过捕捉旧节点可以被重复使用的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。平均来说，当一个表翻倍的时候，只有大约六分之一的节点需要克隆。只要它们不再被任何可能正在并发遍历表的读者线程所引用，它们所替换的节点就可以被垃圾回收。在转移时，旧表仓只包含一个特殊的转发节点（有哈希字段 "MOVED"），该节点包含下一个表作为其键。在遇到转发节点时，访问和更新操作重新开始，使用新表。

Each bin transfer requires its bin lock, which can stall waiting for locks while resizing. However, because other threads can join in and help resize rather than contend for locks, average aggregate waits become shorter as resizing progresses.  The transfer operation must also ensure that all accessible bins in both the old and new table are usable by any traversal.  This is arranged in part by proceeding from the last bin (table.length - 1) up towards the first.  Upon seeing a forwarding node, traversals (see class Traverser) arrange to move to the new table without revisiting nodes.  To ensure that no intervening nodes are skipped even when moved out of order, a stack (see class TableStack) is created on first encounter of a forwarding node during a traversal, to maintain its place if later processing the current table. The need for these save/restore mechanics is relatively rare, but when one forwarding node is encountered, typically many more will be. So Traversers use a simple caching scheme to avoid creating so many new TableStack nodes. (Thanks to Peter Levart for suggesting use of a stack here.)
 
每个仓的转移都需要它的仓锁，在调整大小的时候，会拖延等待锁的时间。然而，由于其他线程可以加入并帮助调整大小，而不是争夺锁，所以随着调整大小的进展，平均总等待时间会变短。 转移操作还必须确保新旧表中所有可访问的仓都能被任何遍历所使用。 这部分是通过从最后一个 bin（table.length - 1）开始向第一个 bin 前进来安排的。 当看到一个转发节点时，遍历者（见类 Traverser）会安排移动到新表，而不重新访问节点。 为了确保即使不按顺序移动也不会跳过中间的节点，在遍历过程中第一次遇到转发节点时，会创建一个栈（见类 TableStack），以便在以后处理当前表时保持其位置。对这些保存/恢复机制的需求是比较少的，但是当遇到一个转发节点时，通常会有很多转发节点。所以遍历者使用一个简单的缓存方案来避免创建这么多新的 TableStack 节点。(感谢 Peter Levart 建议在这里使用堆栈）。

The traversal scheme also applies to partial traversals of ranges of bins (via an alternate Traverser constructor) to support partitioned aggregate operations.  Also, read-only operations give up if ever forwarded to a null table, which provides support for shutdown-style clearing, which is also not currently implemented.

该遍历方案也适用于部分遍历范围的 bin（通过一个备用的 Traverser 构造函数），以支持分区聚合操作。 另外，如果曾经转发到一个空表，只读操作就会放弃，这为关闭式清空提供了支持，目前也没有实现。

Lazy table initialization minimizes footprint until first use, and also avoids resizings when the first operation is from a putAll, constructor with map argument, or deserialization. These cases attempt to override the initial capacity settings, but harmlessly fail to take effect in cases of races.

懒惰的表初始化在第一次使用前最大限度地减少了占用空间，同时也避免了在第一次操作来自 putAll、带有 map 参数的构造器或反序列化时的大小调整。这些情况试图覆盖初始容量设置，但在竞赛的情况下，无害地未能生效。

The element count is maintained using a specialization of LongAdder. We need to incorporate a specialization rather than just use a LongAdder in order to access implicit contention-sensing that leads to creation of multiple CounterCells.  The counter mechanics avoid contention on updates but can encounter cache thrashing if read too frequently during concurrent access. To avoid reading so often, resizing under contention is attempted only upon adding to a bin already holding two or more nodes. Under uniform hash distributions, the probability of this occurring at threshold is around 13%, meaning that only about 1 in 8 puts check threshold (and after resizing, many fewer do so).

使用 LongAdder 的一个特殊化来维护元素计数。我们需要加入一个特殊化，而不是仅仅使用一个 LongAdder，以便访问隐含的竞争感应，从而导致创建多个 CounterCells。 计数器机制避免了更新时的争用，但如果在并发访问时读取的频率过高，就会遇到高速缓存的困扰。为了避免如此频繁的读取，争用下的大小调整只在添加到一个已经容纳了两个或更多节点的仓时才会被尝试。在均匀的哈希分布下，在阈值处发生这种情况的概率约为 13%，这意味着只有约 1/8 的人检查阈值（在调整大小后，这样做的人更少）。

TreeBins use a special form of comparison for search and related operations (which is the main reason we cannot use existing collections such as TreeMaps). TreeBins contain Comparable elements, but may contain others, as well as elements that are Comparable but not necessarily Comparable for the same T, so we cannot invoke compareTo among them. To handle this, the tree is ordered primarily by hash value, then by Comparable.compareTo order if applicable.  On lookup at a node, if elements are not comparable or compare as 0 then both left and right children may need to be searched in the case of tied hash values. (This corresponds to the full list search that would be necessary if all elements were non-Comparable and had tied hashes.) On insertion, to keep a total ordering (or as close as is required here) across rebalancings, we compare classes and identityHashCodes as tie-breakers. The red-black balancing code is updated from pre-jdk-collections (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java) based in turn on Cormen, Leiserson, and Rivest "Introduction to Algorithms" (CLR).

TreeBins 使用一种特殊形式的比较来进行搜索和相关操作（这也是我们不能使用现有集合如 TreeMaps 的主要原因）。TreeBins 包含可比较的元素，但也可能包含其他元素，以及可比较的元素，但不一定是同一T的可比较元素，所以我们不能在它们之间调用 compareTo。为了处理这个问题，树主要是按哈希值排序，如果适用的话，再按 Comparable.compareTo 排序。 在查找一个节点时，如果元素不具有可比性或比较为 0，那么在哈希值相同的情况下，可能需要搜索左右两个子节点。(这相当于如果所有的元素都是不可比较的，并且有相同的哈希值，那么就需要进行全列表搜索)。在插入的时候，为了保持整个排序（或尽可能接近这里的要求），我们比较类和 identityHashCodes 作为平局的打破者。红黑平衡代码是从 pre-jdk-collections (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java) 更新而来的，反过来基于Cormen, Leiserson, and Rivest "Introduction to Algorithms" (CLR)。

TreeBins also require an additional locking mechanism.  While list traversal is always possible by readers even during updates, tree traversal is not, mainly because of tree-rotations that may change the root node and/or its linkages.  TreeBins include a simple read-write lock mechanism parasitic on the main bin-synchronization strategy: Structural adjustments associated with an insertion or removal are already bin-locked (and so cannot conflict with other writers) but must wait for ongoing readers to finish. Since there can be only one such waiter, we use a simple scheme using a single "waiter" field to block writers.  However, readers need never block.  If the root lock is held, they proceed along the slow traversal path (via next-pointers) until the lock becomes available or the list is exhausted, whichever comes first. These cases are not fast, but maximize aggregate expected throughput.

TreeBins 还需要一个额外的锁定机制。 即使在更新过程中，读者也可以对列表进行遍历，而树形遍历则不然，主要是因为树形旋转可能会改变根节点和/或其链接。 TreeBins 包括一个简单的读写锁机制，寄生在主要的 bin 同步策略上。与插入或移除相关的结构调整已经被锁定（因此不能与其他写入者冲突），但必须等待正在进行的读者完成。由于只能有一个这样的等待者，我们使用一个简单的方案，用一个 "等待者" 字段来阻止写入者。 然而，读者永远不需要阻塞。 如果根锁被持有，他们会沿着缓慢的遍历路径（通过下一个指针）前进，直到锁变得可用或者列表被用完，以先到者为准。这些情况并不快，但可以最大限度地提高总的预期吞吐量。

Maintaining API and serialization compatibility with previous versions of this class introduces several oddities. Mainly: We leave untouched but unused constructor arguments refering to concurrencyLevel. We accept a loadFactor constructor argument, but apply it only to initial table capacity (which is the only time that we can guarantee to honor it.) We also declare an unused "Segment" class that is instantiated in minimal form only when serializing.

保持与该类以前版本的 API 和序列化的兼容性引入了几个奇怪的现象。主要是。我们留下了未触及但未使用的构造函数参数，引用了 concurrencyLevel。我们接受一个 loadFactor 构造参数，但只适用于初始表容量（这是我们唯一能保证尊重它的时候）。 我们还声明了一个未使用的 "Segment "类，它只在序列化时以最小形式实例化。

Also, solely for compatibility with previous versions of this class, it extends AbstractMap, even though all of its methods are overridden, so it is just useless baggage.

另外，完全是为了与这个类以前的版本兼容，它扩展了 AbstractMap，尽管它的所有方法都被重写了，所以它只是无用的包袱。

This file is organized to make things a little easier to follow while reading than they might otherwise: First the main static declarations and utilities, then fields, then main public methods (with a few factorings of multiple public methods into internal ones), then sizing methods, trees, traversers, and bulk operations.

这个文件的组织方式是为了让人们在阅读时比其他方式更容易理解。首先是主要的静态声明和实用程序，然后是字段，然后是主要的公共方法（有一些将多个公共方法分解成内部方法的因素），然后是大小方法、树、遍历器和批量操作。

### ConcurrentHashMap (Java5)

#### Overview
A hash table supporting full concurrency of retrievals and adjustable expected concurrency for updates. This class obeys the same functional specification as {@link java.util.Hashtable}, and includes versions of methods corresponding to each method of Hashtable. However, even though all operations are thread-safe, retrieval operations do not entail locking, and there is not any support for locking the entire table in a way that prevents all access.  This class is fully interoperable with Hashtable in programs that rely on its thread safety but not on its synchronization details.

一个支持全并发检索和可调预期并发更新的哈希表。这个类遵守与 {@link java.util.Hashtable} 相同的功能规范，并包括与 Hashtable 的每个方法相对应的方法版本。然而，尽管所有的操作都是线程安全的，但检索操作并不涉及锁定，也不支持以阻止所有访问的方式锁定整个表。 在依赖其线程安全但不依赖其同步细节的程序中，该类与 Hashtable 是完全可以互通的。

Retrieval operations (including get) generally do not block, so may overlap with update operations (including put and remove). Retrievals reflect the results of the most recently completed update operations holding upon their onset. For aggregate operations such as putAll and clear, concurrent retrievals may reflect insertion or removal of only some entries.  Similarly, Iterators and Enumerations return elements reflecting the state of the hash table at some point at or since the creation of the iterator/enumeration. They do not throw {@link ConcurrentModificationException}.  However, iterators are designed to be used by only one thread at a time.

检索操作（包括获取）一般不会阻塞，所以可能与更新操作（包括投放和删除）重叠。检索反映了最近完成的更新操作的结果，在其开始时保持。对于像 putAll 和 clear 这样的聚合操作，并发的检索可能只反映一些条目的插入或移除。 类似地，迭代器和枚举返回反映哈希表在迭代器/枚举创建时或创建后某一时刻的状态的元素。它们不会抛出  ConcurrentModificationException。 然而，迭代器被设计为一次只能由一个线程使用。

The allowed concurrency among update operations is guided by the optional concurrencyLevel constructor argument (default 16), which is used as a hint for internal sizing.  The table is internally partitioned to try to permit the indicated number of concurrent updates without contention. Because placement in hash tables is essentially random, the actual concurrency will vary.  Ideally, you should choose a value to accommodate as many threads as will ever concurrently modify the table. Using a significantly higher value than you need can waste space and time, and a significantly lower value can lead to thread contention. But overestimates and underestimates within an order of magnitude do not usually have much noticeable impact. A value of one is appropriate when it is known that only one thread will modify and all others will only read. Also, resizing this or any other kind of hash table is a relatively slow operation, so, when possible, it is a good idea to provide estimates of expected table sizes in constructors.

更新操作中允许的并发性是由可选的 concurrencyLevel 构造参数（默认为 16）指导的，它被用作内部规模的提示。 该表在内部进行分区，以尝试允许指定数量的并发更新而不发生争执。因为在哈希表中的放置基本上是随机的，实际的并发性会有所不同。 理想情况下，你应该选择一个值来容纳尽可能多的线程来并发修改该表。使用一个明显高于你需要的值会浪费空间和时间，而一个明显较低的值会导致线程争用。但在一个数量级内的高估和低估通常不会有什么明显的影响。当知道只有一个线程会修改，而所有其他的线程只会读取时，一个 1 的值是合适的。另外，调整这个或其他类型的哈希表的大小是一个相对缓慢的操作，所以，在可能的情况下，在构造函数中提供预期表大小的估计是一个好主意。

#### HashEntry
ConcurrentHashMap list entry. Note that this is never exported out as a user-visible Map.Entry. Because the value field is volatile, not final, it is legal wrt the Java Memory Model for an unsynchronized reader to see null instead of initial value when read via a data race.  Although a reordering leading to this is not likely to ever actually occur, the Segment.readValueUnderLock method is used as a backup in case a null (pre-initialized) value is ever seen in an unsynchronized access method.

ConcurrentHashMap 列表条目。请注意，这永远不会作为一个用户可见的 Map.Entry 导出。因为值字段是 volatile 的，而不是 final 的，所以在 Java 内存模型中，当通过数据竞争读取时，非同步读取器看到 null 而不是初始值是合法的。 虽然导致这种情况的重新排序不太可能实际发生，但 Segment.readValueUnderLock 方法被用作备份，以防在非同步访问方法中看到一个空值（预初始化）。

#### Segment
Segments maintain a table of entry lists that are ALWAYS kept in a consistent state, so can be read without locking. Next fields of nodes are immutable (final).  All list additions are performed at the front of each bin. This makes it easy to check changes, and also fast to traverse. When nodes would otherwise be changed, new nodes are created to replace them. This works well for hash tables since the bin lists tend to be short. (The average length is less than two for the default load factor threshold.)

分段维护一个条目列表表，该表始终保持在一致的状态，所以可以在不锁定的情况下读取。节点的下一个字段是不可变的（final）。 所有的列表添加都在每个 bin 的前面进行。这使得它很容易检查变化，也可以快速遍历。当节点被改变时，新的节点被创建以取代它们。这对哈希表很有效，因为 bin 列表往往很短。(对于默认的负载因子阈值，平均长度小于 2）。

Read operations can thus proceed without locking, but rely on selected uses of volatiles to ensure that completed write operations performed by other threads are noticed. For most purposes, the "count" field, tracking the number of elements, serves as that volatile variable ensuring visibility.  This is convenient because this field needs to be read in many read operations anyway:

因此，读操作可以在不加锁的情况下进行，但要依靠对 volatile 的选择使用来确保其他线程执行的已完成的写操作被注意到。在大多数情况下，跟踪元素数量的 "count" 字段可以作为确保可见性的 volatile 变量。这很方便，因为这个字段在许多读操作中都需要被读取。

- All (unsynchronized) read operations must first read the "count" field, and should not look at table entries if it is 0. 
- All (synchronized) write operations should write to the "count" field after structurally changing any bin. The operations must not take any action that could even momentarily cause a concurrent read operation to see inconsistent data. This is made easier by the nature of the read operations in Map. For example, no operation can reveal that the table has grown but the threshold has not yet been updated, so there are no atomicity requirements for this with respect to reads.

- 所有（非同步）读取操作必须首先读取 "count "字段，如果它是0，则不应查看表项。
- 所有（同步的）写操作应该在结构上改变任何 bin 之后，写到 "count" 字段。这些操作不能采取任何可能导致并发的读操作看到不一致数据的行动，哪怕是一时的。这一点由于 Map 中读操作的性质而变得简单。例如，任何操作都不能显示表已经增长，但阈值尚未更新，所以对于读取来说，没有原子性的要求。

As a guide, all critical volatile reads and writes to the count field are marked in code comments.
作为指导，所有对计数字段的关键 volatile 读和写都在代码注释中标明。

## Exchanger
A synchronization point at which threads can pair and swap elements within pairs. Each thread presents some object on entry to the exchange method, matches with a partner thread, and receives its partner's object on return. An Exchanger may be viewed as a bidirectional form of a SynchronousQueue. Exchangers may be useful in applications such as genetic algorithms and pipeline designs.

一个同步点，在这个同步点上，线程可以配对并交换配对中的元素。每个线程在进入交换方法时提出一些对象，与伙伴线程匹配，并在返回时接收其伙伴的对象。交换器可以被看作是同步队列的一种双向形式。交换器在遗传算法和管道设计等应用中可能很有用。

### Sample Usage
Here are the highlights of a class that uses an Exchanger to swap buffers between threads so that the thread filling the buffer gets a freshly emptied one when it needs it, handing off the filled one to the thread emptying the buffer.

下面是一个类的要点，它使用 Exchanger 在线程之间交换缓冲区，这样填充缓冲区的线程在需要的时候就会得到一个刚清空的缓冲区，把已填充的缓冲区交给清空缓冲区的线程。

```
class FillAndEmpty {
   Exchanger<DataBuffer> exchanger = new Exchanger<DataBuffer>();
   DataBuffer initialEmptyBuffer = ... a made-up type
   DataBuffer initialFullBuffer = ...

   class FillingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialEmptyBuffer;
       try {
         while (currentBuffer != null) {
           addToBuffer(currentBuffer);
           if (currentBuffer.isFull())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ... }
     }
   }

   class EmptyingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialFullBuffer;
       try {
         while (currentBuffer != null) {
           takeFromBuffer(currentBuffer);
           if (currentBuffer.isEmpty())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ...}
     }
   }

   void start() {
     new Thread(new FillingLoop()).start();
     new Thread(new EmptyingLoop()).start();
   }
 }
```

Memory consistency effects: For each pair of threads that successfully exchange objects via an Exchanger, actions prior to the exchange() in each thread happen-before those subsequent to a return from the corresponding exchange() in the other thread.

内存一致性效应。对于通过交换器成功交换对象的每一对线程来说，每个线程中 exchange() 之前的动作都会发生在另一个线程中相应的 exchange() 的返回之前。

### Algorithm Description (Java8)
Overview: The core algorithm is, for an exchange "slot",  and a participant (caller) with an item:

概述。核心算法是，对于一个交换 "槽"，和一个有项目的参与者（调用者）。

```
    for (;;) {
        if (slot is empty) {                          // offer
            place item in a Node;
            if (can CAS slot from empty to node) {
                wait for release;
                return matching item in node;
            }
        } else if (can CAS slot from node to empty) { // release
            get the item in node;
            set matching item in node;
            release waiting thread;
        }
        // else retry on CAS failure
    }
```  
This is among the simplest forms of a "dual data structure" --  see Scott and Scherer's DISC 04 paper and http://www.cs.rochester.edu/research/synchronization/pseudocode/duals.html   

This works great in principle. But in practice, like many algorithms centered on atomic updates to a single location, it scales horribly when there are more than a few participants  using the same Exchanger. So the implementation instead uses a form of elimination arena, that spreads out this contention by arranging that some threads typically use different slots,  while still ensuring that eventually, any two parties will be able to exchange items. That is, we cannot completely partition across threads, but instead give threads arena indices that  will on average grow under contention and shrink under lack of  contention. We approach this by defining the Nodes that we need anyway as ThreadLocals, and include in them per-thread index and related bookkeeping state. (We can safely reuse per-thread  nodes rather than creating them fresh each time because slots alternate between pointing to a node vs null, so cannot  encounter ABA problems. However, we do need some care in resetting them between uses.)

这在原则上非常有效。但是在实践中，就像许多以单个位置的原子更新为中心的算法一样，当有几个以上的参与者使用同一个交换器时，它的规模就会大得惊人。因此，该实现使用了一种消除竞技场的形式，通过安排一些线程通常使用不同的槽来分散这种争夺，同时仍然确保最终任何两方都能交换项目。也就是说，我们不能完全划分线程，而是给线程提供竞技场指数，这些指数在争夺的情况下会平均增长，在没有争夺的情况下会平均缩小。我们通过将我们需要的节点定义为 ThreadLocals，并在其中包括每线程索引和相关的簿记状态。(我们可以安全地重复使用每线程节点，而不是每次都创建新的节点，因为槽会交替指向一个节点和空节点，所以不会遇到 ABA 问题。然而，我们确实需要小心地在两次使用之间重新设置它们）。

Implementing an effective arena requires allocating a bunch of space, so we only do so upon detecting contention (except on uniprocessors, where they wouldn't help, so aren't used). Otherwise, exchanges use the single-slot slotExchange method. On contention, not only must the slots be in different locations, but the locations must not encounter memory contention due to being on the same cache line (or more generally, the same coherence unit).  Because, as of this writing, there is no way to determine cacheline size, we define a value that is enough for common platforms.  Additionally, extra care elsewhere is taken to avoid other false/unintended sharing and to enhance locality, including adding padding (via sun.misc.Contended) to Nodes, embedding "bound" as an Exchanger field, and reworking some park/unpark mechanics compared to LockSupport versions.

实现一个有效的竞技场需要分配大量的空间，所以我们只在检测到竞争时才这样做（除了在单处理器上，它们不会有帮助，所以不使用）。否则，交换使用单槽槽位交换方法。在争用时，不仅插槽必须在不同的位置，而且这些位置不能因为在同一高速缓存线上（或者更普遍的，同一一致性单元）而遇到内存争用。 因为截至目前，还没有办法确定缓存线的大小，所以我们定义了一个对普通平台来说足够大的值。 此外，我们还在其他地方采取了额外的措施，以避免其他错误的/非故意的共享，并加强定位，包括为节点添加填充（通过sun.misc.Contended），将 "绑定 "嵌入到 Exchanger 字段中，并与 LockSupport 版本相比，重新修改了一些停放/卸载机制。

The arena starts out with only one used slot. We expand the effective arena size by tracking collisions; i.e., failed CASes while trying to exchange. By nature of the above algorithm, the only kinds of collision that reliably indicate contention are when two attempted releases collide -- one of two attempted offers can legitimately fail to CAS without indicating contention by more than one other thread. (Note: it is possible but not worthwhile to more precisely detect contention by reading slot values after CAS failures.)  When a thread has collided at each slot within the current arena bound, it tries to expand the arena size by one. We track collisions within bounds by using a version (sequence) number on the "bound" field, and conservatively reset collision counts when a participant notices that bound has been updated (in either direction).

竞技场开始时只有一个使用过的插槽。我们通过跟踪碰撞来扩大有效的竞技场规模；即在试图交换时失败的 CAS。根据上述算法的性质，唯一能可靠地表明争夺的碰撞是当两个试图释放的线程发生碰撞时--两个试图提供的线程中的一个可以合法地不能 CAS 而不表明被其他线程争夺。(注意：通过读取 CAS 失败后的槽位值来更精确地检测竞争是可能的，但不值得）。 当一个线程在当前竞技场边界内的每个槽位发生碰撞时，它就会尝试将竞技场的大小扩大一个。我们通过在 "边界"字段上使用版本（序列）号来跟踪边界内的碰撞，并在参与者注意到边界已被更新（在任何方向）时保守地重置碰撞计数。

The effective arena size is reduced (when there is more than one slot) by giving up on waiting after a while and trying to decrement the arena size on expiration. The value of "a while" is an empirical matter.  We implement by piggybacking on the use of spin->yield->block that is essential for reasonable waiting performance anyway -- in a busy exchanger, offers are usually almost immediately released, in which case context switching on multiprocessors is extremely slow/wasteful.  Arena waits just omit the blocking part, and instead cancel. The spin count is empirically chosen to be a value that avoids blocking 99% of the time under maximum sustained exchange rates on a range of test machines. Spins and yields entail some limited randomness (using a cheap xorshift) to avoid regular patterns that can induce unproductive grow/shrink cycles. (Using a pseudorandom also helps regularize spin cycle duration by making branches unpredictable.)  Also, during an offer, a waiter can "know" that it will be released when its slot has changed, but cannot yet proceed until match is set.  In the mean time it cannot cancel the offer, so instead spins/yields. Note: It is possible to avoid this secondary check by changing the linearization point to be a CAS of the match field (as done in one case in the Scott & Scherer DISC paper), which also increases asynchrony a bit, at the expense of poorer collision detection and inability to always reuse per-thread nodes. So the current scheme is typically a better tradeoff.

通过在一段时间后放弃等待并试图在到期时减少竞技场的大小来减少有效的竞技场大小（当有一个以上的槽）。"一段时间"的数值是一个经验性的问题。 我们通过捎带使用spin->yield->block 来实现，这对于合理的等待性能来说是必不可少的--在一个繁忙的交换器中，报价通常几乎是立即释放的，在这种情况下，多处理器上的上下文切换是非常缓慢/浪费的。 竞技场的等待只是省略了阻塞的部分，而是取消。转动次数是根据经验选择的，在一系列测试机器上的最大持续汇率下，这个值可以避免 99% 的阻塞时间。旋转和产量需要一些有限的随机性（使用廉价的 xorshift），以避免可能诱发非生产性增长/收缩周期的规律性模式。(使用伪随机也有助于通过使分支不可预测来规范旋转周期的持续时间）。 另外，在报价期间，一个服务员可以 "知道" 当它的槽位发生变化时它将被释放，但在匹配被设定之前还不能继续。 同时，它不能取消要约，所以要旋转/产生。注意：可以通过改变线性化点为匹配字段的CAS来避免这种二次检查（如 Scott & Scherer DISC 论文中的一个案例），这也增加了一点异步性，代价是更差的碰撞检测和无法总是重复使用每线程节点。所以目前的方案通常是一个更好的权衡。

On collisions, indices traverse the arena cyclically in reverse order, restarting at the maximum index (which will tend to be sparsest) when bounds change. (On expirations, indices instead are halved until reaching 0.) It is possible (and has been tried) to use randomized, prime-value-stepped, or double-hash style traversal instead of simple cyclic traversal to reduce bunching.  But empirically, whatever benefits these may have don't overcome their added overhead: We are managing operations that occur very quickly unless there is sustained contention, so simpler/faster control policies work better than more accurate but slower ones.

在碰撞时，指数以相反的顺序循环遍历竞技场，当界限发生变化时，从最大的指数重新开始（这往往是最稀少的）。(在过期时，指数会减半，直到达到 0。)使用随机化、初值阶梯式或双哈希风格的遍历，而不是简单的循环遍历来减少束缚，是有可能的（并且已经被尝试过）。 但是根据经验，无论这些方法有什么好处，都不能克服它们增加的开销。我们正在管理那些发生得非常快的操作，除非有持续的竞争，所以更简单/更快的控制策略比更精确但更慢的策略更有效。

Because we use expiration for arena size control, we cannot throw TimeoutExceptions in the timed version of the public exchange method until the arena size has shrunken to zero (or the arena isn't enabled). This may delay response to timeout but is still within spec.

因为我们使用过期来控制竞技场的大小，所以我们不能在公共交换方法的定时版本中抛出TimeoutExceptions，直到竞技场大小缩减到零（或者竞技场没有启用）。这可能会延迟对超时的响应，但仍在规范之内。

Essentially all of the implementation is in methods slotExchange and arenaExchange. These have similar overall structure, but differ in too many details to combine. The slotExchange method uses the single Exchanger field "slot" rather than arena array elements. However, it still needs minimal collision detection to trigger arena construction. (The messiest part is making sure interrupt status and InterruptedExceptions come out right during transitions when both methods may be called. This is done by using null return as a sentinel to recheck interrupt status.)

基本上所有的实现都在方法 slotExchange 和 arenaExchange 中。这些方法有类似的整体结构，但在很多细节上有不同，无法合并。slotExchange 方法使用单一的交换器字段 "槽"，而不是竞技场阵列元素。然而，它仍然需要最小的碰撞检测来触发竞技场建设。(最混乱的部分是确保在两个方法都可能被调用的过渡期间，中断状态和 InterruptedExceptions 会正确出现。这是通过使用 null return 作为一个哨兵来重新检查中断状态来实现的）。

As is too common in this sort of code, methods are monolithic because most of the logic relies on reads of fields that are maintained as local variables so can't be nicely factored -- mainly, here, bulky spin->yield->block/cancel code), and heavily dependent on intrinsics (Unsafe) to use inlined embedded CAS and related memory access operations (that tend not to be as readily inlined by dynamic compilers when they are hidden behind other methods that would more nicely name and encapsulate the intended effects). This includes the use of putOrderedX to clear fields of the per-thread Nodes between uses. Note that field Node.item is not declared as volatile even though it is read by releasing threads, because they only do so after CAS operations that must precede access, and all uses by the owning thread are otherwise acceptably ordered by other operations. (Because the actual points of atomicity are slot CASes, it would also be legal for the write to Node.match in a release to be weaker than a full volatile write. However, this is not done because it could allow further postponement of the write, delaying progress.)

在这种代码中很常见，方法是单一的，因为大多数逻辑依赖于对字段的读取，而这些字段是作为局部变量维护的，所以不能很好地被分解--主要是这里，笨重的 spin->yield->block/cancel代码），并且严重依赖内在因素（Unsafe）来使用内联的嵌入式 CAS 和相关内存访问操作（当它们被隐藏在其他方法后面时，动态编译器往往不那么容易内联，这些方法可以更好地命名和封装预期效果）。这包括使用 putOrderedX 来清除每线程节点的字段。请注意，字段Node.item 并没有被声明为 volatile，即使它被释放的线程读取，因为它们只是在 CAS 操作之后才这样做，这些操作必须在访问之前进行，而且所有拥有线程的使用都被其他操作所接受。(因为实际的原子性点是槽的 CAS，所以在释放中对 Node.match 的写入也是合法的，比完全的 volatile 写入更弱。然而，这并没有做到，因为这可能允许进一步推迟写入，延迟进度）。

### Algorithm Description (Java5)

The basic idea is to maintain a "slot", which is a reference to a Node containing both an Item to offer and a "hole" waiting to get filled in.  If an incoming "occupying" thread sees that the slot is null, it CAS'es (compareAndSets) a Node there and waits for another to invoke exchange.  That second "fulfilling" thread sees that the slot is non-null, and so CASes it back to null, also exchanging items by CASing the hole, plus waking up the occupying thread if it is blocked.  In each case CAS'es may fail because a slot at first appears non-null but is null upon CAS, or vice-versa.  So threads may need to retry these actions.

基本的想法是维护一个 "槽"，它是对一个节点的引用，包含一个要提供的项目和一个等待被填补的 "洞"。 如果一个进来的 "占用" 线程看到槽是空的，它就会在那里 CAS'es（compareAndSets）一个Node，并等待另一个线程调用交换。 第二个 "履行"线程看到槽是非空的，于是将其 CAS 回空，也通过CAS 洞来交换项目，如果占用线程被阻塞，则唤醒它。 在每一种情况下，CAS'es 都可能失败，因为一个槽一开始看起来是非空的，但在 CAS 时却是空的，反之亦然。 所以线程可能需要重试这些动作。

This simple approach works great when there are only a few threads using an Exchanger, but performance rapidly deteriorates due to CAS contention on the single slot when there are lots of threads using an exchanger.  So instead we use an "arena"; basically a kind of hash table with a dynamically varying number of slots, any one of which can be used by threads performing an exchange.  Incoming threads pick slots based on a hash of their Thread ids.  If an incoming thread fails to CAS in its chosen slot, it picks an alternative slot instead.  And similarly from there.  If a thread successfully CASes into a slot but no other thread arrives, it tries another, heading toward the zero slot, which always exists even if the table shrinks.  The particular mechanics controlling this are as follows:

当只有几个线程使用交换器时，这种简单的方法非常有效，但当有很多线程使用交换器时，由于 CAS 对单个插槽的争夺，性能会迅速恶化。 因此，我们使用一个 "竞技场"；基本上是一种具有动态变化的插槽数量的哈希表，其中任何一个都可以被执行交换的线程使用。 进来的线程根据他们的线程 ID 的哈希值来选择插槽。 如果一个传入的线程在其选择的槽中没有 CAS，它就会选择一个替代槽。 类似地，从这里开始。 如果一个线程成功地 CAS 到一个槽，但没有其他线程到达，它就会尝试另一个，走向零槽，即使表缩小了，它也总是存在。 控制这一点的具体机制如下。

Waiting: Slot zero is special in that it is the only slot that exists when there is no contention.  A thread occupying slot zero will block if no thread fulfills it after a short spin. In other cases, occupying threads eventually give up and try another slot.  Waiting threads spin for a while (a period that should be a little less than a typical context-switch time) before either blocking (if slot zero) or giving up (if other slots) and restarting.  There is no reason for threads to block unless there are unlikely to be any other threads present. Occupants are mainly avoiding memory contention so sit there quietly polling for a shorter period than it would take to block and then unblock them.  Non-slot-zero waits that elapse because of lack of other threads waste around one extra context-switch time per try, which is still on average much faster than alternative approaches.

等待。0 号槽很特别，因为它是唯一一个在没有竞争的情况下存在的槽。 占据零号槽的线程如果在短暂的旋转后没有线程满足它，就会阻塞。在其他情况下，占用的线程最终会放弃并尝试另一个槽位。 等待的线程在阻塞（如果是零号槽）或放弃（如果是其他槽）并重新启动之前，会旋转一段时间（这段时间应该比典型的上下文切换时间少一点）。 除非不太可能有其他线程存在，否则线程没有理由阻塞。占用者主要是为了避免内存争夺，所以坐在那里静静地轮询，时间要比阻塞的时间短，然后再解除阻塞。 由于没有其他线程，非零槽的等待会在每次尝试中浪费大约一个额外的上下文切换时间，这仍然比其他方法平均快很多。

Sizing: Usually, using only a few slots suffices to reduce contention.  Especially with small numbers of threads, using too many slots can lead to just as poor performance as using too few of them, and there's not much room for error.  The variable "max" maintains the number of slots actually in use.  It is increased when a thread sees too many CAS failures.  (This is analogous to resizing a regular hash table based on a target load factor, except here, growth steps are just one-by-one rather than proportional.)  Growth requires contention failures in each of three tried slots.  Requiring multiple failures for expansion copes with the fact that some failed CASes are not due to contention but instead to simple races between two threads or thread pre-emptions occurring between reading and CASing.  Also, very transient peak contention can be much higher than the average sustainable levels.  The max limit is decreased on average 50% of the times that a non-slot-zero wait elapses without being fulfilled. Threads experiencing elapsed waits move closer to zero, so eventually find existing (or future) threads even if the table has been shrunk due to inactivity.  The chosen mechanics and thresholds for growing and shrinking are intrinsically entangled with indexing and hashing inside the exchange code, and can't be nicely abstracted out.

规模。通常情况下，只使用几个插槽就足以减少争用。 特别是在线程数量较少的情况下，使用太多槽位和使用太少槽位一样会导致性能下降，而且没有太多的出错空间。 变量 "max "保持了实际使用的槽的数量。 当一个线程看到太多的CAS失败时，它就会增加。 (这类似于根据目标负载系数调整常规哈希表的大小，只是在这里，增长步骤只是逐一进行，而不是按比例进行）。 增长需要在三个尝试槽中的每一个都出现争用失败。 扩张需要多次失败，以应对这样一个事实：一些失败的CAS不是由于争用，而是由于两个线程之间的简单竞赛或线程在读取和 CAS 之间发生的抢占。 另外，非常短暂的峰值争用可能比平均可持续水平高得多。 在非槽位为零的等待没有得到满足的情况下，平均 50 %的最大限制会减少。遇到过期等待的线程会更接近于零，所以最终会找到现有（或未来）的线程，即使表因不活动而缩减。 所选择的增长和收缩的机制和阈值与交换代码中的索引和散列有内在的纠葛，不能很好地抽象出来。

Hashing: Each thread picks its initial slot to use in accord with a simple hashcode.  The sequence is the same on each encounter by any given thread, but effectively random across threads.  Using arenas encounters the classic cost vs quality tradeoffs of all hash tables.  Here, we use a one-step FNV-1a hash code based on the current thread's Thread.getId(), along with a cheap approximation to a mod operation to select an index.  The downside of optimizing index selection in this way is that the code is hardwired to use a maximum table size of 32.  But this value more than suffices for known platforms and applications.

哈希：每个线程根据一个简单的哈希码来选择其初始槽。 该序列在任何给定的线程的每次遭遇中都是相同的，但实际上是跨线程的随机。 使用竞技场会遇到所有哈希表的经典成本与质量的权衡。 在这里，我们使用一个基于当前线程的 Thread.getId() 的一步 FNV-1a 哈希代码，以及一个廉价的近似 mod 操作来选择一个索引。 以这种方式优化索引选择的缺点是，代码被硬性规定为使用 32 的最大表尺寸。 但是这个值对于已知的平台和应用来说已经足够了。

Probing: On sensed contention of a selected slot, we probe sequentially through the table, analogously to linear probing after collision in a hash table.  (We move circularly, in reverse order, to mesh best with table growth and shrinkage rules.)  Except that to minimize the effects of false-alarms and cache thrashing, we try the first selected slot twice before moving. 

探测。在感觉到对所选槽位的争夺时，我们按顺序对表进行探测，类似于哈希表中碰撞后的线性探测。 (我们以相反的顺序循环移动，以便与表的增长和缩减规则最匹配)。 除了为了最大限度地减少误报和高速缓存的影响，我们在移动之前对第一个选定的槽位进行两次尝试。

Padding: Even with contention management, slots are heavily contended, so use cache-padding to avoid poor memory performance.  Because of this, slots are lazily constructed only when used, to avoid wasting this space unnecessarily. While isolation of locations is not much of an issue at first in an application, as time goes on and garbage-collectors perform compaction, slots are very likely to be moved adjacent to each other, which can cause much thrashing of cache lines on MPs unless padding is employed. 

填充。即使有争用管理，槽也会被大量争用，所以使用缓存填充来避免内存性能不佳。 正因为如此，插槽只有在使用时才会被懒惰地构建，以避免不必要地浪费这个空间。虽然在应用程序中，位置的隔离起初不是什么问题，但随着时间的推移和垃圾收集器的压缩，槽很可能会被移到彼此相邻的地方，这可能会导致 MP 上的缓存行被大量地破坏，除非采用填充。

This is an improvement of the algorithm described in the paper "A Scalable Elimination-based Exchange Channel" by William Scherer, Doug Lea, and Michael Scott in Proceedings of SCOOL05 workshop.  Available at: http://hdl.handle.net/1802/2104

这是对 William Scherer、Doug Lea 和 Michael Scott 在《A Scalable Elimination-based Exchange Channel》中描述的算法的改进。 可参阅：http://hdl.handle.net/1802/2104