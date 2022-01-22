package com.future.basis;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * 结论:
 * 1. 需要用普通对象作为 key。
 * * 不要用字符串常量池中的字符串作为弱引用的 key，
 * * 否则会 JVM 不会将其回收，也就不会将 WeakReference 放入引用队列。
 * 2. WeakHashMap 中的 Entry 继承自 WeakHashMap，且自有 引用队列。
 * *  当 WeakReference 中的引用对象可以被回收时，会将 WeakReference 放入引用队列，
 * *  WeakHashMap 中有一个 expungeStaleEntries 函数，会将其 Entry 遍历出来，
 * *  然后清理掉其对象，使其 Entry 可回收。
 * 3. WeakHashMap 要完全清理掉一个 Entry，至少需要两次以上 GC。1 次清理 key，第 2 此清理 Entry 及其 value。
 * *  当使用 WeakHashMap 其他方法时，比如 size() 和 getTable() 会调用 expungeStaleEntries ，清理掉过时的 weakReference。
 * *  ThreadLocal 与此类似，虽然 ThreadLocal 没有指定 ReferenceQueue，事实上，它只有一个元素，不需要指定 queue。
 *
 * @author future
 */
@Slf4j
public class WeakHashMapTest {

    @ToString
    static class TestObj {
        String name;

        public TestObj(String name) {
            this.name = name;
        }
    }

    WeakHashMap<TestObj, String> map = new WeakHashMap<>();
    WeakReference<TestObj> reference = new WeakReference<>(new TestObj("hello"));

    private void test() {
        map.put(new TestObj("key1"), "one");
        map.put(new TestObj("key2"), "two");
        map.put(new TestObj("key3"), "three");
        log.debug("{}", map);
    }

    private void testWeakReference() {
        TestObj first = reference.get();
        log.debug("first get() {}", first);
    }

    private void println() throws InterruptedException {
        System.gc();
        Thread.sleep(5000);
        log.debug("{}", map);
        log.debug("second get() {}", reference.get());
    }

    public static void main(String[] args) throws InterruptedException {
        WeakHashMapTest tester = new WeakHashMapTest();
        tester.testWeakReference();
        tester.test();
        tester.println();
    }
}
