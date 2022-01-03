package com.future.concurrent.fakelib;

import io.netty.channel.Channel;
import sun.lwawt.macosx.CTrayIcon;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

/**
 * 提供一个框架，用于实现依赖先进先出（FIFO）等待队列的阻塞锁和相关同步器（信号量、事件等）。
 * 此类被设计为大多数类型的同步器的有用基础，这些同步器依赖于单个原子 int 值来表示状态。
 * 子类必须定义更改此状态的受保护方法，以及定义此状态在获取或释放此对象方面的含义。
 * 鉴于这些，该类中的其他方法执行所有排队和阻塞机制。
 * 子类可以维护其他状态字段，
 * 但只有使用 getState、setState 和 compareAndSetState 方法操作的原子更新的 int 值才能跟踪同步。
 * <p>
 * 子类应定义为非公共内部帮助器类，用于实现其封闭类的同步属性。
 * 类 AbstractQueuedSynchronizer 未实现任何同步接口。
 * 相反，它定义了一些方法，例如 acquireInterruptibly，
 * 具体锁和相关同步器可以适当地调用这些方法来实现它们的公共方法。
 * <p>
 * 此类支持默认独占模式和共享模式之一或两者。
 * 在独占模式下获取时，其他线程尝试的获取无法成功。多线程获取共享模式可能（但不需要）成功。
 * 这个类不“理解”这些差异，除非在机械意义上，当共享模式获取成功时，下一个等待线程（如果存在）也必须确定它是否可以获取。
 * 在不同模式下等待的线程共享相同的FIFO队列。
 * 通常，实现子类只支持其中一种模式，但这两种模式都可以发挥作用，
 * 例如在 ReadWriteLock 中。仅支持独占或共享模式的子类不需要定义支持未使用模式的方法。
 * <p>
 * 此类定义嵌套的 AbstractQueuedSynchronizer。ConditionObject 类，
 * 可由支持独占模式的子类用作条件实现，方法 ISHELDExclusive 为其报告是否以独占方式保持与当前线程的同步，
 * 使用当前 getState 值调用的方法 release 完全释放此对象，并获取，给定此保存的状态值，最终将此对象恢复到其先前获取的状态。
 * 没有任何 AbstractQueuedSynchronizer 方法会创建这样的条件，因此如果无法满足此约束，请不要使用它。
 * AbstractQueuedSynchronizer 的行为。ConditionObject 当然取决于其同步器实现的语义。
 * <p>
 * 此类提供内部队列的检查、检测和监视方法，以及条件对象的类似方法。
 * 可以根据需要使用 AbstractQueuedSynchronizer 将它们导出到类中，作为它们的同步机制。
 * 此类的序列化只存储底层原子整数维护状态，因此反序列化对象具有空线程队列。
 * 需要序列化的典型子类将定义一个readObject方法，该方法在反序列化时将其恢复到已知的初始状态。
 *
 * <b>用法</b>
 * <p>
 * 要将此类用作同步器的基础，请通过使用 getState、setState和/或 compareAndSetState检查和/或修改同步状态，
 * 重新定义以下方法（如适用）：
 * + tryAcquire
 * + tryRelease
 * + tryAcquireShared
 * + tryReleaseShared
 * + isHeldExclusively
 * <p>
 * 默认情况下，这些方法中的每一个都会引发 UnsupportedOperationException。
 * 这些方法的实现必须是内部线程安全的，并且通常应该是简短的，而不是阻塞的。
 * 定义这些方法是使用此类的唯一受支持的方法。所有其他方法都被宣布为 final 方法，因为它们不能独立地改变。
 * <p>
 * 您还可能发现从 AbstractOwnableSynchronizer 继承的方法对于跟踪拥有独占同步器的线程非常有用。
 * 我们鼓励您使用它们——这使监视和诊断工具能够帮助用户确定哪些线程持有锁。
 * <p>
 * 即使此类基于内部 FIFO 队列，它也不会自动强制执行 FIFO 采集策略。独占同步的核心采用以下形式：
 * <p>
 * *   Acquire:
 * *       while (!tryAcquire(arg)) {
 * *          enqueue thread if it is not already queued;
 * *          possibly block current thread;
 * *       }
 * <p>
 * *   Release:
 * *       if (tryRelease(arg))
 * *          unblock the first queued thread;
 * <p>
 * （共享模式类似，但可能涉及级联信号。）
 * <p>
 * 由于 acquire 中的调用是在排队之前调用的，因此新的获取线程可能会抢先阻塞并排队的其他线程。
 * 但是，如果需要，您可以通过内部调用一种或多种检查方法来定义 tryAcquire和/或tryAcquireShared 以禁用驳船，
 * 从而提供公平的FIFO获取命令。
 * 特别是，如果 hasQueuedPredecessors（一种专门为公平同步器设计的方法）返回true，
 * 大多数公平同步器可以定义 tryAcquire 以返回false。其他变化也是可能的。
 * <p>
 * 默认驳船（也称为贪婪、放弃和避免护航）策略的吞吐量和可伸缩性通常最高。
 * 虽然这不能保证公平或无饥饿，但允许较早排队的线程在稍后排队的线程之前重新调度，
 * 并且每个重新调度都有一个针对传入线程的无偏见的成功机会。
 * 此外，虽然获取不会在通常意义上“旋转”，但它们可能会在阻塞之前执行多个tryAcquire调用，并穿插其他计算。
 * 当独占同步仅短暂保持时，这提供了旋转的大部分好处，而当不保持独占同步时，则没有大部分责任。
 * 如果需要，您可以通过前面的调用来增强这一点，以获取具有“快速路径”检查的方法，
 * 可能只在同步器可能不会被争用的情况下，预先检查 hasContented 和/或 hasQueuedThreads 以执行此操作。
 * <p>
 * 该类为同步提供了一个高效且可扩展的基础，
 * 部分是通过将其使用范围专门化为可依赖于 int state、acquire和release参数以及内部FIFO等待队列的同步器。
 * 当这还不够时，您可以从较低级别构建同步器通过使用 atomic 类（自定义 java.util.Queue 和 LockSupport 支持阻塞支持。
 */
