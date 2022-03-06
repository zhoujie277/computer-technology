package com.future.basis;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("all")
@Slf4j
class LRUCache<K, V> {

    private final LinkedList<K> keyList = new LinkedList<>();
    //    private final HashMap<K, V> cache = new HashMap<>();
//    private final HashMap<K, SoftReference<V>> cache = new HashMap<>();
    private final HashMap<K, WeakReference<V>> cache = new HashMap<>();

    private final DataLoader<K, V> loader;
    private final int capacity;

    public LRUCache(int capacity, DataLoader<K, V> loader) {
        this.capacity = capacity;
        this.loader = loader;
    }

    V get(K key) {
        V value = null;
        boolean remove = keyList.remove(key);
        if (remove) {
            value = cache.get(key).get();
        }
        if (value == null) {
            value = loader.load(key);
            if (keyList.size() >= capacity) {
                K k = keyList.removeFirst();
                onRemoveOldest(k, cache.remove(k).get());
            }
            cache.put(key, new WeakReference<>(value));
        }
        keyList.addLast(key);
        return value;
    }

    void put(K key, V value) {
        if (keyList.size() >= capacity) {
            K k = keyList.removeFirst();
            onRemoveOldest(k, cache.remove(k).get());
        }
        keyList.remove(key);
        keyList.addLast(key);
        cache.put(key, new WeakReference<>(value));
    }

    private void onRemoveOldest(K k, V value) {
        log.debug("key={}, value={}", k, value);
    }

    interface DataLoader<K, V> {
        V load(K k);
    }

    static class Data {
        final byte[] data = new byte[1 << 20];

        @Override
        protected void finalize() {
            System.out.println("finalize invoke! the data will be GC.");
        }
    }

    /**
     * -Xmx32m 设置最大的堆内存大小
     * -Xms16m 指定初始化的堆内存大小
     * -XX:+PrintGCDetails 在控制台输出 GC 的详细信息。
     */
    public static void main(String[] args) throws Exception {
        LRUCache<String, Data> cache = new LRUCache<>(20, key -> new Data());
        TimeUnit.SECONDS.sleep(3);
        for (int i = 0; i < 1000; i++) {
            Data data = cache.get("Hello" + i);
            if (data == null)
                System.out.println(data);
            TimeUnit.MILLISECONDS.sleep(120);
        }

        System.in.read();
    }
}
