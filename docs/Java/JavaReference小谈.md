# Java Reference 小谈

[TOC]

## Reference
Abstract base class for reference objects. This class defines the operations common to all reference objects. Because reference objects are implemented in close cooperation with the garbage collector, this class may not be subclassed directly.
引用对象的抽象基类。这个类定义了所有引用对象的共同操作。因为引用对象是与垃圾收集器紧密合作实现的，这个类不能直接被子类化。

A Reference instance is in one of four possible internal states:
一个引用实例处于四种可能的内部状态之一。

Active: Subject to special treatment by the garbage collector.  Some time after the collector detects that the reachability of the referent has changed to the appropriate state, it changes the instance's state to either Pending or Inactive, depending upon whether or not the instance was registered with a queue when it was created.  In the former case it also adds the instance to the pending-Reference list.  Newly-created instances are Active.
活跃状态。受制于垃圾收集器的特殊处理。 在收集器检测到引用者的可及性已经改变为适当的状态后的一段时间，它将实例的状态改变为挂起或不活动，这取决于实例在创建时是否在队列中注册。 在前一种情况下，它还会将该实例添加到待定参考列表中。 新创建的实例是活动的。

Pending: An element of the pending-Reference list, waiting to be enqueued by the Reference-handler thread. Unregistered instances are never in this state.
待定状态。待定引用链表中的一个元素，等待被引用处理线程进行入队操作。未注册的实例从不处于这种状态。

Enqueued: An element of the queue with which the instance was registered when it was created.  When an instance is removed from its ReferenceQueue, it is made Inactive. Unregistered instances are never in this state.
排队状态。实例创建时注册的队列中的一个元素。 当一个实例从它的 ReferenceQueue 中被移除时，它就会变成Inactive。 未注册的实例从不处于这种状态。

Inactive: Nothing more to do. Once an instance becomes Inactive its state will never change again.
不活跃。没有什么可做的了。一旦一个实例变得不活跃，它的状态就不会再改变。

### 状态的编码说明
The state is encoded in the queue and next fields as follows:
该状态在队列和 next 字段中的编码如下。

Active: queue = ReferenceQueue with which instance is registered, or ReferenceQueue.NULL if it was not registered with a queue; next = null.
活跃状态：queue = 实例注册的 ReferenceQueue，如果没有在队列中注册，则 queue = ReferenceQueue.NULL; next = null.

Pending: queue = ReferenceQueue with which instance is registered; next = this
待定状态：queue = 实例注册的 ReferenceQueue; next = this

Enqueued: queue = ReferenceQueue.ENQUEUED; next = Following instance in queue, or this if at end of list.
Enqueued 状态: 此时 queue = ReferenceQueue.ENQUEUED; next = 队列中的下一个实例，如果在列表的最后，则为 this。

Inactive: queue = ReferenceQueue.NULL; next = this.

With this scheme the collector need only examine the next field in order to determine whether a Reference instance requires special treatment: If the next field is null then the instance is active; if it is non-null, then the collector should treat the instance normally.
通过这个方案，收集器只需要检查 next 字段，以确定一个引用实例是否需要特殊处理。如果 next 字段是空的，那么该实例是活动的；如果它不是空的，那么收集器应该正常处理该实例。

To ensure that a concurrent collector can discover active Reference objects without interfering with application threads that may apply the enqueue() method to those objects, collectors should link discovered objects through the discovered field. The discovered field is also used for linking Reference objects in the pending list.
为了确保并发收集器能够发现活动的 Reference 对象，而不干扰可能对这些对象应用 enqueue() 方法的应用线程，收集器应该通过 discovered 字段来链接已发现的对象。discovered 字段也被用来链接待定列表中的引用对象。