@SuppressWarnings("unused")
public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {
    private static final long serialVersionUID = 7373984972572414691L;


    protected AbstractQueuedSynchronizer() {
    }

    /**
     * Wait queue node class.
     * 等待队列是“CLH”（Craig、Landin 和 Hagersten）锁队列的变体。CLH 锁通常用于旋转锁。
     * 相反，我们使用它们来实现阻止同步器，但使用相同的基本策略，
     * 即在其节点的前一个线程中保存一些关于该线程的控制信息。每个节点中的“状态”字段跟踪线程是否应该阻塞。
     * 节点在其前一个节点释放时发出信号。队列的每个节点都充当一个特定的通知样式监视器，其中包含一个等待线程。
     * 但是 status 字段不控制线程是否被授予锁等。如果线程是队列中的第一个线程，它可能会尝试获取。
     * 但第一并不能保证成功；它只给了我们抗争的权利。因此，当前发布的竞争者线程可能需要重新等待。
     * <p>
     * 要排队进入 CLH 锁，您可以将其作为新的尾部进行原子拼接。要退出队列，只需设置 head 字段。
     * *            +------+  prev +-----+       +-----+
     * *       head |      | <---- |     | <---- |     |  tail
     * *            +------+       +-----+       +-----+
     * <p>
     * 插入到CLH队列只需要在“tail”上执行单个原子操作，因此有一个简单的从 unqueued 到 queued 的原子划分点。
     * 类似地，出列只涉及更新“头”。然而，节点需要更多的工作来确定谁是他们的继任者，
     * 部分是为了处理由于超时和中断而可能取消的问题。
     * <p>
     * “prev”链接（未在原始CLH锁中使用）主要用于处理取消。
     * 如果节点被取消，其后续节点（通常）将重新链接到未取消的前置节点。
     * 有关自旋锁的类似技术解释，请参阅
     * Scott 和 Scherer 在 http://www.cs.rochester.edu/u/scott/synchronization/
     * <p>
     * 我们还使用“下一步”链接来实现阻塞机制。
     * 每个节点的线程 id 都保存在自己的节点中，因此前置节点通过遍历下一个链接来确定它是哪个线程，
     * 从而向下一个节点发出唤醒信号。
     * 确定继任者必须避免与新排队的节点竞争，以设置其前任节点的“下一个”字段。
     * 当节点的后续节点显示为空时，通过从原子更新的“tail”向后检查，在必要时可以解决此问题。
     * （或者，换言之，下一个链接是一个优化，因此我们通常不需要反向扫描。）
     * <p>
     * 取消为基本算法引入了一些保守性。
     * 因为我们必须轮询其他节点的取消，所以我们可能会忽略已取消的节点是在我们前面还是后面。
     * 这是通过在撤销时始终取消继承人资格来解决的，允许他们稳定在新的前任上，
     * 除非我们能够确定一个未被撤销的前任将承担这一责任。
     * <p>
     * CLH 队列需要一个虚拟头节点来启动。但我们不会在构建时创建它们，
     * 因为如果不存在争用，这将是徒劳的。相反，在第一次争用时构造节点并设置头指针和尾指针。
     * <p>
     * 等待条件的线程使用相同的节点，但使用额外的链接。
     * 条件只需要链接简单（非并发）链接队列中的节点，因为它们仅在独占持有时才被访问。
     * 等待时，将节点插入到条件队列中。收到信号后，节点被转移到主队列。
     * 状态字段的特殊值用于标记节点所在的队列。
     * <p>
     * AQS 中的对 CLH 锁数据结构的改进主要包括三方面：
     * 1. 扩展每个节点的状态
     * 2. 显式的维护前驱节点和后继节点
     * 3. 诸如出队节点显式设为 null 等辅助 GC 的优化。
     */
    static final class Node {

        /**
         * 用于指示节点正在共享模式下等待的标记
         */
        static final Node SHARED = new Node();

        /**
         * 用于指示节点正在独占模式下等待的标记
         */
        static final Node EXCLUSIVE = null;

        /**
         * waitStatus值，指示线程已取消
         */
        static final int CANCELLED = 1;

        /**
         * waitStatus值，指示后续线程需要取消连接
         */
        static final int SIGNAL = -1;
        /**
         * 指示线程正在等待条件
         */
        static final int CONDITION = -2;

        /**
         * 指示下一个 acquireShared 应无条件传播
         */
        static final int PROPAGATE = -3;

        /**
         * 状态字段，仅接受以下值：信号：此节点的后续节点已（或即将）被阻止（通过驻车），
         * 因此当前节点在释放或取消时必须取消其后续节点的连接。
         * 为了避免竞争，acquire方法必须首先指示它们需要信号，然后重试原子 acquire，然后在失败时阻塞。
         * 取消：由于超时或中断，此节点被取消。节点永远不会离开此状态。
         * 特别是，具有取消节点的线程不会再次阻塞。
         * 条件：此节点当前在条件队列中。
         * 在传输之前，它不会用作同步队列节点，此时状态将设置为0。
         * （此处使用此值与字段的其他用途无关，但简化了力学。）传播：
         * 应将releaseShared传播到其他节点。
         * 这是在 doReleaseShared中设置的（仅针对头部节点），以确保传播继续进行，即使其他操作已经介入。
         * 0：以上值均未以数字形式排列以简化使用。非负值表示节点不需要发送信号。
         * 所以，大多数代码不需要检查特定的值，只需要检查符号。
         * 对于正常同步节点，字段初始化为0；
         * 对于条件节点，字段初始化为条件。使用CAS（或在可能的情况下，使用无条件易失性写入）对其进行修改。
         */
        volatile int waitStatus;

        /**
         * 链接到当前节点/线程检查等待状态所依赖的前置节点。
         * 在排队过程中分配，仅在退队时取消（为了GC）。
         * 此外，在取消前一个节点时，我们会在查找未取消的前一个节点时短路，该前一个节点将始终存在，
         * 因为头节点从未取消：只有成功获取后，节点才会成为头节点。
         * 被取消的线程永远不会成功获取，线程只会取消自身，而不会取消任何其他节点。
         */
        volatile Node prev;

        /**
         * 链接到后续节点，当前节点/线程在释放时将对其进行解析。
         * 在排队过程中分配，在绕过已取消的前导时调整，在退出队列时取消（为了GC）。
         * enq 操作直到连接之后才分配前置的下一个字段，所以看到空的下一个字段并不一定意味着节点在队列的末尾。
         * 但是，如果下一个字段显示为空，我们可以从尾部扫描上一个字段以进行双重检查。
         * 取消节点的下一个字段设置为指向节点本身，而不是null，以使 isOnSyncQueue 的工作更轻松。
         */
        volatile Node next;

        volatile Thread thread;

        /**
         * 链接到下一个等待条件的节点，或共享的特殊值。
         * 因为只有在独占模式下保持时才访问条件队列，
         * 所以我们只需要一个简单的链接队列来保持节点在等待条件时的状态。
         * 然后将它们转移到队列中以重新获取。
         * 由于条件只能是独占的，我们通过使用特殊值来表示共享模式来保存字段。
         */
        Node nextWaiter;

        /**
         * 如果节点正在共享模式下等待，则返回true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null) {
                throw new NullPointerException();
            } else
                return p;
        }

        Node() {
        }

        Node(Thread thread, Node node) {
            this.nextWaiter = node;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) {
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 等待队列的头，延迟初始化。除初始化外，它仅通过方法 setHead 进行修改。
     * 注意：如果 head 存在，its waitStatus is guaranteed not to be CANCELLED.
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized. Modified only via method enq to add new wait node.
     */
    private transient volatile Node tail;

    /**
     * The synchronization state.
     */
    private volatile int state;

    /**
     * 返回同步状态的当前值。此操作具有 volatile 读取的内存语义。
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态的值。此操作具有 volatile 写入的内存语义。
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 如果当前状态值等于预期值，则自动将同步状态设置为给定的更新值。此操作具有 volatile 读写的内存语义。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * 旋转比使用定时驻车更快的纳秒数。粗略的估计足以在很短的超时时间内提高响应能力。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 将节点插入队列，必要时进行初始化。
     * 返回: 插入节点的前驱节点
     */
    private Node enq(final Node node) {
        for (; ; ) {
            Node t = tail;
            if (t == null) {
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * 为当前线程和给定模式创建并排队节点。
     * 返回：新创建的结点
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒节点的后续节点（如果存在）。
     * <p>
     * 其中需要关注的一个细节是：由于没有针对双向链表节点的类似 compareAndSet 的原子性无锁插入指令，
     * 因此后驱节点的设置并非作为原子性插入操作的一部分，而仅是在节点被插入后简单地赋值。
     * 在释放锁时，如果当前节点的后驱节点不可用时，
     * 将从利用队尾指针 Tail 从尾部遍历到直到找到当前节点正确的后驱节点。
     */
    private void unparkSuccessor(Node node) {
        // 如果状态为负（即，可能需要信号），则尝试在预期信号的情况下清除。
        // 如果此操作失败或者等待线程更改了状态，则可以。
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        // unpark 的线程保存在后续节点中，通常只是下一个节点。
        // 但如果取消或明显为空，则从尾部向后遍历以找到实际的未取消的后续项。
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    private void doReleaseShared() {

    }

    private void setHeadAndPropagate(Node node, int propagate) {

    }

    // Utilities for various versions of acquire

    private void cancelAcquire(Node node) {

    }

    /**
     * 检查并更新未能获取的节点的状态。如果线程应该阻塞，则返回true。
     * 这是所有 acquire loops 中的主要信号控制。要求 pred == node.prev
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            return true;
        if (ws > 0) {
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /**
     * 已在队列中的线程以独占不间断模式获取。用于条件等待方法和获取。
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    public final void acquire(int arg) {
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 查询是否有线程等待获取锁的时间长于当前线程。
     * 判定条件为：有其他线程先更新 tail 成功了，使得 h != t
     * <p>
     * 节点入队不是原子操作，所以会出现短暂的head != tail，此时 Tail 指向最后一个节点，而且 Tail 指向Head。
     * 如果 Head 没有指向Tail，这种情况下也需要将相关线程加入队列中。
     * 所以这块代码是为了解决极端情况下的并发问题。
     */
    public final boolean hasQueuedPredecessors() {
        Node t = tail;
        Node h = head;
        Node s;
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;

        private transient Node firstWaiter;

        private transient Node lastWaiter;

        public ConditionObject() {
        }

        private Node addConditionWaiter() {
            Node t = lastWaiter;
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        private void doSignal(Node first) {

        }

        private void doSignalAll(Node first) {

        }

        /**
         * 从条件队列中取消已取消的 waiter 节点的链接。仅在持有锁时调用。
         * 当在条件等待期间发生取消时，以及在看到 lastWaiter 已被取消时插入新的 waiter 时，调用此函数。
         * 需要这种方法来避免在没有信号的情况下垃圾保留。
         * 因此，即使它可能需要一个完整的遍历，它也只有在没有信号的情况下发生超时或取消时才会起作用。
         * 它遍历所有节点，而不是在特定目标处停止，以取消所有指向垃圾节点的指针的链接，而无需在取消风暴期间多次重新遍历。
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                } else
                    trail = t;
                t = next;
            }
        }

        @Override
        public void await() throws InterruptedException {

        }

        @Override
        public void awaitUninterruptibly() {

        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            return 0;
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean awaitUntil(Date deadline) throws InterruptedException {
            return false;
        }

        @Override
        public void signal() {

        }

        @Override
        public void signalAll() {

        }
    }

    private static final Unsafe unsafe;
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }

    private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}






















