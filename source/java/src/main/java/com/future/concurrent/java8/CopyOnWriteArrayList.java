package com.future.concurrent.java8;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * ArrayList 的线程安全变体，其中所有的变化操作（添加、设置等）都是通过创建底层数组的新副本实现的。
 * <p>
 * 这通常成本太高，但当遍历操作的数量远远超过变化操作时，可能比其他方法更有效，
 * 而且当你不能或不想同步遍历，但又需要排除并发线程之间的干扰时，这很有用。
 * "快照"风格的迭代器方法使用对迭代器创建时数组状态的引用。
 * 这个数组在迭代器的生命周期内永远不会改变，所以干扰是不可能的，
 * 迭代器保证不会抛出 ConcurrentModificationException。
 * 迭代器不会反映自迭代器创建以来对列表的添加、删除或更改。不支持对迭代器本身的元素改变操作（移除、设置和添加）。
 * 这些方法会抛出 UnsupportedOperationException。
 * <p>
 * 所有元素都是允许的，包括null。
 * <p>
 * 内存一致性影响。与其他并发集合一样，一个线程在将一个对象放入 CopyOnWriteArrayList 之前的操作，
 * 会在另一个线程中访问或从 CopyOnWriteArrayList 中移除该元素的后续操作之前发生。
 */
@SuppressWarnings("all")
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable {

    final ReentrantLock lock = new ReentrantLock();

    private volatile Object[] array;

    final Object[] getArray() {
        return array;
    }

    final void setArray(Object[] a) {
        array = a;
    }

    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class)
            elements = ((CopyOnWriteArrayList<?>) c).getArray();
        else {
            elements = c.toArray();
            if (c.getClass() != ArrayList.class)
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
        }
        setArray(elements);
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    public int size() {
        return getArray().length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private static boolean eq(Object o1, Object o2) {
        return Objects.equals(o1, o2);
    }

    /**
     * indexOf 的静态版本，允许重复调用而不需要每次都重新获取数组。
     */
    private static int indexOf(Object o, Object[] elements, int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++) {
                if (elements[i] == null)
                    return i;
            }
        } else {
            for (int i = index; i < fence; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--) {
                if (elements[i] == null)
                    return i;
            }
        } else {
            for (int i = index; i >= 0; i--) {
                if (o.equals(elements[i]))
                    return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    /**
     * 返回指定元素在这个列表中第一次出现的索引，从索引开始向前搜索，如果没有找到该元素，则返回 -1。
     * 更正式地说，返回最低的索引 i，这样(i >= index && (e == null ? get(i)==null : e.e equals(get(i)))),
     * 如果没有这样的索引，则返回 -1。
     */
    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    @Override
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    @Override
    public ListIterator<E> listIterator() {
        return null;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return null;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return null;
    }

    public int lastIndexOf(E e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            @SuppressWarnings("unchecked")
            CopyOnWriteArrayList<E> clone = (CopyOnWriteArrayList<E>) super.clone();
            clone.resetLock();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public Object[] toArray() {
        Object[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        if (a.length < len)
            return (T[]) Arrays.copyOf(elements, len, a.getClass());
        else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    public E get(int index) {
        return get(getArray(), index);
    }

    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            E oldValue = get(elements, index);
            if (oldValue != element) {
                int len = elements.length;
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element;
                setArray(newElements);
            } else {
                setArray(elements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定的元素添加到这个列表的末尾
     */
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1, numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index, numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        Object[] snapshot = getArray();
        int index = indexOf(o, snapshot, 0, snapshot.length);
        return index >= 0 && remove(o, snapshot, index);
    }

    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) findIndex:{
                int prefix = Math.min(index, len);
                for (int i = 0; i < prefix; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;
                        break findIndex;
                    }
                }
                if (index >= len)
                    return false;
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)
                    return false;
            }
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    void removeRange(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (fromIndex < 0 || toIndex > len || toIndex < fromIndex)
                throw new IndexOutOfBoundsException();
            int newLen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, newLen));
            else {
                Object[] newElements = new Object[newLen];
                System.arraycopy(elements, 0, newElements, 0, fromIndex);
                System.arraycopy(elements, toIndex, newElements, fromIndex, numMoved);
                setArray(newElements);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean addIfAbsent(E e) {
        Object[] snapshot = getArray();
        return indexOf(e, snapshot, 0, snapshot.length) < 0 && addIfAbsent(e, snapshot);
    }

    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++) {
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                }
                if (indexOf(e, current, common, len) >= 0)
                    return false;
            }
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Object e : c) {
            if (indexOf(e, elements, 0, len) < 0)
                return false;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newLen = 0;
                Object[] temp = new Object[len];
                for (Object element : elements) {
                    if (!c.contains(element))
                        temp[newLen++] = element;
                }
                if (newLen != len) {
                    setArray(Arrays.copyOf(temp, newLen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (Object element : elements) {
                    if (c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(new Object[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
                ((CopyOnWriteArrayList<?>) c).getArray() : c.toArray();
        if (cs.length == 0)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && (c.getClass() == java.util.concurrent.CopyOnWriteArrayList.class ||
                    c.getClass() == ArrayList.class)) {
                setArray(cs);
            } else {
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + len);
            if (cs.length == 0)
                return false;
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + cs.length);
            else {
                newElements = new Object[len + cs.length];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                        newElements, index + cs.length,
                        numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super E> action) {
        if (action == null) throw new NullPointerException();
        Object[] elements = getArray();
        int len = elements.length;
        for (Object element : elements) {
            E e = (E) element;
            action.accept(e);
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newLen = 0;
                Object[] temp = new Object[len];
                for (Object element : elements) {
                    @SuppressWarnings("unchecked")
                    E e = (E) element;
                    if (!filter.test(e))
                        temp[newLen++] = e;
                }
                if (newLen != len) {
                    setArray(Arrays.copyOf(temp, newLen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            for (int i = 0; i < len; ++i) {
                @SuppressWarnings("unchecked")
                E e = (E) elements[i];
                newElements[i] = operator.apply(e);
            }
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            Object[] newElements = Arrays.copyOf(elements, elements.length);
            @SuppressWarnings("unchecked")
            E[] es = (E[]) newElements;
            Arrays.sort(es, c);
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    private void resetLock() {
        U.putObjectVolatile(this, lockOffset, new ReentrantLock());
    }

    private static final sun.misc.Unsafe U;
    private static final long lockOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
            lockOffset = U.objectFieldOffset(CopyOnWriteArrayList.class.getDeclaredField("lock"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}