关键代码如下所示。
```
    private static Reference<Object> pending = null;
    
    static boolean tryHandlePending(boolean waitForNotify) {
        Reference<Object> r;
        Cleaner c;
        try {
            synchronized (lock) {
                // 当 pending 发现有值时，将其存到本地，并将本地的 discoverd 字段作为链表的下一个元素，进行访问。
                if (pending != null) {
                    r = pending;
                    c = r instanceof Cleaner ? (Cleaner) r : null;
                    pending = r.discovered;
                    r.discovered = null;
                } else {
                    if (waitForNotify) {
                        lock.wait();
                    }
                    return waitForNotify;
                }
            }
        } catch (OutOfMemoryError x) {
            Thread.yield();
            return true;
        } catch (InterruptedException x) {
            return true;
        }

        // Fast path for cleaners
        if (c != null) {
            c.clean();
            return true;
        }

        ReferenceQueue<? super Object> q = r.queue;
        if (q != ReferenceQueue.NULL) q.enqueue(r);
        return true;
```

### 状态的编码小结
当 Reference 的 next 字段为空时，说明该 reference 处于活跃状态；如果不为空，并且该引用所在的 queue 字段没有指向空，也不是排队状态。则该 reference 处于 pendding 状态。反之，如果 queue 字段等于 ReferenceQueue.ENQUEUED，说明该 reference 处于排队状态。如果 queue 等于 ReferenceQueue.NULL，则该 reference 处于非活跃状态。

### pending 字段
首先该字段是一个静态字段，所有引用共享这个队列。所以访问这个队列，需要进行同步。

```
    private static Reference<Object> pending = null;
```

List of References waiting to be enqueued.  The collector adds References to this list, while the Reference-handler thread removes them.  This list is protected by the above lock object. The list uses the discovered field to link its elements.

等待被排队的引用列表。收集器将引用添加到这个列表中，而引用处理程序线程将其删除。这个列表受到上述锁对象的保护。该列表使用 discovered 字段来连接其元素。

### discovered 字段
discovered 是 Reference 的实例字段，由虚拟机使用。用于维护引用链表。

When active: next element in a discovered reference list maintained by GC (or this if last)
活动状态时：由 GC 维护的被发现的引用列表中的下一个元素（如果是最后一个，则是这个元素）。

pending:   next element in the pending list (or null if last)
待定状态时：在 pending 列表中的下一个元素（如果是最后一个，则为空）。

otherwise:   NULL

## ReferenceQueue
Reference queues, to which registered reference objects are appended by the garbage collector after the appropriate reachability changes are detected.
引用队列，在检测到适当的可达性变化后，垃圾收集器将注册的引用对象追加到该队列中。

入队代码如下。
```
    // 仅仅由 Reference 类调用
    boolean enqueue(Reference<? extends T> r) {
        synchronized (lock) {
            if ((queue == NULL) || (queue == ENQUEUED)) {
                return false;
            }
            assert queue == this;
            r.queue = ENQUEUED;
            // 头插法
            r.next = (head == null) ? r : head;
            head = r;
            queueLength++;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(1);
            }
            lock.notifyAll();
            return true;
        }
    }
```

从队列取出代码如下。
```
    private Reference<? extends T> reallyPoll() {
        Reference<? extends T> r = head;
        if (r != null) {
            Reference<? extends T> rn = r.next;
            head = (rn == r) ? null : rn;
            r.queue = NULL;
            r.next = r;
            queueLength--;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(-1);
            }
            return r;
        }
        return null;
    }
```

## SoftReference
Soft reference objects, which are cleared at the discretion of the garbage collector in response to memory demand. Soft references are most often used to implement memory-sensitive caches.

软引用对象，由垃圾收集器决定是否清除，以响应内存需求。软引用最常被用来实现对内存敏感的缓冲区。

Suppose that the garbage collector determines at a certain point in time that an object is softly reachable. At that time it may choose to clear atomically all soft references to that object and all soft references to any other softly-reachable objects from which that object is reachable through a chain of strong references. At the same time or at some later time it will enqueue those newly-cleared soft references that are registered with reference queues.

假设垃圾收集器在某个时间点上确定一个对象是软性可及的。在那个时候，它可以选择以原子方式清除对该对象的所有软引用，以及对任何其他软可达对象的所有软引用，该对象可以通过强引用链到达。在同一时间或稍后的时间，它将对那些在引用队列中注册的新清除的软引用进行排队。

All soft references to softly-reachable objects are guaranteed to have been cleared before the virtual machine throws an OutOfMemoryError. Otherwise no constraints are placed upon the time at which a soft reference will be cleared or the order in which a set of such references to different objects will be cleared. Virtual machine implementations are, however, encouraged to bias against clearing recently-created or recently-used soft references.

在虚拟机抛出 OutOfMemoryError 之前，保证所有对软性可达对象的软性引用都被清除了。否则，对软引用被清除的时间或对不同对象的一组引用被清除的顺序没有任何限制。然而，我们鼓励虚拟机实现偏向于清除最近创建的或最近使用的软引用。

Direct instances of this class may be used to implement simple caches; this class or derived subclasses may also be used in larger data structures to implement more sophisticated caches. As long as the referent of a soft reference is strongly reachable, that is, is actually in use, the soft reference will not be cleared. Thus a sophisticated cache can, for example, prevent its most recently used entries from being discarded by keeping strong referents to those entries, leaving the remaining entries to be discarded at the discretion of the garbage collector.

这个类的直接实例可以用来实现简单的缓存；这个类或派生的子类也可以用在更大的数据结构中来实现更复杂的缓存。只要一个软引用的引用者是强可及的，也就是说，实际上是在使用中的，那么这个软引用就不会被清除。因此，一个复杂的缓冲区可以，例如，通过保持这些条目的强引用来防止其最近使用的条目被丢弃，剩下的条目则由垃圾收集器来决定是否丢弃。

### 源代码说明
```
    // 静态共享字段，时间戳时钟字段，由垃圾收集器更新
    static private long clock;
 
    // 由每次调用get方法更新的时间戳。在选择要清除的软引用时，虚拟机可以使用这个字段，但它不需要这样做。
    private long timestamp;

    // 构造函数时，使用当前时钟给 timestamp 赋值。
    public SoftReference(T referent) {
        super(referent);
        this.timestamp = clock;
    }

    public SoftReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
        this.timestamp = clock;
    }

    // 返回对象的引用。如果这个对象已经被程序或垃圾收集器清除了，那么这个方法就会返回 null。
    public T get() {
        T o = super.get();
        if (o != null && this.timestamp != clock)
            this.timestamp = clock;
        return o;
    }
```

## WeakReference
Weak reference objects, which do not prevent their referents from being made finalizable, finalized, and then reclaimed. Weak references are most often used to implement canonicalizing mappings.

弱引用对象，它不阻止它们的引用被 finalizable，finalized，然后被回收。弱引用最常被用来实现规范化的映射。

Suppose that the garbage collector determines at a certain point in time that an object is weakly reachable. At that time it will atomically clear all weak references to that object and all weak references to any other weakly-reachable objects from which that object is reachable through a chain of strong and soft references. At the same time it will declare all of the formerly weakly-reachable objects to be finalizable. At the same time or at some later time it will enqueue those newly-cleared weak references that are registered with reference queues.

假设垃圾收集器在某个时间点上确定一个对象是弱可及的。那时，它将原子化地清除对该对象的所有弱引用，以及对任何其他弱可及对象的所有弱引用，该对象可以通过强引用和软引用链到达。同时，它将声明所有以前可被弱化的对象是 finalizable 的。在同一时间或稍后的时间，它将对那些在引用队列中注册的新清除的弱引用进行排队。

### WeakHashMap
Hash table based implementation of the Map interface, with weak keys. An entry in a WeakHashMap will automatically be removed when its key is no longer in ordinary use. More precisely, the presence of a mapping for a given key will not prevent the key from being discarded by the garbage collector, that is, made finalizable, finalized, and then reclaimed. When a key has been discarded its entry is effectively removed from the map, so this class behaves somewhat differently from other Map implementations.

基于哈希表的 Map 接口的实现，具有弱键。WeakHashMap 中的一个条目将在其键不再被正常使用时自动被删除。更确切地说，一个给定键的映射的存在不会阻止该键被垃圾收集器丢弃，也就是说，会使其 finalizable, finalized, and then reclaimed. 当一个键被丢弃时，它的条目被有效地从 map 中删除，所以这个类的行为与其他 map 实现有些不同。

Both null values and the null key are supported. This class has performance characteristics similar to those of the HashMap class, and has the same efficiency parameters of initial capacity and load factor.

空值和空键都被支持。该类的性能特征与HashMap类相似，并且具有相同的初始容量和负载系数的效率参数。

Like most collection classes, this class is not synchronized. A synchronized WeakHashMap may be constructed using the Collections.synchronizedMap method.

像大多数集合类一样，这个类是不同步的。一个同步的 WeakHashMap 可以用Collections.synchronizedMap 方法来构造。

This class is intended primarily for use with key objects whose equals methods test for object identity using the == operator. Once such a key is discarded it can never be recreated, so it is impossible to do a lookup of that key in a WeakHashMap at some later time and be surprised that its entry has been removed. This class will work perfectly well with key objects whose equals methods are not based upon object identity, such as String instances. With such recreatable key objects, however, the automatic removal of WeakHashMap entries whose keys have been discarded may prove to be confusing.

这个类主要是为了用于那些使用 == 运算符测试对象身份的等价方法的键对象。一旦这样的键被丢弃，它就不能被重新创建，所以不可能在以后的某个时候在 WeakHashMap 中对该键进行查找，并惊讶地发现它的条目已被删除。这个类可以很好地适用于那些等价方法不基于对象身份的键对象，比如 String 实例。然而，对于这种可重新创建的键对象，自动删除键已被丢弃的 WeakHashMap 条目可能会被证明是混乱的。

The behavior of the WeakHashMap class depends in part upon the actions of the garbage collector, so several familiar (though not required) Map invariants do not hold for this class. Because the garbage collector may discard keys at any time, a WeakHashMap may behave as though an unknown thread is silently removing entries. In particular, even if you synchronize on a WeakHashMap instance and invoke none of its mutator methods, it is possible for the size method to return smaller values over time, for the isEmpty method to return false and then true, for the containsKey method to return true and later false for a given key, for the get method to return a value for a given key but later return null, for the put method to return null and the remove method to return false for a key that previously appeared to be in the map, and for successive examinations of the key set, the value collection, and the entry set to yield successively smaller numbers of elements.

WeakHashMap 类的行为部分取决于垃圾收集器的行为，因此几个熟悉的（虽然不是必须的）Map 不变性对该类不成立。因为垃圾收集器可以在任何时候丢弃键值，WeakHashMap 的行为可能就像一个未知的线程在默默地删除条目。特别是，即使你在 WeakHashMap 实例上进行了同步，并且没有调用它的任何可变性方法，size 方法也有可能随着时间的推移返回更小的值，isEmpty 方法有可能先返回 false 然后再返回 true，containsKey 方法有可能先返回 true 然后再返回 false。get 方法为一个给定的键返回一个值，但后来又返回 null，put 方法返回 null，remove 方法对一个以前似乎在 map 中的键返回false，以及对键集、值集和条目集的连续检查产生的元素数量逐渐减少。

Each key object in a WeakHashMap is stored indirectly as the referent of a weak reference. Therefore a key will automatically be removed only after the weak references to it, both inside and outside of the map, have been cleared by the garbage collector.

WeakHashMap 中的每个键对象都被间接地存储为弱引用的引用。因此，只有在 map 内部和外部对它的弱引用被垃圾收集器清除后，一个键才会被自动删除。

Implementation note: The value objects in a WeakHashMap are held by ordinary strong references. Thus care should be taken to ensure that value objects do not strongly refer to their own keys, either directly or indirectly, since that will prevent the keys from being discarded. Note that a value object may refer indirectly to its key via the WeakHashMap itself; that is, a value object may strongly refer to some other key object whose associated value object, in turn, strongly refers to the key of the first value object. If the values in the map do not rely on the map holding strong references to them, one way to deal with this is to wrap values themselves within WeakReferences before inserting, as in: m.put(key, new WeakReference(value)), and then unwrapping upon each get.

实现说明：WeakHashMap 中的值对象是由普通的强引用持有的。因此，应该注意确保值对象不直接或间接地强引用它们自己的键，因为这将防止键被丢弃。请注意，一个值对象可以通过 WeakHashMap 本身间接地引用它的键；也就是说，一个值对象可以强烈地引用其他一些键对象，而这些键对象的相关值对象又强烈地引用了第一个值对象的键。如果 map 中的值不依赖于 map 对它们的强引用，处理这个问题的一个方法是在插入前将值本身包裹在 WeakReference 中，如：m.put(key, new WeakReference(value))，然后在每次获取时解除包裹。

The iterators returned by the iterator method of the collections returned by all of this class's "collection view methods" are fail-fast: if the map is structurally modified at any time after the iterator is created, in any way except through the iterator's own remove method, the iterator will throw a ConcurrentModificationException. Thus, in the face of concurrent modification, the iterator fails quickly and cleanly, rather than risking arbitrary, non-deterministic behavior at an undetermined time in the future.

这个类的所有 "集合视图方法 "所返回的集合的迭代器是快速失败的：如果地图在迭代器创建后的任何时候被结构性地修改，除了通过迭代器自己的移除方法，迭代器会抛出一个ConcurrentModificationException。因此，在面对并发修改时，迭代器会快速而干净地失败，而不是在未来某个不确定的时间冒着任意的、不确定的行为。

Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally speaking, impossible to make any hard guarantees in the presence of unsynchronized concurrent modification. Fail-fast iterators throw ConcurrentModificationException on a best-effort basis. Therefore, it would be wrong to write a program that depended on this exception for its correctness: the fail-fast behavior of iterators should be used only to detect bugs.

请注意，迭代器的故障快速行为不能被保证，因为一般来说，在非同步并发修改的情况下，不可能做出任何硬性保证。失败快速迭代器在尽力的基础上抛出 ConcurrentModificationException。因此，编写一个依靠这个异常来保证其正确性的程序是错误的：迭代器的故障快速行为应该只用于检测错误。

### WeakHashMap 中的使用
```
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    private void expungeStaleEntries() {
        // 从引用队列中获取被系统回收的了 WeakReference。清空其 value 以及清理 map 中的 WeakReference 本身。
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                Entry<K,V> e = (Entry<K,V>) x;
                int i = indexFor(e.hash, table.length);

                Entry<K,V> prev = table[i];
                Entry<K,V> p = prev;
                while (p != null) {
                    Entry<K,V> next = p.next;
                    if (p == e) {
                        if (prev == e)
                            table[i] = next;
                        else
                            prev.next = next;
                        // Must not null out e.next;
                        // stale entries may be in use by a HashIterator
                        e.value = null; // Help GC
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }

    // expungeStaleEntries() 方法会在其访问桶时触发。
    private Entry<K,V>[] getTable() {
        expungeStaleEntries();
        return table;
    }

    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }
```

## PhantomReference
Phantom reference objects, which are enqueued after the collector determines that their referents may otherwise be reclaimed. Phantom references are most often used for scheduling pre-mortem cleanup actions in a more flexible way than is possible with the Java finalization mechanism.

幻影引用对象，在收集器确定其引用对象可能被回收后被排队。幻影引用最常被用于调度死前清理行动，其方式比 Java 的 finalization 处理机制更灵活。

If the garbage collector determines at a certain point in time that the referent of a phantom reference is phantom reachable, then at that time or at some later time it will enqueue the reference.

如果垃圾收集器在某个时间点确定一个幻象引用的引用者是幻象可及的，那么在那个时间点或以后的某个时间点，它将排队等待该引用。

In order to ensure that a reclaimable object remains so, the referent of a phantom reference may not be retrieved: The get method of a phantom reference always returns null.

为了确保一个可回收的对象可被回收，幻影引用的引用对象不能被检索。幻影引用的获取方法总是返回 null。

Unlike soft and weak references, phantom references are not automatically cleared by the garbage collector as they are enqueued. An object that is reachable via phantom references will remain so until all such references are cleared or themselves become unreachable.

与软引用和弱引用不同，幻影引用在被排队时不会被垃圾收集器自动清除。一个可以通过幻影引用到达的对象将保持这种状态，直到所有这些引用被清除或者它们自己变得不可到达。

```
    // 返回这个引用对象的引用。因为幻象引用的引用对象总是不可访问的，这个方法总是返回null。
    public T get() {
        return null;
    }
```

### Cleaner
```
    public class Cleaner extends PhantomReference<Object> {
        private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue();
        private static Cleaner first = null;
        private Cleaner next = null;
        private Cleaner prev = null;
        private final Runnable thunk;

        private static synchronized Cleaner add(Cleaner var0) {
            if (first != null) {
                var0.next = first;
                first.prev = var0;
            }

            first = var0;
            return var0;
        }

        private static synchronized boolean remove(Cleaner var0) {
            if (var0.next == var0) {
                return false;
            } else {
                if (first == var0) {
                    if (var0.next != null) {
                        first = var0.next;
                    } else {
                        first = var0.prev;
                    }
                }

                if (var0.next != null) {
                    var0.next.prev = var0.prev;
                }

                if (var0.prev != null) {
                    var0.prev.next = var0.next;
                }

                var0.next = var0;
                var0.prev = var0;
                return true;
            }
        }

        private Cleaner(Object var1, Runnable var2) {
            super(var1, dummyQueue);
            this.thunk = var2;
        }

        public static Cleaner create(Object var0, Runnable var1) {
            return var1 == null ? null : add(new Cleaner(var0, var1));
        }

        public void clean() {
            if (remove(this)) {
                try {
                    this.thunk.run();
                } catch (final Throwable var2) {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        public Void run() {
                            if (System.err != null) {
                                (new Error("Cleaner terminated abnormally", var2)).printStackTrace();
                            }

                            System.exit(1);
                            return null;
                        }
                    });
                }

            }
        }
    }
```

## FinalReference
Final references, used to implement finalization
final 引用，用于实施最后的处理

当对象被标记为垃圾，即将被回收之前，JVM 会将重写了 finalize() 且没有被调用过的对象送入 Finalizer 的引用队列，由 Finalizer 线程从该队列获取该对象，并调用该对象的 finalize()，如果这次 finalize() 方法执行过后，该对象依然被判定为垃圾，则该对象会被真正回收。

### Finalizer
```
    final class Finalizer extends FinalReference<Object> {
        private static ReferenceQueue<Object> queue = new ReferenceQueue<>();
        private static Finalizer unfinalized = null;
        private static final Object lock = new Object();

        private Finalizer next = null, prev = null;

        private boolean hasBeenFinalized() {
            return (next == this);
        }

        private void add() {
            synchronized (lock) {
                if (unfinalized != null) {
                    this.next = unfinalized;
                    unfinalized.prev = this;
                }
                unfinalized = this;
            }
        }

        private void remove() {
            synchronized (lock) {
                if (unfinalized == this) {
                    if (this.next != null) {
                        unfinalized = this.next;
                    } else {
                        unfinalized = this.prev;
                    }
                }
                if (this.next != null) {
                    this.next.prev = this.prev;
                }
                if (this.prev != null) {
                    this.prev.next = this.next;
                }
                this.next = this;
                this.prev = this;
            }
        }

        private Finalizer(Object finalizee) {
            super(finalizee, queue);
            add();
        }

        static ReferenceQueue<Object> getQueue() {
            return queue;
        }

        /* Invoked by VM */
        static void register(Object finalizee) {
            new Finalizer(finalizee);
        }

        private void runFinalizer(JavaLangAccess jla) {
            synchronized (this) {
                if (hasBeenFinalized()) return;
                remove();
            }
            try {
                Object finalizee = this.get();
                if (finalizee != null && !(finalizee instanceof java.lang.Enum)) {
                    jla.invokeFinalize(finalizee);
                    finalizee = null;
                }
            } catch (Throwable x) { }
            super.clear();
        }

        /* 在系统线程组中为给定的Runnable创建一个有特权的二级终结者线程，并等待它完成。

        这个方法被 runFinalization 和 runFinalizersOnExit 使用。前者调用所有待定的终结者，而后者如果启用了退出时的终结，则调用所有未调用的终结者。

        这两个方法本可以通过将它们的工作卸载到常规的终结者线程并等待该线程完成来实现。
        然而，创建一个新线程的好处是，它将这些方法的调用者与停滞或死锁的终结者线程隔离开。
        */
        private static void forkSecondaryFinalizer(final Runnable proc) {
            AccessController.doPrivileged(
                new PrivilegedAction<Void>() {
                    public Void run() {
                        ThreadGroup tg = Thread.currentThread().getThreadGroup();
                        for (ThreadGroup tgn = tg;
                            tgn != null;
                            tg = tgn, tgn = tg.getParent());
                        Thread sft = new Thread(tg, proc, "Secondary finalizer");
                        sft.start();
                        try {
                            sft.join();
                        } catch (InterruptedException x) {
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }});
        }

        /* Called by Runtime.runFinalization() */
        static void runFinalization() {
            if (!VM.isBooted()) {
                return;
            }

            forkSecondaryFinalizer(new Runnable() {
                private volatile boolean running;
                public void run() {
                    // in case of recursive call to run()
                    if (running)
                        return;
                    final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                    running = true;
                    for (;;) {
                        Finalizer f = (Finalizer)queue.poll();
                        if (f == null) break;
                        f.runFinalizer(jla);
                    }
                }
            });
        }

        /* Invoked by java.lang.Shutdown */
        static void runAllFinalizers() {
            if (!VM.isBooted()) {
                return;
            }

            forkSecondaryFinalizer(new Runnable() {
                private volatile boolean running;
                public void run() {
                    // in case of recursive call to run()
                    if (running)
                        return;
                    final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                    running = true;
                    for (;;) {
                        Finalizer f;
                        synchronized (lock) {
                            f = unfinalized;
                            if (f == null) break;
                            unfinalized = f.next;
                        }
                        f.runFinalizer(jla);
                    }}});
        }

        private static class FinalizerThread extends Thread {
            private volatile boolean running;
            FinalizerThread(ThreadGroup g) {
                super(g, "Finalizer");
            }
            public void run() {
                // in case of recursive call to run()
                if (running)
                    return;

                while (!VM.isBooted()) {
                    // delay until VM completes initialization
                    try {
                        VM.awaitBooted();
                    } catch (InterruptedException x) {
                        // ignore and continue
                    }
                }
                final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                running = true;
                for (;;) {
                    try {
                        Finalizer f = (Finalizer)queue.remove();
                        f.runFinalizer(jla);
                    } catch (InterruptedException x) {
                        // ignore and continue
                    }
                }
            }
        }

        static {
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            for (ThreadGroup tgn = tg;
                tgn != null;
                tg = tgn, tgn = tg.getParent());
            Thread finalizer = new FinalizerThread(tg);
            finalizer.setPriority(Thread.MAX_PRIORITY - 2);
            finalizer.setDaemon(true);
            finalizer.start();
        }

    }
```